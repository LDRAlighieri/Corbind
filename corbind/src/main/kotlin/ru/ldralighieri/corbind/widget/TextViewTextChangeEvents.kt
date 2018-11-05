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

data class TextViewTextChangeEvent(
        val view: TextView,
        val text: CharSequence,
        val start: Int,
        val before: Int,
        val count: Int
)

// -----------------------------------------------------------------------------------------------


fun TextView.textChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (TextViewTextChangeEvent) -> Unit
) {

    val events = scope.actor<TextViewTextChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    val listener = listener(scope = scope, textView = this, emitter = events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.textChangeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (TextViewTextChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<TextViewTextChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@textChangeEvents))
    val listener = listener(scope = this, textView = this@textChangeEvents, emitter = events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TextView.textChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<TextViewTextChangeEvent> = corbindReceiveChannel(capacity) {

    safeOffer(initialValue(this@textChangeEvents))
    val listener = listener(scope, this@textChangeEvents, ::safeOffer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(textView: TextView): TextViewTextChangeEvent =
        TextViewTextChangeEvent(textView, textView.editableText, 0, 0, 0)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        textView: TextView,
        emitter: (TextViewTextChangeEvent) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {  }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (scope.isActive) {
            emitter(TextViewTextChangeEvent(textView, s, start, before, count))
        }
    }

    override fun afterTextChanged(s: Editable) {  }
}