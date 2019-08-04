@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
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
 * Item long-click event on a adapter view
 */
data class AdapterViewItemLongClickEvent(
        val view: AdapterView<*>,
        val clickedView: View?,
        val position: Int,
        val id: Long
)

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on item long-click events for `view`.
 */
fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
        action: suspend (AdapterViewItemLongClickEvent) -> Unit
) {

    val events = scope.actor<AdapterViewItemLongClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemLongClickListener = listener(scope, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}

/**
 * Perform an action on item long-click events for `view` inside new CoroutineScope.
 */
suspend fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
        action: suspend (AdapterViewItemLongClickEvent) -> Unit
) = coroutineScope {

    val events = actor<AdapterViewItemLongClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemLongClickListener = listener(this, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of the item long-click events for `view`.
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<AdapterViewItemLongClickEvent> = corbindReceiveChannel(capacity) {
    onItemLongClickListener = listener(scope, handled, ::safeOffer)
    invokeOnClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of the item long-click events for `view`.
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
    handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue
): Flow<AdapterViewItemLongClickEvent> = channelFlow {
    onItemLongClickListener = listener(this, handled, ::offer)
    awaitClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` item long-click events
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (AdapterViewItemLongClickEvent) -> Boolean,
        emitter: (AdapterViewItemLongClickEvent) -> Boolean
) = AdapterView.OnItemLongClickListener { parent, view: View?, position, id ->

    if (scope.isActive) {
        val event = AdapterViewItemLongClickEvent(parent, view, position, id)
        if (handled(event)) {
            emitter(event)
            return@OnItemLongClickListener true
        }
    }
    return@OnItemLongClickListener false

}
