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
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

/**
 * Perform an action on changes to the checked view IDs in [ChipGroup].
 *
 * *Warning:* The created actor uses [ChipGroup.setOnCheckedStateChangeListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun ChipGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (List<Int>) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<List<Int>>(Dispatchers.Main.immediate, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    events.corbindEventEmitter(scope)(checkedChipIds)
    setOnCheckedStateChangeListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnCheckedStateChangeListener(null) }
}

/**
 * Perform an action on changes to the checked view IDs in [ChipGroup], in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [ChipGroup.setOnCheckedStateChangeListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun ChipGroup.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (List<Int>) -> Unit,
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel that emits changes to the checked view IDs in [ChipGroup].
 *
 * *Warning:* The created channel uses [ChipGroup.setOnCheckedStateChangeListener]. Only one channel
 * can be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun ChipGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<List<Int>> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(checkedChipIds)
    setOnCheckedStateChangeListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnCheckedStateChangeListener(null) }
}

/**
 * Create a flow that emits changes to the checked view IDs in [ChipGroup].
 *
 * *Warning:* The created flow uses [ChipGroup.setOnCheckedStateChangeListener]. Only one flow can
 * be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
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
fun ChipGroup.checkedChanges(): InitialValueFlow<List<Int>> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    setOnCheckedStateChangeListener(listener(this, emitter))
    emitter.sendInitialValue(checkedChipIds)
    awaitClose { setOnCheckedStateChangeListener(null) }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (List<Int>) -> Boolean,
) = ChipGroup.OnCheckedStateChangeListener { _, checkedIds ->
    if (scope.isActive) emitter(checkedIds)
}
