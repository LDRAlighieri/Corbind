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
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

data class TextViewTextChangeEvent(
    val view: TextView,
    val text: CharSequence,
    val start: Int,
    val before: Int,
    val count: Int,
)

/**
 * Perform an action on [text change events][TextViewTextChangeEvent] for [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun TextView.textChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TextViewTextChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<TextViewTextChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.corbindEventEmitter(scope)(initialValue(this))
    val listener = listener(scope, this, events.corbindEventEmitter(scope))
    addTextChangedListener(listener)
    events.invokeOnCloseOnMain { removeTextChangedListener(listener) }
}

/**
 * Perform an action on [text change events][TextViewTextChangeEvent] for [TextView], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun TextView.textChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TextViewTextChangeEvent) -> Unit,
) = coroutineScope {
    textChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [text change events][TextViewTextChangeEvent] for [TextView].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      textView.textChangeEvents(scope)
 *          .consumeEach { /* handle text change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun TextView.textChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<TextViewTextChangeEvent> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(initialValue(this@textChangeEvents))
    val listener = listener(scope, this@textChangeEvents, corbindEventEmitter())
    addTextChangedListener(listener)
    awaitClose { removeTextChangedListener(listener) }
}

/**
 * Create a flow of [text change events][TextViewTextChangeEvent] for [TextView].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * textView.textChangeEvents()
 *      .onEach { /* handle text change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * textView.textChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle text change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun TextView.textChangeEvents(): InitialValueFlow<TextViewTextChangeEvent> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    val listener = listener(this, this@textChangeEvents, emitter)
    addTextChangedListener(listener)
    emitter.sendInitialValue(initialValue(textView = this@textChangeEvents))
    awaitClose { removeTextChangedListener(listener) }
}.asInitialValueFlow()

@CheckResult
private fun initialValue(textView: TextView): TextViewTextChangeEvent = TextViewTextChangeEvent(textView, textView.editableText, 0, 0, 0)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    textView: TextView,
    emitter: (TextViewTextChangeEvent) -> Boolean,
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (scope.isActive) {
            emitter(TextViewTextChangeEvent(textView, s, start, before, count))
        }
    }

    override fun afterTextChanged(s: Editable) = Unit
}
