package ru.ldralighieri.corbind.view

import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

fun View.hovers(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) {
    val events = scope.actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnHoverListener(listener(handled, events::offer))
    events.invokeOnClose { setOnHoverListener(null) }
}

suspend fun View.hovers(
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) = coroutineScope {
    val events = actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnHoverListener(listener(handled, events::offer))
    events.invokeOnClose { setOnHoverListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun View.hovers(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnHoverListener(listener(handled, ::offer))
    invokeOnClose { setOnHoverListener(null) }
}

suspend fun View.hovers(
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = coroutineScope {

    produce<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnHoverListener(listener(handled, ::offer))
        invokeOnClose { setOnHoverListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        handled: (MotionEvent) -> Boolean,
        emitter: (MotionEvent) -> Boolean
) = View.OnHoverListener { _, motionEvent ->
    if (handled(motionEvent)) { emitter(motionEvent) }
    else { false }
}