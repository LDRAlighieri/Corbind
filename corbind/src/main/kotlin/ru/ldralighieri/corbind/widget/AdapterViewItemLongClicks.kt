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




/**
 * Perform an action on position of item long-clicks for [AdapterView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemLongClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: () -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (position in channel) action(position)
    }

    onItemLongClickListener = listener(scope, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}

/**
 * Perform an action on position of item long-clicks for [AdapterView] inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemLongClicks(
        capacity: Int = Channel.RENDEZVOUS,
        handled: () -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (position in channel) action(position)
    }

    onItemLongClickListener = listener(this, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}





/**
 * Create a channel of the position of item long-clicks for [AdapterView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    onItemLongClickListener = listener(scope, handled, ::safeOffer)
    invokeOnClose { onItemLongClickListener = null }
}





/**
 * Create a flow of the position of item long-clicks for [AdapterView].
 *
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
    handled: () -> Boolean = AlwaysTrue
): Flow<Int> = channelFlow {
    onItemLongClickListener = listener(this, handled, ::offer)
    awaitClose { onItemLongClickListener = null }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: () -> Boolean,
        emitter: (Int) -> Boolean
) = AdapterView.OnItemLongClickListener { _, _: View?, position, _ ->

    if (scope.isActive) {
        if (handled()) {
            emitter(position)
            return@OnItemLongClickListener true
        }
    }
    return@OnItemLongClickListener false

}
