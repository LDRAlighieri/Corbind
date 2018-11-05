@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.recyclerview

import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

data class RecyclerViewFlingEvent(val view: RecyclerView, val velocityX: Int, val velocityY: Int)

// -----------------------------------------------------------------------------------------------


fun RecyclerView.flingEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (RecyclerViewFlingEvent) -> Unit
) {

    val events = scope.actor<RecyclerViewFlingEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onFlingListener = listener(scope = scope, recyclerView = this, emitter = events::offer)
    events.invokeOnClose { onFlingListener = null }
}

suspend fun RecyclerView.flingEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (RecyclerViewFlingEvent) -> Unit
) = coroutineScope {

    val events = actor<RecyclerViewFlingEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onFlingListener = listener(scope = this, recyclerView = this@flingEvents,
            emitter = events::offer)
    events.invokeOnClose { onFlingListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RecyclerView.flingEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<RecyclerViewFlingEvent> = corbindReceiveChannel(capacity) {

    onFlingListener = listener(scope, this@flingEvents, ::safeOffer)
    invokeOnClose { onFlingListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        recyclerView: RecyclerView,
        emitter: (RecyclerViewFlingEvent) -> Boolean
) = object : RecyclerView.OnFlingListener() {

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        if (scope.isActive) {
            emitter(RecyclerViewFlingEvent(recyclerView, velocityX, velocityY))
        }
        return false
    }
}