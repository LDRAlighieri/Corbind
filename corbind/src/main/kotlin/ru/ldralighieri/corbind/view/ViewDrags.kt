package ru.ldralighieri.corbind.view

import android.view.DragEvent
import android.view.MenuItem
import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

fun View.drags(
        scope: CoroutineScope,
        handled: (DragEvent) -> Boolean = AlwaysTrue,
        action: suspend (DragEvent) -> Unit
) {
    val events = scope.actor<DragEvent>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (drag in channel) action(drag)
    }

    setOnDragListener(listener(handled = handled, emitter = events::offer))
    events.invokeOnClose { setOnDragListener(null) }
}

// -----------------------------------------------------------------------------------------------

fun View.drags(
        scope: CoroutineScope,
        handled: (DragEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<DragEvent> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    setOnDragListener(listener(handled = handled, emitter = ::offer))
    invokeOnClose { setOnDragListener(null) }
}

// -----------------------------------------------------------------------------------------------

private fun listener(
        handled: (DragEvent) -> Boolean,
        emitter: (DragEvent) -> Boolean
) = View.OnDragListener { _, dragEvent ->
    if (handled(dragEvent)) { emitter(dragEvent) }
    else { false }
}