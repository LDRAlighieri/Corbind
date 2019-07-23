@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.swiperefreshlayout

import androidx.annotation.CheckResult
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnRefreshListener(listener(scope, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}

suspend fun SwipeRefreshLayout.refreshes(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnRefreshListener(listener(this, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {

    setOnRefreshListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


suspend fun SwipeRefreshLayout.refreshes(): Flow<Unit> {
    var listener: SwipeRefreshLayout.OnRefreshListener?
    return flow {
        coroutineScope {
            val events = actor<Unit>(Dispatchers.Main) {
                for (unit in channel) emit(unit)
            }

            listener = listener(this, events::offer)
            setOnRefreshListener(listener)
        }
    }.onCompletion { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = SwipeRefreshLayout.OnRefreshListener {

    if (scope.isActive) { emitter(Unit) }
}