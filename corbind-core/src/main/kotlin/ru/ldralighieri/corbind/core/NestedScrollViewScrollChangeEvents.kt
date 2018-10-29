@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.core

import androidx.annotation.CheckResult
import androidx.core.widget.NestedScrollView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.view.ViewScrollChangeEvent

// -----------------------------------------------------------------------------------------------


fun NestedScrollView.scrollChangeEvents(
        scope: CoroutineScope,
        action: suspend (ViewScrollChangeEvent) -> Unit
) {

    val events = scope.actor<ViewScrollChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::offer)
    setOnScrollChangeListener(listener)
    events.invokeOnClose {
        setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }
}

suspend fun NestedScrollView.scrollChangeEvents(
        action: suspend (ViewScrollChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewScrollChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(this, events::offer)
    setOnScrollChangeListener(listener)
    events.invokeOnClose {
        setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun NestedScrollView.scrollChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<ViewScrollChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(this, ::offer)
    setOnScrollChangeListener(listener)
    invokeOnClose { setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?) }
}

@CheckResult
suspend fun NestedScrollView.scrollChangeEvents(): ReceiveChannel<ViewScrollChangeEvent> = coroutineScope {

    produce<ViewScrollChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(this, ::offer)
        setOnScrollChangeListener(listener)
        invokeOnClose { setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (ViewScrollChangeEvent) -> Boolean
) = NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

    if (scope.isActive) {
        emitter(ViewScrollChangeEvent(v, scrollX, scrollY, oldScrollX, oldScrollY))
    }
}