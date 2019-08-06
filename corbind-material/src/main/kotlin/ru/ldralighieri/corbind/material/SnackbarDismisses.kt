@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.snackbar.Snackbar
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
 * Perform an action on the dismiss events from [Snackbar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Snackbar.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val callback = callback(scope, events::offer)
    addCallback(callback)
    events.invokeOnClose { removeCallback(callback) }
}

/**
 * Perform an action on the dismiss events from [Snackbar] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Snackbar.dismisses(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val callback = callback(this, events::offer)
    addCallback(callback)
    events.invokeOnClose { removeCallback(callback) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits the dismiss events from [Snackbar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Snackbar.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val callback = callback(scope, ::safeOffer)
    addCallback(callback)
    invokeOnClose { removeCallback(callback) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits the dismiss events from [Snackbar].
 */
@CheckResult
fun Snackbar.dismisses(): Flow<Int> = channelFlow {
    val callback = callback(this, ::offer)
    addCallback(callback)
    awaitClose { removeCallback(callback) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun callback(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = object : Snackbar.Callback() {
    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
        if (scope.isActive) { emitter(event) }
    }
}
