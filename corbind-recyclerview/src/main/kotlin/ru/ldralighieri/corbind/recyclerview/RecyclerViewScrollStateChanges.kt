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


fun RecyclerView.scrollStateChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (state in channel) action(state)
    }

    val scrollListener = listener(scope, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}

suspend fun RecyclerView.scrollStateChanges(
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (state in channel) action(state)
    }

    val scrollListener = listener(this, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RecyclerView.scrollStateChanges(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val scrollListener = listener(this, ::offer)
    addOnScrollListener(scrollListener)
    invokeOnClose { removeOnScrollListener(scrollListener) }
}

@CheckResult
suspend fun RecyclerView.scrollStateChanges(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        val scrollListener = listener(this, ::offer)
        addOnScrollListener(scrollListener)
        invokeOnClose { removeOnScrollListener(scrollListener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) =  object : RecyclerView.OnScrollListener() {

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (scope.isActive) { emitter(newState) }
    }
}