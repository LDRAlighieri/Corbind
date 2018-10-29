@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.view.KeyEvent
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

data class TextViewEditorActionEvent(
        val view: TextView,
        val actionId: Int,
        val keyEvent: KeyEvent?
)

// -----------------------------------------------------------------------------------------------


fun TextView.editorActionEvents(
        scope: CoroutineScope,
        handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
        action: suspend (TextViewEditorActionEvent) -> Unit
) {

    val events = scope.actor<TextViewEditorActionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnEditorActionListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}

suspend fun TextView.editorActionEvents(
        handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
        action: suspend (TextViewEditorActionEvent) -> Unit
) = coroutineScope {

    val events = actor<TextViewEditorActionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnEditorActionListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TextView.editorActionEvents(
        scope: CoroutineScope,
        handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<TextViewEditorActionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnEditorActionListener(listener(this, handled, ::offer))
    invokeOnClose { setOnEditorActionListener(null) }
}

@CheckResult
suspend fun TextView.editorActionEvents(
        handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<TextViewEditorActionEvent> = coroutineScope {

    produce<TextViewEditorActionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnEditorActionListener(listener(this, handled, ::offer))
        invokeOnClose { setOnEditorActionListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (TextViewEditorActionEvent) -> Boolean,
        emitter: (TextViewEditorActionEvent) -> Boolean
) = TextView.OnEditorActionListener { v, actionId, keyEvent ->

    if (scope.isActive) {
        val event = TextViewEditorActionEvent(v, actionId, keyEvent)
        if (handled(event)) {
            emitter(event)
            return@OnEditorActionListener true
        }
    }
    return@OnEditorActionListener false
}