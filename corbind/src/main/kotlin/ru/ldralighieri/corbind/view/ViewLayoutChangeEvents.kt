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



data class ViewLayoutChangeEvent(
        val view: View,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val oldLeft: Int,
        val oldTop: Int,
        val oldRight: Int,
        val oldBottom: Int
)




/**
 * Perform an action on layout-change events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.layoutChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewLayoutChangeEvent) -> Unit
) {

    val events = scope.actor<ViewLayoutChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}

/**
 * Perform an action on layout-change events for [View] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.layoutChangeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewLayoutChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewLayoutChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(this, events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}




/**
 * Create a channel of layout-change events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.layoutChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewLayoutChangeEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::safeOffer)
    addOnLayoutChangeListener(listener)
    invokeOnClose { removeOnLayoutChangeListener(listener) }
}




/**
 * Create a flow of layout-change events for [View].
 */
@CheckResult
fun View.layoutChangeEvents(): Flow<ViewLayoutChangeEvent> = channelFlow {
    val listener = listener(this, ::offer)
    addOnLayoutChangeListener(listener)
    awaitClose { removeOnLayoutChangeListener(listener) }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (ViewLayoutChangeEvent) -> Boolean
) = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->

    if (scope.isActive) {
        emitter(ViewLayoutChangeEvent(v, left, top, right, bottom, oldLeft, oldTop, oldRight,
                oldBottom)
        )
    }

}
