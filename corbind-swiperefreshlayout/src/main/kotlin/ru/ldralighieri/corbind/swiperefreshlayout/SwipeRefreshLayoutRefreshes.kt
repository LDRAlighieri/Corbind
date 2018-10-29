@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.swiperefreshlayout

import androidx.annotation.CheckResult
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnRefreshListener(listener(scope, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}

suspend fun SwipeRefreshLayout.refreshes(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnRefreshListener(listener(this, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnRefreshListener(listener(this, ::offer))
    invokeOnClose { setOnRefreshListener(null) }
}

@CheckResult
suspend fun SwipeRefreshLayout.refreshes(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        setOnRefreshListener(listener(this, ::offer))
        invokeOnClose { setOnRefreshListener(null) }
    }
}

// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = SwipeRefreshLayout.OnRefreshListener {

    if (scope.isActive) { emitter(Unit) }
}