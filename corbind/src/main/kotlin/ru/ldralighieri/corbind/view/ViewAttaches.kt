package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun View.attaches(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.attaches(
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.attaches(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(true, ::offer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.attaches(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(true, ::offer)
        addOnAttachStateChangeListener(listener)
        invokeOnClose { removeOnAttachStateChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


fun View.detaches(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(false, events::offer)

    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.detaches(
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(false, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.detaches(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(false, ::offer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.detaches(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(false, ::offer)
        addOnAttachStateChangeListener(listener)
        invokeOnClose { removeOnAttachStateChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        callOnAttach: Boolean,
        emitter: (Unit) -> Boolean
) = object: View.OnAttachStateChangeListener {
    override fun onViewDetachedFromWindow(v: View) {
        if (callOnAttach) { emitter(Unit) }
    }

    override fun onViewAttachedToWindow(v: View) {
        if (!callOnAttach) { emitter(Unit) }
    }
}