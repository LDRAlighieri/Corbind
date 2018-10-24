package ru.ldralighieri.corbind.swiperefreshlayout

import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope,
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    setOnRefreshListener(listener(this, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}

suspend fun SwipeRefreshLayout.refreshes(
        action: suspend (View) -> Unit
) = coroutineScope {
    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    setOnRefreshListener(listener(this@refreshes, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun SwipeRefreshLayout.refreshes(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnRefreshListener(listener(this@refreshes, ::offer))
    invokeOnClose { setOnRefreshListener(null) }
}

suspend fun SwipeRefreshLayout.refreshes(): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        setOnRefreshListener(listener(this@refreshes, ::offer))
        invokeOnClose { setOnRefreshListener(null) }
    }
}

// -----------------------------------------------------------------------------------------------


private fun listener(
        swipeRefreshLayout: SwipeRefreshLayout,
        emitter: (View) -> Boolean
) = SwipeRefreshLayout.OnRefreshListener { emitter(swipeRefreshLayout) }