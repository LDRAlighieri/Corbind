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

package ru.ldralighieri.corbind.view

import android.view.KeyEvent
import android.view.View
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
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on key events for [View].
 *
 * *Warning:* The created actor uses [View.setOnKeyListener]. Only one actor can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 * @param action An action to perform
 */
fun View.keys(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (KeyEvent) -> Boolean = AlwaysTrue,
    action: suspend (KeyEvent) -> Unit
) {
    val events = scope.actor<KeyEvent>(Dispatchers.Main.immediate, capacity) {
        for (key in channel) action(key)
    }

    setOnKeyListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnKeyListener(null) }
}

/**
 * Perform an action on key events for [View], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnKeyListener]. Only one actor can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 * @param action An action to perform
 */
suspend fun View.keys(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (KeyEvent) -> Boolean = AlwaysTrue,
    action: suspend (KeyEvent) -> Unit
) = coroutineScope {
    keys(this, capacity, handled, action)
}

/**
 * Create a channel of key events for [View].
 *
 * *Warning:* The created channel uses [View.setOnKeyListener]. Only one channel can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.keys(scope)
 *          .consumeEach { /* handle key */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 */
@CheckResult
fun View.keys(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (KeyEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<KeyEvent> = corbindReceiveChannel(capacity) {
    setOnKeyListener(listener(scope, handled, ::offerCatching))
    invokeOnClose { setOnKeyListener(null) }
}

/**
 * Create a flow of key events for [View].
 *
 * *Warning:* The created flow uses [View.setOnKeyListener]. Only one flow can be used at a time.
 *
 * Example:
 *
 * ```
 * view.keys()
 *      .onEach { /* handle key */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 */
@CheckResult
fun View.keys(
    handled: (KeyEvent) -> Boolean = AlwaysTrue
): Flow<KeyEvent> = channelFlow<KeyEvent> {
    setOnKeyListener(listener(this, handled, ::offerCatching))
    awaitClose { setOnKeyListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (KeyEvent) -> Boolean,
    emitter: (KeyEvent) -> Boolean
) = View.OnKeyListener { _, _, keyEvent ->
    if (scope.isActive && handled(keyEvent)) {
        emitter(keyEvent)
        return@OnKeyListener true
    }
    return@OnKeyListener false
}
