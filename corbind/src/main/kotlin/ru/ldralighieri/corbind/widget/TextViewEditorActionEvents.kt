package ru.ldralighieri.corbind.widget

import android.view.KeyEvent
import android.widget.TextView
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive
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