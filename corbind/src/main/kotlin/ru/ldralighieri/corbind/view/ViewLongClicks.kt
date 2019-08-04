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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on `view` long-click events.
 */
fun View.longClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: () -> Boolean = AlwaysTrue,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnLongClickListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}

/**
 * Perform an action on `view` long-click events inside new CoroutineScope.
 */
suspend fun View.longClicks(
        capacity: Int = Channel.RENDEZVOUS,
        handled: () -> Boolean = AlwaysTrue,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnLongClickListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits on `view` long-click events.
 */
@CheckResult
fun View.longClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {

    setOnLongClickListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnLongClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits on `view` long-click events.
 */
@CheckResult
fun View.longClicks(
    handled: () -> Boolean = AlwaysTrue
): Flow<Unit> = channelFlow {
    setOnLongClickListener(listener(this, handled, ::offer))
    awaitClose { setOnLongClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` long-click events.
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: () -> Boolean,
        emitter: (Unit) -> Boolean
) = View.OnLongClickListener {

    if (scope.isActive) {
        if (handled()) {
            emitter(Unit)
            return@OnLongClickListener true
        }
    }
    return@OnLongClickListener false

}
