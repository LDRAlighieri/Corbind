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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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


/**
 * Perform an action on text change events for [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
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

/**
 * Perform an action on text change events for [TextView] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
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


/**
 * Create a channel of text change events for [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
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


/**
 * Create a flow of text change events for [TextView].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun TextView.textChangeEvents(): Flow<TextViewTextChangeEvent> = channelFlow {
    offer(initialValue(this@textChangeEvents))
    val listener = listener(this, this@textChangeEvents, ::offer)
    addTextChangedListener(listener)
    awaitClose { removeTextChangedListener(listener) }
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
