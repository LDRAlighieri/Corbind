@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.recyclerview

import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

data class RecyclerViewFlingEvent(val view: RecyclerView, val velocityX: Int, val velocityY: Int)

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on fling events on [RecyclerView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
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

/**
 * Perform an action on fling events on [RecyclerView] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
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


/**
 * Create a channel of fling events on [RecyclerView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RecyclerView.flingEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<RecyclerViewFlingEvent> = corbindReceiveChannel(capacity) {
    onFlingListener = listener(scope, this@flingEvents, ::safeOffer)
    invokeOnClose { onFlingListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of fling events on [RecyclerView].
 */
@CheckResult
fun RecyclerView.flingEvents(): Flow<RecyclerViewFlingEvent> = channelFlow {
    onFlingListener = listener(this, this@flingEvents, ::offer)
    awaitClose { onFlingListener = null }
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
