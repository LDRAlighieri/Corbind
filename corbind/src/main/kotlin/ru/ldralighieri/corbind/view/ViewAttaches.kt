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
 * Perform an action on [View] attach events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.attaches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(scope, true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Perform an action on [View] attach events inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.attaches(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(this, true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits on [View] attach events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.attaches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {

    val listener = listener(scope, true, ::safeOffer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits on [View] attach events.
 */
@CheckResult
fun View.attaches(): Flow<Unit> = channelFlow {
    val listener = listener(this, true, ::offer)
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}


// ===============================================================================================


/**
 * Perform an action on [View] detach events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.detaches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(scope, false, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Perform an action on [View] detach events inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.detaches(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(this, false, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits on [View] detach events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.detaches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {

    val listener = listener(scope, false, ::safeOffer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits on [View] detach events.
 */
@CheckResult
fun View.detaches(): Flow<Unit> = channelFlow {
    val listener = listener(this, false, ::offer)
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}



// -----------------------------------------------------------------------------------------------


/**
 * Listener of [View] attach / detach events
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        callOnAttach: Boolean,
        emitter: (Unit) -> Boolean
) = object: View.OnAttachStateChangeListener {

    override fun onViewDetachedFromWindow(v: View) {
        if (callOnAttach && scope.isActive) { emitter(Unit) }
    }

    override fun onViewAttachedToWindow(v: View) {
        if (!callOnAttach && scope.isActive) { emitter(Unit) }
    }
}
