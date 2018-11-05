@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.Adapter
import android.widget.AdapterView
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


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


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {

    onItemLongClickListener = listener(scope, handled, ::safeOffer)
    invokeOnClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: () -> Boolean,
        emitter: (Int) -> Boolean
) = AdapterView.OnItemLongClickListener { _, _, position, _ ->

    if (scope.isActive) {
        if (handled()) {
            emitter(position)
            return@OnItemLongClickListener true
        }
    }
    return@OnItemLongClickListener false
}