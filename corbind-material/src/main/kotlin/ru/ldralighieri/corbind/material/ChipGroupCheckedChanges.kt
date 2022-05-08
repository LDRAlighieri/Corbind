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

import androidx.annotation.CheckResult
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.corbindReceiveChannel

/**
 * Perform an action on checked view IDs changes in [ChipGroup].
 *
 * *Warning:* The created actor uses [ChipGroup.setOnCheckedStateChangeListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ChipGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (List<Int>) -> Unit
) {
    val events = scope.actor<List<Int>>(Dispatchers.Main.immediate, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    events.trySend(checkedChipIds)
    setOnCheckedStateChangeListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnCheckedStateChangeListener(null) }
}

/**
 * Perform an action on checked view IDs changes in [ChipGroup], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [ChipGroup.setOnCheckedStateChangeListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ChipGroup.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (List<Int>) -> Unit
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel of the checked view IDs changes in [ChipGroup].
 *
 * *Warning:* The created channel uses [ChipGroup.setOnCheckedStateChangeListener]. Only one channel
 * can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 * *Note:* When the selection is cleared, checkedIds will be an empty list.
 *
 * Example:
 *
 * ```
 * launch {
 *      chipGroup.checkedChanges(scope)
 *          .consumeEach { /* handle checked ids */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ChipGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<List<Int>> = corbindReceiveChannel(capacity) {
    trySend(checkedChipIds)
    setOnCheckedStateChangeListener(listener(scope, ::trySend))
    invokeOnClose { setOnCheckedStateChangeListener(null) }
}

/**
 * Create a flow of the checked view IDs changes in [ChipGroup].
 *
 * *Warning:* The created flow uses [ChipGroup.setOnCheckedStateChangeListener]. Only one flow can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 * *Note:* When the selection is cleared, checkedIds will be an empty list.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * chipGroup.checkedChanges()
 *      .onEach { /* handle checked ids */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * chipGroup.checkedChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle checked view */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun ChipGroup.checkedChanges(): InitialValueFlow<List<Int>> = channelFlow {
    setOnCheckedStateChangeListener(listener(this, ::trySend))
    awaitClose { setOnCheckedStateChangeListener(null) }
}.asInitialValueFlow(checkedChipIds)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (List<Int>) -> Unit
) = ChipGroup.OnCheckedStateChangeListener { _, checkedIds ->
    if (scope.isActive) { emitter(checkedIds) }
}
