@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.swiperefreshlayout

import androidx.annotation.CheckResult
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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


/**
 * Perform an action on refresh events on [SwipeRefreshLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnRefreshListener(listener(scope, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}

/**
 * Perform an action on refresh events on [SwipeRefreshLayout] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SwipeRefreshLayout.refreshes(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnRefreshListener(listener(this, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of refresh events on [SwipeRefreshLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnRefreshListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of refresh events on [SwipeRefreshLayout].
 */
@CheckResult
fun SwipeRefreshLayout.refreshes(): Flow<Unit> = channelFlow {
    setOnRefreshListener(listener(this, ::offer))
    awaitClose { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = SwipeRefreshLayout.OnRefreshListener {
    if (scope.isActive) { emitter(Unit) }
}
