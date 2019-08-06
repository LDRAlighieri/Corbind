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

data class RecyclerViewScrollEvent(val view: RecyclerView, val dx: Int, val dy: Int)

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on scroll events on [RecyclerView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RecyclerView.scrollEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (RecyclerViewScrollEvent) -> Unit
) {

    val events = scope.actor<RecyclerViewScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val scrollListener = listener(scope, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}

/**
 * Perform an action on scroll events on [RecyclerView] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RecyclerView.scrollEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (RecyclerViewScrollEvent) -> Unit
) = coroutineScope {

    val events = actor<RecyclerViewScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val scrollListener = listener(this, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of scroll events on [RecyclerView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RecyclerView.scrollEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<RecyclerViewScrollEvent> = corbindReceiveChannel(capacity) {
    val scrollListener = listener(scope, ::safeOffer)
    addOnScrollListener(scrollListener)
    invokeOnClose { removeOnScrollListener(scrollListener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of scroll events on [RecyclerView].
 */
@CheckResult
fun RecyclerView.scrollEvents(): Flow<RecyclerViewScrollEvent> = channelFlow {
    val scrollListener = listener(this, ::offer)
    addOnScrollListener(scrollListener)
    awaitClose { removeOnScrollListener(scrollListener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (RecyclerViewScrollEvent) -> Boolean
) = object : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (scope.isActive) { emitter(RecyclerViewScrollEvent(recyclerView, dx, dy)) }
    }

}
