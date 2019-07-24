@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.TextView
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun TextView.editorActions(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (Int) -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (actionId in channel) action(actionId)
    }

    setOnEditorActionListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}

suspend fun TextView.editorActions(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (Int) -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (actionId in channel) action(actionId)
    }

    setOnEditorActionListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TextView.editorActions(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (Int) -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    setOnEditorActionListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnEditorActionListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TextView.editorActions(
        handled: (Int) -> Boolean = AlwaysTrue
): Flow<Int> = channelFlow {
    setOnEditorActionListener(listener(this, handled, ::offer))
    awaitClose { setOnEditorActionListener(null) }
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
