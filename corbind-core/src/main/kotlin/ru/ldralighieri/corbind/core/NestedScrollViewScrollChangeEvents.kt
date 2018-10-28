package ru.ldralighieri.corbind.core

import androidx.annotation.CheckResult
import androidx.core.widget.NestedScrollView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive
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