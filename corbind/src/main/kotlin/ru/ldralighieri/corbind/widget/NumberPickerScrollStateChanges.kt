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

import android.widget.NumberPicker
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
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on [NumberPicker] scroll state change.
 *
 * *Warning:* The created actor uses [NumberPicker.setOnScrollListener]. Only one actor can be used
 * at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun NumberPicker.scrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    setOnScrollListener(listener(scope, events::offer))
    events.invokeOnClose { setOnScrollListener(null) }
}

/**
 * Perform an action on [NumberPicker] scroll state change, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [NumberPicker.setOnScrollListener]. Only one actor can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      numberPicker.scrollStateChanges(scope)
 *          .consumeEach { /* handle scroll state change */ }
 * }
 * ```
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun NumberPicker.scrollStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    scrollStateChanges(this, capacity, action)
}

/**
 * Create a channel which emits on [NumberPicker] scroll state change.
 *
 * *Warning:* The created channel uses [NumberPicker.setOnScrollListener]. Only one channel can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun NumberPicker.scrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    setOnScrollListener(listener(scope, ::offerCatching))
    invokeOnClose { setOnScrollListener(null) }
}

/**
 * Create a flow which emits on [NumberPicker] scroll state change.
 *
 * *Warning:* The created flow uses [NumberPicker.setOnScrollListener]. Only one flow can be used at
 * a time.
 *
 * Example:
 *
 * ```
 * numberPicker.scrollStateChanges()
 *      .onEach { /* handle scroll state change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun NumberPicker.scrollStateChanges(): Flow<Int> = channelFlow<Int> {
    setOnScrollListener(listener(this, ::offerCatching))
    awaitClose { setOnScrollListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = NumberPicker.OnScrollListener { _, scrollState ->
    if (scope.isActive) { emitter(scrollState) }
}
