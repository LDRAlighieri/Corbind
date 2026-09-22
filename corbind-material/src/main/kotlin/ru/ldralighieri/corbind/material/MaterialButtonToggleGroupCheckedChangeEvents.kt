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
import androidx.annotation.MainThread
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
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

data class MaterialButtonCheckedChangeEvent(
    val checkedId: Int,
    val isChecked: Boolean,
)

/**
 * Perform an action on [check change event][MaterialButtonCheckedChangeEvent] on [MaterialButton]
 * in [MaterialButtonToggleGroup].
 *
 * *Warning:* Requires multiple-selection mode. Use `buttonCheckedChanges` for single selection.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MaterialButtonCheckedChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<MaterialButtonCheckedChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    checkSelectionMode(this@buttonCheckedChangeEvents)
    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnButtonCheckedListener(listener)
    events.invokeOnCloseOnMain { removeOnButtonCheckedListener(listener) }
}

/**
 * Perform an action on [check change event][MaterialButtonCheckedChangeEvent] on [MaterialButton]
 * in [MaterialButtonToggleGroup], in a new [CoroutineScope].
 *
 * *Warning:* Requires multiple-selection mode. Use `buttonCheckedChanges` for single selection.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MaterialButtonCheckedChangeEvent) -> Unit,
) = coroutineScope {
    buttonCheckedChangeEvents(this, capacity, action)
}

/**
 * Create a channel that emits [check change events][MaterialButtonCheckedChangeEvent] for buttons
 * in [MaterialButtonToggleGroup].
 *
 * *Warning:* Requires multiple-selection mode. Use `buttonCheckedChanges` for single selection.
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<MaterialButtonCheckedChangeEvent> = corbindReceiveChannel(scope, capacity) {
    checkSelectionMode(this@buttonCheckedChangeEvents)
    val listener = listener(scope, corbindEventEmitter())
    addOnButtonCheckedListener(listener)
    awaitClose { removeOnButtonCheckedListener(listener) }
}

/**
 * Create a flow that emits [check change events][MaterialButtonCheckedChangeEvent] for buttons in
 * [MaterialButtonToggleGroup].
 *
 * *Warning:* Requires multiple-selection mode. Use `buttonCheckedChanges` for single selection.
 *
 * Example:
 *
 * ```
 * materialButtonToggleGroup.buttonCheckedChangeEvents()
 *      .onEach { /* handle check change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun MaterialButtonToggleGroup.buttonCheckedChangeEvents(): Flow<MaterialButtonCheckedChangeEvent> = corbindCallbackFlow {
    checkSelectionMode(this@buttonCheckedChangeEvents)
    val listener = listener(this, corbindEventEmitter())
    addOnButtonCheckedListener(listener)
    awaitClose { removeOnButtonCheckedListener(listener) }
}

private fun checkSelectionMode(group: MaterialButtonToggleGroup) {
    check(!group.isSingleSelection) {
        "The MaterialButtonToggleGroup is in single selection mode. " +
            "Use `buttonCheckedChanges` extension instead"
    }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MaterialButtonCheckedChangeEvent) -> Boolean,
) = MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
    if (scope.isActive) emitter(MaterialButtonCheckedChangeEvent(checkedId, isChecked))
}
