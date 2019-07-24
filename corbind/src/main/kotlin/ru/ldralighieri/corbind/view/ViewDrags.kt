@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.DragEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun View.drags(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (DragEvent) -> Boolean = AlwaysTrue,
        action: suspend (DragEvent) -> Unit
) {

    val events = scope.actor<DragEvent>(Dispatchers.Main, capacity) {
        for (drag in channel) action(drag)
    }

    setOnDragListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnDragListener(null) }
}

suspend fun View.drags(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (DragEvent) -> Boolean = AlwaysTrue,
        action: suspend (DragEvent) -> Unit
) = coroutineScope {

    val events = actor<DragEvent>(Dispatchers.Main, capacity) {
        for (drag in channel) action(drag)
    }

    setOnDragListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnDragListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.drags(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (DragEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<DragEvent> = corbindReceiveChannel(capacity) {

    setOnDragListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnDragListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.drags(
    handled: (DragEvent) -> Boolean = AlwaysTrue
): Flow<DragEvent> = channelFlow {
    setOnDragListener(listener(this, handled, ::offer))
    awaitClose { setOnDragListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (DragEvent) -> Boolean,
        emitter: (DragEvent) -> Boolean
) = View.OnDragListener { _, dragEvent ->

    if (scope.isActive) {
        if (handled(dragEvent)) {
            emitter(dragEvent)
            return@OnDragListener true
        }
    }
    return@OnDragListener false
}
