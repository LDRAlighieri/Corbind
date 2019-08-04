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
 * Perform an action on `view` focus change.
 */
fun View.focusChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Boolean) -> Unit
) {

    val events = scope.actor<Boolean>(Dispatchers.Main, capacity) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}

/**
 * Perform an action on `view` focus change inside new CoroutineScope.
 */
suspend fun View.focusChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Boolean) -> Unit
) = coroutineScope {

    val events = actor<Boolean>(Dispatchers.Main, capacity) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(this, events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of booleans representing the focus of `view`.
 */
@CheckResult
fun View.focusChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    safeOffer(hasFocus())
    onFocusChangeListener = listener(scope, ::safeOffer)
    invokeOnClose { onFocusChangeListener = null }
}


// -----------------------------------------------------------------------------------------------

/**
 * Create a flow of booleans representing the focus of `view`.
 */
@CheckResult
fun View.focusChanges(): Flow<Boolean> = channelFlow {
    offer(hasFocus())
    onFocusChangeListener = listener(this, ::offer)
    awaitClose { onFocusChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` focus change
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Boolean) -> Boolean
) = View.OnFocusChangeListener { _, hasFocus ->
    if (scope.isActive) { emitter(hasFocus) }
}
