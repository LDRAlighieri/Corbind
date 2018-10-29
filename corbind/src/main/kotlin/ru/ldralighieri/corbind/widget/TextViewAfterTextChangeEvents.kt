@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.text.Editable
import android.text.TextWatcher
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
    val listener = listener(scope = scope, textView = this, emitter = events::offer)
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
    val listener = listener(scope = this, textView = this@afterTextChangeEvents,
            emitter = events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TextView.afterTextChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<TextViewAfterTextChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@afterTextChangeEvents))
    val listener = listener(scope = this, textView = this@afterTextChangeEvents, emitter = ::offer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}

@CheckResult
suspend fun TextView.afterTextChangeEvents(): ReceiveChannel<TextViewAfterTextChangeEvent> =
        coroutineScope {

            produce<TextViewAfterTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
                offer(initialValue(this@afterTextChangeEvents))
                val listener = listener(scope = this, textView = this@afterTextChangeEvents,
                        emitter = ::offer)
                addTextChangedListener(listener)
                invokeOnClose { removeTextChangedListener(listener) }
            }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(textView: TextView): TextViewAfterTextChangeEvent =
        TextViewAfterTextChangeEvent(textView, textView.editableText)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        textView: TextView,
        emitter: (TextViewAfterTextChangeEvent) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {  }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {  }

    override fun afterTextChanged(s: Editable) {
        if (scope.isActive) { emitter(TextViewAfterTextChangeEvent(textView, s)) }
    }
}