package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun View.attachEvents(
        scope: CoroutineScope,
        action: suspend (ViewAttachEvent) -> Unit
) {
    val events = scope.actor<ViewAttachEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.attachEvents(
        action: suspend (ViewAttachEvent) -> Unit
) = coroutineScope {
    val events = actor<ViewAttachEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.attachEvents(
        scope: CoroutineScope
): ReceiveChannel<ViewAttachEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(::offer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.attachEvents(): ReceiveChannel<ViewAttachEvent> = coroutineScope {

    produce<ViewAttachEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(::offer)
        addOnAttachStateChangeListener(listener)
        invokeOnClose { removeOnAttachStateChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (ViewAttachEvent) -> Boolean
) = object: View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) { emitter(ViewAttachAttachedEvent(v)) }
    override fun onViewDetachedFromWindow(v: View) { emitter(ViewAttachDetachedEvent(v)) }
}