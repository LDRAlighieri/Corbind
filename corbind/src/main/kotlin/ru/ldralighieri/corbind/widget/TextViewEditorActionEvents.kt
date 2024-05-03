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

import android.view.KeyEvent
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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel

data class TextViewEditorActionEvent(
    val view: TextView,
    val actionId: Int,
    val keyEvent: KeyEvent?,
)

/**
 * Perform an action on [editor action events][TextViewEditorActionEvent] on [TextView].
 *
 * *Warning:* The created actor uses [TextView.setOnEditorActionListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener].
 * @param action An action to perform
 */
fun TextView.editorActionEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
    action: suspend (TextViewEditorActionEvent) -> Unit,
) {
    val events = scope.actor<TextViewEditorActionEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    setOnEditorActionListener(listener(scope, handled, events::trySend))
    events.invokeOnClose { setOnEditorActionListener(null) }
}

/**
 * Perform an action on [editor action events][TextViewEditorActionEvent] on [TextView], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [TextView.setOnEditorActionListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener].
 * @param action An action to perform
 */
suspend fun TextView.editorActionEvents(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
    action: suspend (TextViewEditorActionEvent) -> Unit,
) = coroutineScope {
    editorActionEvents(this, capacity, handled, action)
}

/**
 * Create a channel of [editor action events][TextViewEditorActionEvent] on [TextView].
 *
 * *Warning:* The created channel uses [TextView.setOnEditorActionListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      textView.editorActionEvents(scope)
 *          .consumeEach { /* handle editor action event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener].
 */
@CheckResult
fun TextView.editorActionEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
): ReceiveChannel<TextViewEditorActionEvent> = corbindReceiveChannel(capacity) {
    setOnEditorActionListener(listener(scope, handled, ::trySend))
    invokeOnClose { setOnEditorActionListener(null) }
}

/**
 * Create a flow of [editor action events][TextViewEditorActionEvent] on [TextView].
 *
 * *Warning:* The created flow uses [TextView.setOnEditorActionListener]. Only one flow can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * textView.editorActionEvents()
 *      .onEach { /* handle editor action event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener].
 */
@CheckResult
fun TextView.editorActionEvents(
    handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
): Flow<TextViewEditorActionEvent> = channelFlow {
    setOnEditorActionListener(listener(this, handled, ::trySend))
    awaitClose { setOnEditorActionListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (TextViewEditorActionEvent) -> Boolean,
    emitter: (TextViewEditorActionEvent) -> Unit,
) = TextView.OnEditorActionListener { v, actionId, keyEvent ->
    if (scope.isActive) {
        val event = TextViewEditorActionEvent(v, actionId, keyEvent)
        if (handled(event)) {
            emitter(event)
            return@OnEditorActionListener true
        }
    }
    return@OnEditorActionListener false
}
