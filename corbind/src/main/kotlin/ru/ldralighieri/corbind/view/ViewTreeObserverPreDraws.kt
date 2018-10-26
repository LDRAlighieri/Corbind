package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun View.preDraws(
        scope: CoroutineScope,
        proceedDrawingPass: () -> Boolean,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

suspend fun View.preDraws(
        proceedDrawingPass: () -> Boolean,
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.preDraws(
        scope: CoroutineScope,
        proceedDrawingPass: () -> Boolean
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(proceedDrawingPass, ::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

@CheckResult
suspend fun View.preDraws(
        proceedDrawingPass: () -> Boolean
): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(proceedDrawingPass, ::offer)
        viewTreeObserver.addOnPreDrawListener(listener)
        invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        proceedDrawingPass: () -> Boolean,
        emitter: (Unit) -> Boolean
) = ViewTreeObserver.OnPreDrawListener {
    emitter(Unit)
    proceedDrawingPass()
}