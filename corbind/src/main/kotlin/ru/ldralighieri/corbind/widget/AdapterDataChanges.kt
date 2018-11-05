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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> T.dataChanges(
        scope: CoroutineScope,
        action: suspend (T) -> Unit
) {

    val events = scope.actor<T>(Dispatchers.Main, Channel.CONFLATED) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this)
    val dataSetObserver = observer(scope = scope, adapter = this, emitter = events::offer)
    registerDataSetObserver(dataSetObserver)
    events.invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}

suspend fun <T : Adapter> T.dataChanges(
        action: suspend (T) -> Unit
) = coroutineScope {

    val events = actor<T>(Dispatchers.Main, Channel.CONFLATED) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this@dataChanges)
    val dataSetObserver = observer(scope = this, adapter = this@dataChanges,
            emitter = events::offer)
    registerDataSetObserver(dataSetObserver)
    events.invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> T.dataChanges(
        scope: CoroutineScope
): ReceiveChannel<T> = corbindReceiveChannel {

    offer(this@dataChanges)
    val dataSetObserver = observer(scope, this@dataChanges, ::safeOffer)
    registerDataSetObserver(dataSetObserver)
    invokeOnClose { unregisterDataSetObserver(dataSetObserver) }
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