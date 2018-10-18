package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce

// -----------------------------------------------------------------------------------------------

fun View.attaches(
        scope: CoroutineScope,
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(callOnAttach = true, emitter = events::offer)

    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


fun View.attaches(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    val listener = listener(callOnAttach = true, emitter = ::offer)

    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

// -----------------------------------------------------------------------------------------------

fun View.detaches(
        scope: CoroutineScope,
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = listener(callOnAttach = false, emitter = events::offer)

    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


fun View.detaches(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    val listener = listener(callOnAttach = false, emitter = ::offer)

    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

// -----------------------------------------------------------------------------------------------

private fun listener(
        callOnAttach: Boolean,
        emitter: (View) -> Boolean
) = object: View.OnAttachStateChangeListener {
    override fun onViewDetachedFromWindow(v: View) {
        if (callOnAttach) { emitter(v) }
    }

    override fun onViewAttachedToWindow(v: View) {
        if (!callOnAttach) { emitter(v) }
    }
}