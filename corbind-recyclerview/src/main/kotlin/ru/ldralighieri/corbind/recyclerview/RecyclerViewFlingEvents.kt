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

data class RecyclerViewFlingEvent(val view: RecyclerView, val velocityX: Int, val velocityY: Int)

// -----------------------------------------------------------------------------------------------


fun RecyclerView.flingEvents(
        scope: CoroutineScope,
        action: suspend (RecyclerViewFlingEvent) -> Unit
) {

    val events = scope.actor<RecyclerViewFlingEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onFlingListener = listener(scope = scope, recyclerView = this, emitter = events::offer)
    events.invokeOnClose { onFlingListener = null }
}

suspend fun RecyclerView.flingEvents(
        action: suspend (RecyclerViewFlingEvent) -> Unit
) = coroutineScope {

    val events = actor<RecyclerViewFlingEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onFlingListener = listener(scope = this, recyclerView = this@flingEvents,
            emitter = events::offer)
    events.invokeOnClose { onFlingListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RecyclerView.flingEvents(
        scope: CoroutineScope
): ReceiveChannel<RecyclerViewFlingEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    onFlingListener = listener(scope = this, recyclerView = this@flingEvents, emitter = ::offer)
    invokeOnClose { onFlingListener = null }
}

@CheckResult
suspend fun RecyclerView.flingEvents(): ReceiveChannel<RecyclerViewFlingEvent> = coroutineScope {

    produce<RecyclerViewFlingEvent>(Dispatchers.Main, Channel.CONFLATED) {
        onFlingListener = listener(scope = this, recyclerView = this@flingEvents, emitter = ::offer)
        invokeOnClose { onFlingListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        recyclerView: RecyclerView,
        emitter: (RecyclerViewFlingEvent) -> Boolean
) = object : RecyclerView.OnFlingListener() {

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        if (scope.isActive) {
            emitter(RecyclerViewFlingEvent(recyclerView, velocityX, velocityY))
        }
        return false
    }
}