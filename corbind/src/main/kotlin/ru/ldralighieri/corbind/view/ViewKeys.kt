@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.KeyEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

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
): ReceiveChannel<KeyEvent> = corbindReceiveChannel {

    setOnKeyListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnKeyListener(null) }
}

@CheckResult
suspend fun View.keys(
        handled: (KeyEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<KeyEvent> = coroutineScope {

    corbindReceiveChannel<KeyEvent> {
        setOnKeyListener(listener(this@coroutineScope, handled, ::safeOffer))
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