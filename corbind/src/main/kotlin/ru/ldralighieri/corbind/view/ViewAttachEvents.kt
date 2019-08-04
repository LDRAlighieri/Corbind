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
 * A view attach event on a view.
 */
sealed class ViewAttachEvent {
    abstract val view: View
}

/**
 * A view attached event on a view.
 */
data class ViewAttachAttachedEvent(
        override val view: View
) : ViewAttachEvent()

/**
 * A view detached event on a view.
 */
data class ViewAttachDetachedEvent(
        override val view: View
) : ViewAttachEvent()

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on attach and detach events on `view`.
 */
fun View.attachEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewAttachEvent) -> Unit
) {

    val events = scope.actor<ViewAttachEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Perform an action on attach and detach events on `view` inside new CoroutineScope.
 */
suspend fun View.attachEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewAttachEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewAttachEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(this, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of attach and detach events on `view`.
 */
@CheckResult
fun View.attachEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewAttachEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::safeOffer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of attach and detach events on `view`.
 */
@CheckResult
fun View.attachEvents(): Flow<ViewAttachEvent> = channelFlow {
    val listener = listener(this, ::offer)
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of attach and detach events on `view`.
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (ViewAttachEvent) -> Boolean
) = object: View.OnAttachStateChangeListener {

    override fun onViewAttachedToWindow(v: View) { onEvent(ViewAttachAttachedEvent(v)) }
    override fun onViewDetachedFromWindow(v: View) { onEvent(ViewAttachDetachedEvent(v)) }

    private fun onEvent(event: ViewAttachEvent) {
        if (scope.isActive) { emitter(event) }
    }
}
