@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.MotionEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
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

    setOnHoverListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnHoverListener(null) }
}

suspend fun View.hovers(
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) = coroutineScope {

    val events = actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnHoverListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnHoverListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.hovers(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnHoverListener(listener(this, handled, ::offer))
    invokeOnClose { setOnHoverListener(null) }
}

@CheckResult
suspend fun View.hovers(
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = coroutineScope {

    produce<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnHoverListener(listener(this, handled, ::offer))
        invokeOnClose { setOnHoverListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean,
        emitter: (MotionEvent) -> Boolean
) = View.OnHoverListener { _, motionEvent ->

    if (scope.isActive) {
        if (handled(motionEvent)) {
            emitter(motionEvent)
            return@OnHoverListener true
        }
    }
    return@OnHoverListener false
}