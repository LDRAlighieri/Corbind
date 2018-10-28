package ru.ldralighieri.corbind.recyclerview

import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

// -----------------------------------------------------------------------------------------------

data class RecyclerViewScrollEvent(val view: RecyclerView, val dx: Int, val dy: Int)

// -----------------------------------------------------------------------------------------------


fun RecyclerView.scrollEvents(
        scope: CoroutineScope,
        action: suspend (RecyclerViewScrollEvent) -> Unit
) {

    val events = scope.actor<RecyclerViewScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val scrollListener = listener(scope, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}

suspend fun RecyclerView.scrollEvents(
        action: suspend (RecyclerViewScrollEvent) -> Unit
) = coroutineScope {

    val events = actor<RecyclerViewScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val scrollListener = listener(this, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RecyclerView.scrollEvents(
        scope: CoroutineScope
): ReceiveChannel<RecyclerViewScrollEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val scrollListener = listener(this, ::offer)
    addOnScrollListener(scrollListener)
    invokeOnClose { removeOnScrollListener(scrollListener) }
}

@CheckResult
suspend fun RecyclerView.scrollEvents(): ReceiveChannel<RecyclerViewScrollEvent> = coroutineScope {

    produce<RecyclerViewScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val scrollListener = listener(this, ::offer)
        addOnScrollListener(scrollListener)
        invokeOnClose { removeOnScrollListener(scrollListener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (RecyclerViewScrollEvent) -> Boolean
) = object : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (scope.isActive) { emitter(RecyclerViewScrollEvent(recyclerView, dx, dy)) }
    }
}