@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewTreeObserver
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
 * Perform an action on pre-draws on [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param proceedDrawingPass Let drawing process proceed
 * @param action An action to perform
 */
fun View.preDraws(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        proceedDrawingPass: () -> Boolean,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(scope, proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

/**
 * Perform an action on pre-draws on [View] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param proceedDrawingPass Let drawing process proceed
 * @param action An action to perform
 */
suspend fun View.preDraws(
        capacity: Int = Channel.RENDEZVOUS,
        proceedDrawingPass: () -> Boolean,
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(this, proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel for pre-draws on [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param proceedDrawingPass Let drawing process proceed
 */
@CheckResult
fun View.preDraws(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        proceedDrawingPass: () -> Boolean
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, proceedDrawingPass, ::safeOffer)
    viewTreeObserver.addOnPreDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow for pre-draws on [View].
 *
 * @param proceedDrawingPass Let drawing process proceed
 */
@CheckResult
fun View.preDraws(
    proceedDrawingPass: () -> Boolean
): Flow<Unit> = channelFlow {
    val listener = listener(this, proceedDrawingPass, ::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    awaitClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        proceedDrawingPass: () -> Boolean,
        emitter: (Unit) -> Boolean
) = ViewTreeObserver.OnPreDrawListener {

    if (scope.isActive) {
        emitter(Unit)
        return@OnPreDrawListener proceedDrawingPass()
    }
    return@OnPreDrawListener true

}
