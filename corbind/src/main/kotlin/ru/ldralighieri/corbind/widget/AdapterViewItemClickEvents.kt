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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer



data class AdapterViewItemClickEvent(
        val view: AdapterView<*>,
        val clickedView: View?,
        val position: Int,
        val id: Long
)




/**
 * Perform an action on the item click events for [AdapterView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemClickEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (AdapterViewItemClickEvent) -> Unit
) {

    val events = scope.actor<AdapterViewItemClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(scope, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}

/**
 * Perform an action on the item click events for [AdapterView] inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemClickEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (AdapterViewItemClickEvent) -> Unit
) = coroutineScope {

    val events = actor<AdapterViewItemClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(this, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}





/**
 * Create a channel of the item click events for [AdapterView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemClickEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<AdapterViewItemClickEvent> = corbindReceiveChannel(capacity) {
    onItemClickListener = listener(scope, ::safeOffer)
    invokeOnClose { onItemClickListener = null }
}





/**
 * Create a flow of the item click events for [AdapterView].
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemClickEvents(): Flow<AdapterViewItemClickEvent> = channelFlow {
    onItemClickListener = listener(this, ::offer)
    awaitClose { onItemClickListener = null }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (AdapterViewItemClickEvent) -> Boolean
) = AdapterView.OnItemClickListener { parent, view: View?, position, id ->

    if (scope.isActive) {
        emitter(AdapterViewItemClickEvent(parent, view, position, id))
    }

}
