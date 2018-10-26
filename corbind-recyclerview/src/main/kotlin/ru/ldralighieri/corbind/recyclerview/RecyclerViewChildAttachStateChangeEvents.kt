package ru.ldralighieri.corbind.recyclerview

import android.view.View
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

sealed class RecyclerViewChildAttachStateChangeEvent {
    abstract val view: RecyclerView
    abstract val child: View
}

data class RecyclerViewChildAttachEvent(
        override val view: RecyclerView,
        override val child: View
) : RecyclerViewChildAttachStateChangeEvent()

data class RecyclerViewChildDetachEvent(
        override val view: RecyclerView,
        override val child: View
) : RecyclerViewChildAttachStateChangeEvent()

// -----------------------------------------------------------------------------------------------


fun RecyclerView.childAttachStateChangeEvents(
        scope: CoroutineScope,
        action: suspend (RecyclerViewChildAttachStateChangeEvent) -> Unit
) {
    val events = scope.actor<RecyclerViewChildAttachStateChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(this, events::offer)
    addOnChildAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}

suspend fun RecyclerView.childAttachStateChangeEvents(
        action: suspend (RecyclerViewChildAttachStateChangeEvent) -> Unit
) = coroutineScope {
    val events = actor<RecyclerViewChildAttachStateChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(this@childAttachStateChangeEvents, events::offer)
    addOnChildAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RecyclerView.childAttachStateChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<RecyclerViewChildAttachStateChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(this@childAttachStateChangeEvents, ::offer)
    addOnChildAttachStateChangeListener(listener)
    invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}

@CheckResult
suspend fun RecyclerView.childAttachStateChangeEvents():
        ReceiveChannel<RecyclerViewChildAttachStateChangeEvent> = coroutineScope {


    produce<RecyclerViewChildAttachStateChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(this@childAttachStateChangeEvents, ::offer)
        addOnChildAttachStateChangeListener(listener)
        invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        recyclerView: RecyclerView,
        emitter: (RecyclerViewChildAttachStateChangeEvent) -> Boolean
) = object : RecyclerView.OnChildAttachStateChangeListener {

    override fun onChildViewAttachedToWindow(childView: View) {
        emitter(RecyclerViewChildAttachEvent(recyclerView, childView))
    }

    override fun onChildViewDetachedFromWindow(childView: View) {
        emitter(RecyclerViewChildDetachEvent(recyclerView, childView))
    }
}