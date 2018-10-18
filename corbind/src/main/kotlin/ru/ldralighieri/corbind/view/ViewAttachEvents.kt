package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce

// -----------------------------------------------------------------------------------------------

fun View.attachEvents(
        scope: CoroutineScope,
        action: suspend (ViewAttachEvent) -> Unit
) {
    val events = scope.actor<ViewAttachEvent>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(emitter = events::offer)

    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


fun View.attachEvents(
        scope: CoroutineScope
): ReceiveChannel<ViewAttachEvent> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    val listener = listener(emitter = ::offer)

    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

// -----------------------------------------------------------------------------------------------

private fun listener(
        emitter: (ViewAttachEvent) -> Boolean
) = object: View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) { emitter(ViewAttachAttachedEvent(v)) }
    override fun onViewDetachedFromWindow(v: View) { emitter(ViewAttachDetachedEvent(v)) }
}