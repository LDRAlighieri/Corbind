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
import ru.ldralighieri.corbind.internal.safeOffer

data class TextViewEditorActionEvent(
    val view: TextView,
    val actionId: Int,
    val keyEvent: KeyEvent?
)

/**
 * Perform an action on editor action events on [TextView].
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
    action: suspend (TextViewEditorActionEvent) -> Unit
) {

    val events = scope.actor<TextViewEditorActionEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnEditorActionListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}

/**
 * Perform an action on editor action events on [TextView] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener].
 * @param action An action to perform
 */
suspend fun TextView.editorActionEvents(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue,
    action: suspend (TextViewEditorActionEvent) -> Unit
) = coroutineScope {

    val events = actor<TextViewEditorActionEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnEditorActionListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnEditorActionListener(null) }
}

/**
 * Create a channel of editor action events on [TextView].
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
    handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<TextViewEditorActionEvent> = corbindReceiveChannel(capacity) {
    setOnEditorActionListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnEditorActionListener(null) }
}

/**
 * Create a flow of editor action events on [TextView].
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener].
 */
@CheckResult
fun TextView.editorActionEvents(
    handled: (TextViewEditorActionEvent) -> Boolean = AlwaysTrue
): Flow<TextViewEditorActionEvent> = channelFlow {
    setOnEditorActionListener(listener(this, handled, ::offer))
    awaitClose { setOnEditorActionListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (TextViewEditorActionEvent) -> Boolean,
    emitter: (TextViewEditorActionEvent) -> Boolean
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
