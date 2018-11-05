@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

sealed class ViewAttachEvent {
    abstract val view: View
}

data class ViewAttachAttachedEvent(
        override val view: View
) : ViewAttachEvent()

data class ViewAttachDetachedEvent(
        override val view: View
) : ViewAttachEvent()

// -----------------------------------------------------------------------------------------------


fun View.attachEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewAttachEvent) -> Unit
) {

    val events = scope.actor<ViewAttachEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.attachEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewAttachEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewAttachEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(this, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.attachEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewAttachEvent> = corbindReceiveChannel(capacity) {

    val listener = listener(scope, ::safeOffer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (ViewAttachEvent) -> Boolean
) = object: View.OnAttachStateChangeListener {

    override fun onViewAttachedToWindow(v: View) { onEvent(ViewAttachAttachedEvent(v)) }
    override fun onViewDetachedFromWindow(v: View) { onEvent(ViewAttachDetachedEvent(v)) }

    private fun onEvent(event: ViewAttachEvent) {
        if (scope.isActive) { emitter(event) }
    }
}