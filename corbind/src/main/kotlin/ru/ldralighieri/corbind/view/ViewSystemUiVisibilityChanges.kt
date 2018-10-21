package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

fun View.systemUiVisibilityChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (visibility in channel) action(visibility)
    }

    setOnSystemUiVisibilityChangeListener(listener(events::offer))
    events.invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}

suspend fun View.systemUiVisibilityChanges(
        action: suspend (Int) -> Unit
) = coroutineScope {
    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (visibility in channel) action(visibility)
    }

    setOnSystemUiVisibilityChangeListener(listener(events::offer))
    events.invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun View.systemUiVisibilityChanges(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnSystemUiVisibilityChangeListener(listener(::offer))
    invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}

suspend fun View.systemUiVisibilityChanges(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        setOnSystemUiVisibilityChangeListener(listener(::offer))
        invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (Int) -> Boolean
) = View.OnSystemUiVisibilityChangeListener { emitter(it) }