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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on the selected position of [AdapterView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemSelections(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (position in channel) action(position)
    }

    events.offer(selectedItemPosition)
    onItemSelectedListener = listener(scope, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}

/**
 * Perform an action on the selected position of [AdapterView] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemSelections(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (position in channel) action(position)
    }

    events.offer(selectedItemPosition)
    onItemSelectedListener = listener(this, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of the selected position of [AdapterView]. If nothing is selected,
 * [AdapterView.INVALID_POSITION] will be emitted
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemSelections(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    offer(selectedItemPosition)
    onItemSelectedListener = listener(scope, ::safeOffer)
    invokeOnClose { onItemSelectedListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of the selected position of [AdapterView]. If nothing is selected,
 * [AdapterView.INVALID_POSITION] will be emitted
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemSelections(): Flow<Int> = channelFlow {
    offer(selectedItemPosition)
    onItemSelectedListener = listener(this, ::offer)
    awaitClose { onItemSelectedListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        onEvent(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>) { onEvent(AdapterView.INVALID_POSITION) }

    private fun onEvent(position: Int) {
        if (scope.isActive) { emitter(position) }
    }

}
