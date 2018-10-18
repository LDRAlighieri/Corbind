package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce

// -----------------------------------------------------------------------------------------------

fun View.focusChanges(
        scope: CoroutineScope,
        action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}

// -----------------------------------------------------------------------------------------------

fun View.focusChanges(
        scope: CoroutineScope
): ReceiveChannel<Boolean> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    offer(hasFocus())
    onFocusChangeListener = listener(::offer)
    invokeOnClose { onFocusChangeListener = null }
}

// -----------------------------------------------------------------------------------------------

private fun listener(
        emitter: (Boolean) -> Boolean
) = View.OnFocusChangeListener { _, hasFocus -> emitter(hasFocus)}