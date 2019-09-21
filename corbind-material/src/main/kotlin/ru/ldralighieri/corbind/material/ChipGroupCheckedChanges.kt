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
import com.google.android.material.chip.ChipGroup
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
 * Perform an action on checked view ID changes in [ChipGroup].
 *
 * *Warning:* The created actor uses [ChipGroup.setOnCheckedChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ChipGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    checkSelectionMode(this)
    events.offer(checkedChipId)
    setOnCheckedChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Perform an action on checked view ID changes in [ChipGroup], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [ChipGroup.setOnCheckedChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ChipGroup.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel of the checked view ID changes in [ChipGroup].
 *
 * *Warning:* Only in single selection mode.
 * *Warning:* The created channel uses [ChipGroup.setOnCheckedChangeListener]. Only one channel can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 * *Note:* When the selection is cleared, checkedId is [View.NO_ID].
 *
 * Example:
 *
 * ```
 * launch {
 *      chipGroup.checkedChanges(scope)
 *          .consumeEach { /* handle checked view */ }
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
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    checkSelectionMode(this@checkedChanges)
    offerElement(checkedChipId)
    setOnCheckedChangeListener(listener(scope, ::offerElement))
    invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Create a flow of the checked view ID changes in [ChipGroup].
 *
 * *Warning:* Only in single selection mode.
 * *Warning:* The created flow uses [ChipGroup.setOnCheckedChangeListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 * *Note:* When the selection is cleared, checkedId is [View.NO_ID].
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * chipGroup.checkedChanges()
 *      .onEach { /* handle checked view */ }
 *      .launchIn(scope)
 *
 * // drop initial value
 * chipGroup.checkedChanges()
 *      .drop(1)
 *      .onEach { /* handle checked view */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun ChipGroup.checkedChanges(): Flow<Int> = channelFlow {
    checkSelectionMode(this@checkedChanges)
    offer(checkedChipId)
    setOnCheckedChangeListener(listener(this, ::offer))
    awaitClose { setOnCheckedChangeListener(null) }
}

private fun checkSelectionMode(group: ChipGroup) {
    check(group.isSingleSelection) { "The ChipGroup is not in single selection mode." }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : ChipGroup.OnCheckedChangeListener {

    private var lastChecked = View.NO_ID
    override fun onCheckedChanged(group: ChipGroup, checkedId: Int) {
        if (scope.isActive && checkedId != lastChecked) {
            lastChecked = checkedId
            emitter(checkedId)
        }
    }
}
