@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.TextView
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------


fun TextView.editorActions(
        scope: CoroutineScope,
        handled: (Int) -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (actionId in channel) action(actionId)
    }

    setOnEditorActionListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}

suspend fun TextView.editorActions(
        handled: (Int) -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (actionId in channel) action(actionId)
    }

    setOnEditorActionListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TextView.editorActions(
        scope: CoroutineScope,
        handled: (Int) -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnEditorActionListener(listener(this, handled, ::offer))
    invokeOnClose { setOnEditorActionListener(null) }
}

@CheckResult
suspend fun TextView.editorActions(
        handled: (Int) -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        setOnEditorActionListener(listener(this, handled, ::offer))
        invokeOnClose { setOnEditorActionListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (Int) -> Boolean,
        emitter: (Int) -> Boolean
) = TextView.OnEditorActionListener { _, actionId, _ ->

    if (scope.isActive && handled(actionId)) {
        emitter(actionId)
        return@OnEditorActionListener true
    }
    return@OnEditorActionListener false
}