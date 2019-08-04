@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.MotionEvent
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
 * Perform an action on hover events for `view`.
 */
fun View.hovers(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) {

    val events = scope.actor<MotionEvent>(Dispatchers.Main, capacity) {
        for (motion in channel) action(motion)
    }

    setOnHoverListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnHoverListener(null) }
}

/**
 * Perform an action on hover events for `view` inside new CoroutineScope.
 */
suspend fun View.hovers(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) = coroutineScope {

    val events = actor<MotionEvent>(Dispatchers.Main, capacity) {
        for (motion in channel) action(motion)
    }

    setOnHoverListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnHoverListener(null) }
}


// -----------------------------------------------------------------------------------------------

/**
 * Create a channel of hover events for `view`.
 */
@CheckResult
fun View.hovers(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = corbindReceiveChannel(capacity) {
    setOnHoverListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnHoverListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of hover events for `view`.
 */
@CheckResult
fun View.hovers(
    handled: (MotionEvent) -> Boolean = AlwaysTrue
): Flow<MotionEvent> = channelFlow {
    setOnHoverListener(listener(this, handled, ::offer))
    awaitClose { setOnHoverListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` hover
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean,
        emitter: (MotionEvent) -> Boolean
) = View.OnHoverListener { _, motionEvent ->

    if (scope.isActive) {
        if (handled(motionEvent)) {
            emitter(motionEvent)
            return@OnHoverListener true
        }
    }
    return@OnHoverListener false

}
