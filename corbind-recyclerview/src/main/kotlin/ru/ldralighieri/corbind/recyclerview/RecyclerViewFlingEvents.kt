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

data class RecyclerViewFlingEvent(val view: RecyclerView, val velocityX: Int, val velocityY: Int)

// -----------------------------------------------------------------------------------------------


fun RecyclerView.flingEvents(
        scope: CoroutineScope,
        action: suspend (RecyclerViewFlingEvent) -> Unit
) {
    val events = scope.actor<RecyclerViewFlingEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onFlingListener = listener(this, events::offer)
    events.invokeOnClose { onFlingListener = null }
}

suspend fun RecyclerView.flingEvents(
        action: suspend (RecyclerViewFlingEvent) -> Unit
) = coroutineScope {
    val events = actor<RecyclerViewFlingEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onFlingListener = listener(this@flingEvents, events::offer)
    events.invokeOnClose { onFlingListener = null }
}


// -----------------------------------------------------------------------------------------------


fun RecyclerView.flingEvents(
        scope: CoroutineScope
): ReceiveChannel<RecyclerViewFlingEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    onFlingListener = listener(this@flingEvents, ::offer)
    invokeOnClose { onFlingListener = null }
}

suspend fun RecyclerView.flingEvents(): ReceiveChannel<RecyclerViewFlingEvent> = coroutineScope {

    produce<RecyclerViewFlingEvent>(Dispatchers.Main, Channel.CONFLATED) {
        onFlingListener = listener(this@flingEvents, ::offer)
        invokeOnClose { onFlingListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        recyclerView: RecyclerView,
        emitter: (RecyclerViewFlingEvent) -> Boolean
) = object : RecyclerView.OnFlingListener() {

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        emitter(RecyclerViewFlingEvent(recyclerView, velocityX, velocityY))
        return false
    }
}