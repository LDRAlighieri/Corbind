@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
import androidx.annotation.CheckResult
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
 * Perform an action on a new system UI visibility for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.systemUiVisibilityChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (visibility in channel) action(visibility)
    }

    setOnSystemUiVisibilityChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}

/**
 * Perform an action on a new system UI visibility for [View] inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.systemUiVisibilityChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (visibility in channel) action(visibility)
    }

    setOnSystemUiVisibilityChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of integers representing a new system UI visibility for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.systemUiVisibilityChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    setOnSystemUiVisibilityChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of integers representing a new system UI visibility for [View].
 */
@CheckResult
fun View.systemUiVisibilityChanges(): Flow<Int> = channelFlow {
    setOnSystemUiVisibilityChangeListener(listener(this, ::offer))
    awaitClose { setOnSystemUiVisibilityChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of a new system UI visibility for [View]
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = View.OnSystemUiVisibilityChangeListener {
    if (scope.isActive) { emitter(it) }
}
