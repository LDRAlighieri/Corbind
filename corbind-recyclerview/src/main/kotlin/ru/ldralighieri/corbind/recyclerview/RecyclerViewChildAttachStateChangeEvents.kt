@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.recyclerview

import android.view.View
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

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

    val listener = listener(scope = scope, recyclerView = this, emitter = events::offer)
    addOnChildAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}

suspend fun RecyclerView.childAttachStateChangeEvents(
        action: suspend (RecyclerViewChildAttachStateChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<RecyclerViewChildAttachStateChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(scope = this, recyclerView = this@childAttachStateChangeEvents,
            emitter = events::offer)
    addOnChildAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RecyclerView.childAttachStateChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<RecyclerViewChildAttachStateChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(scope = this, recyclerView = this@childAttachStateChangeEvents,
            emitter = ::offer)
    addOnChildAttachStateChangeListener(listener)
    invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}

@CheckResult
suspend fun RecyclerView.childAttachStateChangeEvents():
        ReceiveChannel<RecyclerViewChildAttachStateChangeEvent> = coroutineScope {


    produce<RecyclerViewChildAttachStateChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(scope = this, recyclerView = this@childAttachStateChangeEvents,
                emitter = ::offer)
        addOnChildAttachStateChangeListener(listener)
        invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        recyclerView: RecyclerView,
        emitter: (RecyclerViewChildAttachStateChangeEvent) -> Boolean
) = object : RecyclerView.OnChildAttachStateChangeListener {

    override fun onChildViewAttachedToWindow(childView: View) {
        onEvent(RecyclerViewChildAttachEvent(recyclerView, childView))
    }

    override fun onChildViewDetachedFromWindow(childView: View) {
        onEvent(RecyclerViewChildDetachEvent(recyclerView, childView))
    }

    private fun onEvent(event: RecyclerViewChildAttachStateChangeEvent) {
        if (scope.isActive) { emitter(event) }
    }
}