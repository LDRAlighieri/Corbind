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


fun View.globalLayouts(
        scope: CoroutineScope,
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(this, events::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

suspend fun View.globalLayouts(
        action: suspend (View) -> Unit
) = coroutineScope {
    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(this@globalLayouts, events::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.globalLayouts(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(this@globalLayouts, ::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

suspend fun View.globalLayouts(): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(this@globalLayouts, ::offer)
        viewTreeObserver.addOnGlobalLayoutListener(listener)
        invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        view: View,
        emitter: (View) -> Boolean
) = ViewTreeObserver.OnGlobalLayoutListener { emitter(view) }