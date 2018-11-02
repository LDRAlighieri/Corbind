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
): ReceiveChannel<RecyclerViewScrollEvent> = corbindReceiveChannel {

    val scrollListener = listener(scope, ::safeOffer)
    addOnScrollListener(scrollListener)
    invokeOnClose { removeOnScrollListener(scrollListener) }
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