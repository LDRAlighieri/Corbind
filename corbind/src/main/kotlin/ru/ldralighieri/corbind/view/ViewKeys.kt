package ru.ldralighieri.corbind.view

import android.view.KeyEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive
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

    setOnKeyListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnKeyListener(null) }
}

suspend fun View.keys(
        handled: (KeyEvent) -> Boolean = AlwaysTrue,
        action: suspend (KeyEvent) -> Unit
) = coroutineScope {

    val events = actor<KeyEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (key in channel) action(key)
    }

    setOnKeyListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnKeyListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.keys(
        scope: CoroutineScope,
        handled: (KeyEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<KeyEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnKeyListener(listener(this, handled, ::offer))
    invokeOnClose { setOnKeyListener(null) }
}

@CheckResult
suspend fun View.keys(
        handled: (KeyEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<KeyEvent> = coroutineScope {

    produce<KeyEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnKeyListener(listener(this, handled, ::offer))
        invokeOnClose { setOnKeyListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (KeyEvent) -> Boolean,
        emitter: (KeyEvent) -> Boolean
) = View.OnKeyListener { _, _, keyEvent ->

    if (scope.isActive) {
        if (handled(keyEvent)) {
            emitter(keyEvent)
            return@OnKeyListener true
        }
    }
    return@OnKeyListener false
}