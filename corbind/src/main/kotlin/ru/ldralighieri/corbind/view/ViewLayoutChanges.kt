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


fun View.layoutChanges(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}

suspend fun View.layoutChanges(
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.layoutChanges(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(::offer)
    addOnLayoutChangeListener(listener)
    invokeOnClose { removeOnLayoutChangeListener(listener) }
}

suspend fun View.layoutChanges(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(::offer)
        addOnLayoutChangeListener(listener)
        invokeOnClose { removeOnLayoutChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (Unit) -> Boolean
) = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> emitter(Unit) }