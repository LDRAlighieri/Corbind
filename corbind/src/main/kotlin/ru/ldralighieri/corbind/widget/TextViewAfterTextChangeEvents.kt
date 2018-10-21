package ru.ldralighieri.corbind.widget

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

data class TextViewAfterTextChangeEvent(
        val view: TextView,
        val editable: Editable?
)

// -----------------------------------------------------------------------------------------------


fun TextView.afterTextChangeEvents(
        scope: CoroutineScope,
        action: suspend (TextViewAfterTextChangeEvent) -> Unit
) {
    val events = scope.actor<TextViewAfterTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    val listener = listener(this, events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.afterTextChangeEvents(
        action: suspend (TextViewAfterTextChangeEvent) -> Unit
) = coroutineScope {
    val events = actor<TextViewAfterTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@afterTextChangeEvents))
    val listener = listener(this@afterTextChangeEvents, events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun TextView.afterTextChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<TextViewAfterTextChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@afterTextChangeEvents))
    val listener = listener(this@afterTextChangeEvents, ::offer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.afterTextChangeEvents(): ReceiveChannel<TextViewAfterTextChangeEvent> =
        coroutineScope {

            produce<TextViewAfterTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
                offer(initialValue(this@afterTextChangeEvents))
                val listener = listener(this@afterTextChangeEvents, ::offer)
                addTextChangedListener(listener)
                invokeOnClose { removeTextChangedListener(listener) }
            }
}


// -----------------------------------------------------------------------------------------------


private fun initialValue(textView: TextView): TextViewAfterTextChangeEvent =
        TextViewAfterTextChangeEvent(textView, textView.editableText)


// -----------------------------------------------------------------------------------------------


private fun listener(
        textView: TextView,
        emitter: (TextViewAfterTextChangeEvent) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {  }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {  }

    override fun afterTextChanged(s: Editable) {
        emitter(TextViewAfterTextChangeEvent(textView, s))
    }
}