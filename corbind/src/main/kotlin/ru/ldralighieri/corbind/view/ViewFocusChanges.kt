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


fun View.focusChanges(
        scope: CoroutineScope,
        action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}

suspend fun View.focusChanges(
        action: suspend (Boolean) -> Unit
) = coroutineScope {
    val events = actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


fun View.focusChanges(
        scope: CoroutineScope
): ReceiveChannel<Boolean> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(hasFocus())
    onFocusChangeListener = listener(::offer)
    invokeOnClose { onFocusChangeListener = null }
}

suspend fun View.focusChanges(): ReceiveChannel<Boolean> = coroutineScope {

    produce<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        offer(hasFocus())
        onFocusChangeListener = listener(::offer)
        invokeOnClose { onFocusChangeListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (Boolean) -> Boolean
) = View.OnFocusChangeListener { _, hasFocus -> emitter(hasFocus)}