@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.recyclerview

import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
        scope: CoroutineScope,
        action: suspend (T) -> Unit
) {

    val events = scope.actor<T>(Dispatchers.Main, Channel.CONFLATED) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this)
    val dataObserver = observer(scope = scope, adapter = this, emitter = events::offer)
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
    val dataObserver = observer(scope = this, adapter = this@dataChanges, emitter = events::offer)
    registerAdapterDataObserver(dataObserver)
    events.invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
        scope: CoroutineScope
): ReceiveChannel<T> = corbindReceiveChannel {

    safeOffer(this@dataChanges)
    val dataObserver = observer(scope, this@dataChanges, ::safeOffer)
    registerAdapterDataObserver(dataObserver)
    invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
}

@CheckResult
suspend fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
): ReceiveChannel<T> = coroutineScope {

    corbindReceiveChannel<T> {
        safeOffer(this@dataChanges)
        val dataObserver = observer(this@coroutineScope, this@dataChanges, ::safeOffer)
        registerAdapterDataObserver(dataObserver)
        invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> observer(
        scope: CoroutineScope,
        adapter: T,
        emitter: (T) -> Boolean
) =  object : RecyclerView.AdapterDataObserver() {

    override fun onChanged() {
        if (scope.isActive) { emitter(adapter) }
    }
}