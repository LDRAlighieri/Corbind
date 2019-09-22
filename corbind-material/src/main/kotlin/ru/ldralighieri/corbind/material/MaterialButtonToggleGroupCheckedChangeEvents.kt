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
import androidx.annotation.IdRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
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

data class MaterialButtonCheckedChangeEvent(
    @IdRes val checkedId: Int,
    val isChecked: Boolean
)

/**
 * Perform an action on [check change event][MaterialButtonCheckedChangeEvent] on [MaterialButton]
 * in [MaterialButtonToggleGroup].
 *
 * *Warning:* Only *not* in single selection mode, use `buttonCheckedChanges` extension instead
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MaterialButtonCheckedChangeEvent) -> Unit
) {
    val events = scope.actor<MaterialButtonCheckedChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    checkSelectionMode(this@buttonCheckedChangeEvents)
    val listener = listener(scope, events::offer)
    addOnButtonCheckedListener(listener)
    events.invokeOnClose { removeOnButtonCheckedListener(listener) }
}

/**
 * Perform an action on [check change event][MaterialButtonCheckedChangeEvent] on [MaterialButton]
 * in [MaterialButtonToggleGroup], inside new [CoroutineScope].
 *
 * *Warning:* Only *not* in single selection mode, use `buttonCheckedChanges` extension instead.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MaterialButtonCheckedChangeEvent) -> Unit
) = coroutineScope {
    buttonCheckedChangeEvents(this, capacity, action)
}

/**
 * Create a channel which emits on [check change event][MaterialButtonCheckedChangeEvent] on
 * [MaterialButton] in [MaterialButtonToggleGroup]
 *
 * *Warning:* Only *not* in single selection mode, use `buttonCheckedChanges` extension instead.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialButtonToggleGroup.buttonCheckedChangeEvents(scope)
 *          .consumeEach { /* handle check change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<MaterialButtonCheckedChangeEvent> = corbindReceiveChannel(capacity) {
    checkSelectionMode(this@buttonCheckedChangeEvents)
    val listener = listener(scope, ::safeOffer)
    addOnButtonCheckedListener(listener)
    invokeOnClose { removeOnButtonCheckedListener(listener) }
}

/**
 * Create a flow which emits on [check change event][MaterialButtonCheckedChangeEvent] on
 * [MaterialButton] in [MaterialButtonToggleGroup]
 *
 * *Warning:* Only *not* in single selection mode, use `buttonCheckedChanges` extension instead.
 *
 * Example:
 *
 * ```
 * materialButtonToggleGroup.buttonCheckedChangeEvents()
 *      .onEach { /* handle check change event */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(): Flow<MaterialButtonCheckedChangeEvent> =
    channelFlow {
        checkSelectionMode(this@buttonCheckedChangeEvents)
        val listener = listener(this, ::offer)
        addOnButtonCheckedListener(listener)
        awaitClose { removeOnButtonCheckedListener(listener) }
    }

private fun checkSelectionMode(group: MaterialButtonToggleGroup) {
    check(!group.isSingleSelection) { "The MaterialButtonToggleGroup is in single selection mode. " +
        "Use `buttonCheckedChanges` extension instead" }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MaterialButtonCheckedChangeEvent) -> Boolean
) = MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
    if (scope.isActive) { emitter(MaterialButtonCheckedChangeEvent(checkedId, isChecked)) }
}
