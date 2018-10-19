package ru.ldralighieri.corbind.view

import android.view.KeyEvent
import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

fun View.keys(
        scope: CoroutineScope,
        handled: (KeyEvent) -> Boolean = AlwaysTrue,
        action: suspend (KeyEvent) -> Unit
) {
    val events = scope.actor<KeyEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (key in channel) action(key)
    }

    setOnKeyListener(listener(handled, events::offer))
    events.invokeOnClose { setOnKeyListener(null) }
}

suspend fun View.keys(
        handled: (KeyEvent) -> Boolean = AlwaysTrue,
        action: suspend (KeyEvent) -> Unit
) = coroutineScope {
    val events = actor<KeyEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (key in channel) action(key)
    }

    setOnKeyListener(listener(handled, events::offer))
    events.invokeOnClose { setOnKeyListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun View.keys(
        scope: CoroutineScope,
        handled: (KeyEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<KeyEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnKeyListener(listener(handled, ::offer))
    invokeOnClose { setOnKeyListener(null) }
}

suspend fun View.keys(
        handled: (KeyEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<KeyEvent> = coroutineScope {

    produce<KeyEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnKeyListener(listener(handled, ::offer))
        invokeOnClose { setOnKeyListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        handled: (KeyEvent) -> Boolean,
        emitter: (KeyEvent) -> Boolean
) = View.OnKeyListener { _, _, keyEvent ->
    if (handled(keyEvent)) { emitter(keyEvent) }
    else { false }
}