/*
 * Copyright 2019 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

data class TextViewBeforeTextChangeEvent(
    val view: TextView,
    val text: CharSequence,
    val start: Int,
    val count: Int,
    val after: Int
)

/**
 * Perform an action [before text change events][TextViewBeforeTextChangeEvent] for [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TextView.beforeTextChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TextViewBeforeTextChangeEvent) -> Unit
) {
    val events = scope.actor<TextViewBeforeTextChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    val listener = listener(scope, this, events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

/**
 * Perform an action [before text change events][TextViewBeforeTextChangeEvent] for [TextView],
 * inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TextView.beforeTextChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TextViewBeforeTextChangeEvent) -> Unit
) = coroutineScope {
    beforeTextChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [before text change events][TextViewBeforeTextChangeEvent] for [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TextView.beforeTextChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<TextViewBeforeTextChangeEvent> = corbindReceiveChannel(capacity) {
    offerElement(initialValue(this@beforeTextChangeEvents))
    val listener = listener(scope, this@beforeTextChangeEvents, ::offerElement)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}

/**
 * Create a flow of [before text change events][TextViewBeforeTextChangeEvent] for [TextView].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun TextView.beforeTextChangeEvents(): Flow<TextViewBeforeTextChangeEvent> = channelFlow {
    offer(initialValue(this@beforeTextChangeEvents))
    val listener = listener(this, this@beforeTextChangeEvents, ::offer)
    awaitClose { removeTextChangedListener(listener) }
}

@CheckResult
private fun initialValue(textView: TextView): TextViewBeforeTextChangeEvent =
        TextViewBeforeTextChangeEvent(textView, textView.editableText, 0, 0, 0)

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

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { }
    override fun afterTextChanged(s: Editable) { }
}
