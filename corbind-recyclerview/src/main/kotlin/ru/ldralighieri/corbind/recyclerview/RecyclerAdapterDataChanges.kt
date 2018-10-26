package ru.ldralighieri.corbind.recyclerview

import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
        scope: CoroutineScope,
        action: suspend (T) -> Unit
) {
    val events = scope.actor<T>(Dispatchers.Main, Channel.CONFLATED) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this)
    val dataObserver = observer(this, events::offer)
    registerAdapterDataObserver(dataObserver)
    events.invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
}

suspend fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
        action: suspend (T) -> Unit
) = coroutineScope {
    val events = actor<T>(Dispatchers.Main, Channel.CONFLATED) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this@dataChanges)
    val dataObserver = observer(this@dataChanges, events::offer)
    registerAdapterDataObserver(dataObserver)
    events.invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
}


// -----------------------------------------------------------------------------------------------


fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
        scope: CoroutineScope
): ReceiveChannel<T> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(this@dataChanges)
    val dataObserver = observer(this@dataChanges, ::offer)
    registerAdapterDataObserver(dataObserver)
    invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
}

suspend fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
): ReceiveChannel<T> = coroutineScope {

    produce<T>(Dispatchers.Main, Channel.CONFLATED) {
        offer(this@dataChanges)
        val dataObserver = observer(this@dataChanges, ::offer)
        registerAdapterDataObserver(dataObserver)
        invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> observer(
        adapter: T,
        emitter: (T) -> Boolean
) =  object : RecyclerView.AdapterDataObserver() {

    override fun onChanged() { emitter(adapter) }
}