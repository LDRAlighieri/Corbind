package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewTreeObserver
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
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(this, proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

suspend fun View.preDraws(
        proceedDrawingPass: () -> Boolean,
        action: suspend (View) -> Unit
) = coroutineScope {
    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(this@preDraws, proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.preDraws(
        scope: CoroutineScope,
        proceedDrawingPass: () -> Boolean
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(this@preDraws, proceedDrawingPass, ::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

suspend fun View.preDraws(
        proceedDrawingPass: () -> Boolean
): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(this@preDraws, proceedDrawingPass, ::offer)
        viewTreeObserver.addOnPreDrawListener(listener)
        invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        view: View,
        proceedDrawingPass: () -> Boolean,
        emitter: (View) -> Boolean
) = ViewTreeObserver.OnPreDrawListener {
    emitter(view)
    proceedDrawingPass()
}