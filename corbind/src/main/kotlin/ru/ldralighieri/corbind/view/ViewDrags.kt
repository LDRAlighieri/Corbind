package ru.ldralighieri.corbind.view

import android.view.DragEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------


fun View.drags(
        scope: CoroutineScope,
        handled: (DragEvent) -> Boolean = AlwaysTrue,
        action: suspend (DragEvent) -> Unit
) {
    val events = scope.actor<DragEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (drag in channel) action(drag)
    }

    setOnDragListener(listener( handled, events::offer))
    events.invokeOnClose { setOnDragListener(null) }
}

suspend fun View.drags(
        handled: (DragEvent) -> Boolean = AlwaysTrue,
        action: suspend (DragEvent) -> Unit
) = coroutineScope {
    val events = actor<DragEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (drag in channel) action(drag)
    }

    setOnDragListener(listener( handled, events::offer))
    events.invokeOnClose { setOnDragListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.drags(
        scope: CoroutineScope,
        handled: (DragEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<DragEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnDragListener(listener(handled, ::offer))
    invokeOnClose { setOnDragListener(null) }
}

@CheckResult
suspend fun View.drags(
        handled: (DragEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<DragEvent> = coroutineScope {

    produce<DragEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnDragListener(listener(handled, ::offer))
        invokeOnClose { setOnDragListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        handled: (DragEvent) -> Boolean,
        emitter: (DragEvent) -> Boolean
) = View.OnDragListener { _, dragEvent ->
    if (handled(dragEvent)) { emitter(dragEvent) }
    else { false }
}