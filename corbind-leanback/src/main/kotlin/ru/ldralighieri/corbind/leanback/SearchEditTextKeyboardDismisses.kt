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

package ru.ldralighieri.corbind.leanback

import androidx.annotation.CheckResult
import androidx.leanback.widget.SearchEditText
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
 * Perform an action on the keyboard dismiss events from [SearchEditText].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SearchEditText.keyboardDismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnKeyboardDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}

/**
 * Perform an action on the keyboard dismiss events from [SearchEditText] inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchEditText.keyboardDismisses(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnKeyboardDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}

/**
 * Create a channel which emits the keyboard dismiss events from [SearchEditText].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchEditText.keyboardDismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnKeyboardDismissListener(listener(scope, ::offerElement))
    invokeOnClose { setOnKeyboardDismissListener(null) }
}

/**
 * Create a flow which emits the keyboard dismiss events from [SearchEditText].
 */
@CheckResult
fun SearchEditText.keyboardDismisses(): Flow<Unit> = channelFlow {
    setOnKeyboardDismissListener(listener(this, ::offer))
    awaitClose { setOnKeyboardDismissListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = SearchEditText.OnKeyboardDismissListener {
    if (scope.isActive) { emitter(Unit) }
}
