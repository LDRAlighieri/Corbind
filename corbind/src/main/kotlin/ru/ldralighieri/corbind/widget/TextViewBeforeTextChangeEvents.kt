package ru.ldralighieri.corbind.widget

import android.text.Editable
import android.text.TextWatcher
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

// -----------------------------------------------------------------------------------------------

data class TextViewBeforeTextChangeEvent(
        val view: TextView,
        val text: CharSequence,
        val start: Int,
        val count: Int,
        val after: Int
)

// -----------------------------------------------------------------------------------------------


fun TextView.beforeTextChangeEvents(
        scope: CoroutineScope,
        action: suspend (TextViewBeforeTextChangeEvent) -> Unit
) {

    val events = scope.actor<TextViewBeforeTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    val listener = listener(scope = scope, textView = this, emitter = events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.beforeTextChangeEvents(
        action: suspend (TextViewBeforeTextChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<TextViewBeforeTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@beforeTextChangeEvents))
    val listener = listener(scope = this, textView = this@beforeTextChangeEvents,
            emitter = events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TextView.beforeTextChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<TextViewBeforeTextChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@beforeTextChangeEvents))
    val listener = listener(scope = this, textView = this@beforeTextChangeEvents, emitter = ::offer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}

@CheckResult
suspend fun TextView.beforeTextChangeEvents(): ReceiveChannel<TextViewBeforeTextChangeEvent> =
        coroutineScope {

            produce<TextViewBeforeTextChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
                offer(initialValue(this@beforeTextChangeEvents))
                val listener = listener(scope = this, textView = this@beforeTextChangeEvents,
                        emitter = ::offer)
                addTextChangedListener(listener)
                invokeOnClose { removeTextChangedListener(listener) }
            }
        }


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(textView: TextView): TextViewBeforeTextChangeEvent =
        TextViewBeforeTextChangeEvent(textView, textView.editableText, 0, 0, 0)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        textView: TextView,
        emitter: (TextViewBeforeTextChangeEvent) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (scope.isActive) {
            emitter(TextViewBeforeTextChangeEvent(textView, s, start, count, after))
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {  }
    override fun afterTextChanged(s: Editable) {  }
}