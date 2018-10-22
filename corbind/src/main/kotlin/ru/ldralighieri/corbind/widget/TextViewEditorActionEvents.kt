package ru.ldralighieri.corbind.widget

import android.view.KeyEvent
import android.widget.TextView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
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

    setOnEditorActionListener(listener(handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}

suspend fun TextView.editorActionEvents(
        handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
        action: suspend (TextViewEditorActionEvent) -> Unit
) = coroutineScope {
    val events = actor<TextViewEditorActionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnEditorActionListener(listener(handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun TextView.editorActionEvents(
        scope: CoroutineScope,
        handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<TextViewEditorActionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnEditorActionListener(listener(handled, ::offer))
    invokeOnClose { setOnEditorActionListener(null) }
}

suspend fun TextView.editorActionEvents(
        handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<TextViewEditorActionEvent> = coroutineScope {

    produce<TextViewEditorActionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnEditorActionListener(listener(handled, ::offer))
        invokeOnClose { setOnEditorActionListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        handled: (TextViewEditorActionEvent) -> Boolean,
        emitter: (TextViewEditorActionEvent) -> Boolean
) = TextView.OnEditorActionListener { v, actionId, event ->
    val actionEvent = TextViewEditorActionEvent(v, actionId, event)
    if (handled(actionEvent)) { emitter(TextViewEditorActionEvent(v, actionId, event)) }
    else { false }
}