package ru.ldralighieri.corbind.material

import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun Snackbar.dismisses(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val callback = callback(events::offer)
    addCallback(callback)
    events.invokeOnClose { removeCallback(callback) }
}

suspend fun Snackbar.dismisses(
        action: suspend (Int) -> Unit
) = coroutineScope {
    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val callback = callback(events::offer)
    addCallback(callback)
    events.invokeOnClose { removeCallback(callback) }
}


// -----------------------------------------------------------------------------------------------


fun Snackbar.dismisses(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val callback = callback(::offer)
    addCallback(callback)
    invokeOnClose { removeCallback(callback) }
}

suspend fun Snackbar.dismisses(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        val callback = callback(::offer)
        addCallback(callback)
        invokeOnClose { removeCallback(callback) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun callback(
        emitter: (Int) -> Boolean
) = object : Snackbar.Callback() {
    override fun onDismissed(transientBottomBar: Snackbar, event: Int) { emitter(event) }
}