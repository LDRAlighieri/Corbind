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


fun View.clicks(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnClickListener(listener(events::offer))
    events.invokeOnClose { setOnClickListener(null) }
}

suspend fun View.clicks(
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnClickListener(listener(events::offer))
    events.invokeOnClose { setOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun View.clicks(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnClickListener(listener(::offer))
    invokeOnClose { setOnClickListener(null) }
}

suspend fun View.clicks(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        setOnClickListener(listener(::offer))
        invokeOnClose { setOnClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (Unit) -> Boolean
) = View.OnClickListener { emitter(Unit) }
