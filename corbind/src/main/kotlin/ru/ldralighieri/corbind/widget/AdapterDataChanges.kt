@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.database.DataSetObserver
import android.widget.Adapter
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
 * Perform an action on data change events for [Adapter].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : Adapter> T.dataChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (T) -> Unit
) {

    val events = scope.actor<T>(Dispatchers.Main, capacity) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this)
    val dataSetObserver = observer(scope = scope, adapter = this, emitter = events::offer)
    registerDataSetObserver(dataSetObserver)
    events.invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}

/**
 * Perform an action on data change events for [Adapter] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : Adapter> T.dataChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (T) -> Unit
) = coroutineScope {

    val events = actor<T>(Dispatchers.Main, capacity) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this@dataChanges)
    val dataSetObserver = observer(scope = this, adapter = this@dataChanges,
            emitter = events::offer)
    registerDataSetObserver(dataSetObserver)
    events.invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of data change events for [Adapter].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : Adapter> T.dataChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<T> = corbindReceiveChannel(capacity) {
    offer(this@dataChanges)
    val dataSetObserver = observer(scope, this@dataChanges, ::safeOffer)
    registerDataSetObserver(dataSetObserver)
    invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of data change events for [Adapter].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun <T : Adapter> T.dataChanges(): Flow<T> = channelFlow {
    offer(this@dataChanges)
    val dataSetObserver = observer(this, this@dataChanges, ::offer)
    registerDataSetObserver(dataSetObserver)
    awaitClose { unregisterDataSetObserver(dataSetObserver) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun <T : Adapter> observer(
        scope: CoroutineScope,
        adapter: T,
        emitter: (T) -> Boolean
) = object : DataSetObserver() {

    override fun onChanged() {
        if (scope.isActive) { emitter(adapter) }
    }

}
