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


fun View.touches(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) {
    val events = scope.actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}

suspend fun View.touches(
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) = coroutineScope {
    val events = actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun View.touches(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnTouchListener(listener(handled, ::offer))
    invokeOnClose { setOnTouchListener(null) }
}

suspend fun View.touches(
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = coroutineScope {

    produce<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnTouchListener(listener(handled, ::offer))
        invokeOnClose { setOnTouchListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        handled: (MotionEvent) -> Boolean,
        emitter: (MotionEvent) -> Boolean
) = View.OnTouchListener { _, motionEvent ->
    if (handled(motionEvent)) { emitter(motionEvent) }
    else { false }
}