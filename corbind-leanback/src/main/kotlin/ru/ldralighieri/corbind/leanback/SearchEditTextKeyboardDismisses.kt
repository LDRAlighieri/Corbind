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
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on the keyboard dismiss events from [SearchEditText].
 *
 * *Warning:* The created actor uses [SearchEditText.setOnKeyboardDismissListener]. Only one actor
 * can be used at a time.
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
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setOnKeyboardDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}

/**
 * Perform an action on the keyboard dismiss events from [SearchEditText], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [SearchEditText.setOnKeyboardDismissListener]. Only one actor
 * can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchEditText.keyboardDismisses(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {
    keyboardDismisses(this, capacity, action)
}

/**
 * Create a channel which emits the keyboard dismiss events from [SearchEditText].
 *
 * *Warning:* The created channel uses [SearchEditText.setOnKeyboardDismissListener]. Only one
 * channel can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      searchEditText.keyboardDismisses(scope)
 *          .consumeEach { /* handle keyboard dismiss */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchEditText.keyboardDismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnKeyboardDismissListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnKeyboardDismissListener(null) }
}

/**
 * Create a flow which emits the keyboard dismiss events from [SearchEditText].
 *
 * *Warning:* The created flow uses [SearchEditText.setOnKeyboardDismissListener]. Only one flow can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * searchEditText.keyboardDismisses()
 *      .onEach { /* handle keyboard dismiss */ }
 *      .launchIn(scope)
 * ```
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
