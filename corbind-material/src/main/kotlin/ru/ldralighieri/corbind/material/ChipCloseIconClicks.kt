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

package ru.ldralighieri.corbind.material

import android.view.View
import androidx.annotation.CheckResult
import com.google.android.material.chip.Chip
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
 * Perform an action on [Chip] close icon click events.
 *
 * *Warning:* The created actor uses [Chip.setOnCloseIconClickListener]. Only one actor can be used
 * at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Chip.closeIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (unit in channel) action()
    }

    setOnCloseIconClickListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCloseIconClickListener(null) }
}

/**
 * Perform an action on [Chip] close icon click events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [Chip.setOnCloseIconClickListener]. Only one actor can be used
 * at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Chip.closeIconClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {
    closeIconClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [Chip] close icon click events.
 *
 * *Warning:* The created channel uses [Chip.setOnCloseIconClickListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      chip.closeIconClicks(scope)
 *          .consumeEach { /* handle close icon click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Chip.closeIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnCloseIconClickListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnClickListener(null) }
}

/**
 * Create a flow which emits on [Chip] close icon click events.
 *
 * *Warning:* The created flow uses [Chip.setOnCloseIconClickListener]. Only one flow can be used at
 * a time.
 *
 * Example:
 *
 * ```
 * chip.closeIconClicks()
 *      .onEach { /* handle close icon click */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun Chip.closeIconClicks(): Flow<Unit> = channelFlow {
    setOnCloseIconClickListener(listener(this, ::offer))
    awaitClose { setOnClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = View.OnClickListener {
    if (scope.isActive) { emitter(Unit) }
}
