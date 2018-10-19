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
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}

suspend fun View.layoutChanges(
        action: suspend (View) -> Unit
) = coroutineScope {
    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.layoutChanges(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(::offer)
    addOnLayoutChangeListener(listener)
    invokeOnClose { removeOnLayoutChangeListener(listener) }
}

suspend fun View.layoutChanges(): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(::offer)
        addOnLayoutChangeListener(listener)
        invokeOnClose { removeOnLayoutChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (View) -> Boolean
) = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> emitter(v) }