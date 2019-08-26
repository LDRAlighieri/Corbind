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

/**
 * Perform an action on character sequences for text changes on [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TextView.textChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CharSequence) -> Unit
) {

    val events = scope.actor<CharSequence>(Dispatchers.Main, capacity) {
        for (chars in channel) action(chars)
    }

    events.offer(text)
    val listener = listener(scope, events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

/**
 * Perform an action on character sequences for text changes on [TextView] inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TextView.textChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CharSequence) -> Unit
) = coroutineScope {

    val events = actor<CharSequence>(Dispatchers.Main, capacity) {
        for (chars in channel) action(chars)
    }

    events.offer(text)
    val listener = listener(this, events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

/**
 * Create a channel of character sequences for text changes on [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TextView.textChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<CharSequence> = corbindReceiveChannel(capacity) {
    offerElement(text)
    val listener = listener(scope, ::offerElement)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}

/**
 * Create a channel of character sequences for text changes on [TextView].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
suspend fun TextView.textChanges(
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<CharSequence> = coroutineScope {
    corbindReceiveChannel<CharSequence>(capacity) {
        offerElement(text)
        val listener = listener(this@coroutineScope, ::offerElement)
        addTextChangedListener(listener)
        invokeOnClose { removeTextChangedListener(listener) }
    }
}

/**
 * Create a flow of character sequences for text changes on [TextView].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun TextView.textChanges(): Flow<CharSequence> = channelFlow {
    offer(text)
    val listener = listener(this, ::offer)
    addTextChangedListener(listener)
    awaitClose { removeTextChangedListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (CharSequence) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (scope.isActive) { emitter(s) }
    }

    override fun afterTextChanged(s: Editable) { }
}
