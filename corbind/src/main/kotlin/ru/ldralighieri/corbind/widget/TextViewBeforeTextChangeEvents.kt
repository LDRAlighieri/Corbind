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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

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
): ReceiveChannel<TextViewBeforeTextChangeEvent> = corbindReceiveChannel {

    safeOffer(initialValue(this@beforeTextChangeEvents))
    val listener = listener(scope, this@beforeTextChangeEvents, ::safeOffer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
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