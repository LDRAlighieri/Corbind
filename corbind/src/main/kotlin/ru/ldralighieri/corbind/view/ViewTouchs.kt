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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun View.touches(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) {

    val events = scope.actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}

suspend fun View.touches(
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) = coroutineScope {

    val events = actor<MotionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.touches(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = corbindReceiveChannel {

    setOnTouchListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnTouchListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean,
        emitter: (MotionEvent) -> Boolean
) = View.OnTouchListener { _, motionEvent ->

    if (scope.isActive) {
        if (handled(motionEvent)) {
            emitter(motionEvent)
            return@OnTouchListener true
        }
    }
    return@OnTouchListener false
}