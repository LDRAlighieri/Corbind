@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.core

import androidx.annotation.CheckResult
import androidx.core.widget.NestedScrollView
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer
import ru.ldralighieri.corbind.view.ViewScrollChangeEvent

// -----------------------------------------------------------------------------------------------


fun NestedScrollView.scrollChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewScrollChangeEvent) -> Unit
) {

    val events = scope.actor<ViewScrollChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::offer)
    setOnScrollChangeListener(listener)
    events.invokeOnClose {
        setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }
}

suspend fun NestedScrollView.scrollChangeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewScrollChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewScrollChangeEvent>(Dispatchers.Main, capacity) {
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
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewScrollChangeEvent> = corbindReceiveChannel(capacity) {

    val listener = listener(scope, ::safeOffer)
    setOnScrollChangeListener(listener)
    invokeOnClose { setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?) }
}


// -----------------------------------------------------------------------------------------------


suspend fun NestedScrollView.scrollChangeEvents(): Flow<ViewScrollChangeEvent> {
    var listener: NestedScrollView.OnScrollChangeListener?
    return flow {
        coroutineScope {
            val events = actor<ViewScrollChangeEvent>(Dispatchers.Main) {
                for (event in channel) emit(event)
            }

            listener = listener(this, events::offer)
            setOnScrollChangeListener(listener)
        }
    }.onCompletion { setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?) }
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
