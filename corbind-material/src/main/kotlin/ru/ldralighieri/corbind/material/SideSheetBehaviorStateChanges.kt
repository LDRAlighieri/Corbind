/*
 * Copyright 2022 Vladimir Raupov
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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.sidesheet.SideSheetCallback
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
 * Perform an action on the state change events from [View] on [SideSheetBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.sideSheetStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val behavior = getSideSheetBehavior()
    events.trySend(behavior.state)
    val callback = callback(scope, events::trySend)
    behavior.addCallback(callback)
    events.invokeOnClose { behavior.removeCallback(callback) }
}

/**
 * Perform an action on the state change events from [View] on [SideSheetBehavior], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.sideSheetStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    sideSheetStateChanges(this, capacity, action)
}

/**
 * Create a channel which emits the state change events from [View] on [SideSheetBehavior].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      sideSheetBehavior.sideSheetStateChanges(scope)
 *          .consumeEach { /* handle state change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.sideSheetStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val behavior = getSideSheetBehavior()
    trySend(behavior.state)
    val callback = callback(scope, ::trySend)
    behavior.addCallback(callback)
    invokeOnClose { behavior.removeCallback(callback) }
}

/**
 * Create a flow which emits the state change events from [View] on [SideSheetBehavior].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * sideSheetBehavior.sideSheetStateChanges()
 *      .onEach { /* handle state change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * sideSheetBehavior.sideSheetStateChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle state change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.sideSheetStateChanges(): InitialValueFlow<Int> = channelFlow {
    val behavior = getSideSheetBehavior()
    val callback = callback(this, ::trySend)
    behavior.addCallback(callback)
    awaitClose { behavior.removeCallback(callback) }
}.asInitialValueFlow(getSideSheetBehavior().state)

internal fun View.getSideSheetBehavior(): SideSheetBehavior<*> {
    val params = layoutParams as? CoordinatorLayout.LayoutParams
        ?: throw IllegalArgumentException("The view is not in a Coordinator Layout.")
    return params.behavior as SideSheetBehavior<*>?
        ?: throw IllegalStateException("There's no SideSheetBehavior set on this view.")
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Int) -> Unit,
) = object : SideSheetCallback() {

    override fun onStateChanged(sheet: View, newState: Int) {
        if (scope.isActive) emitter(newState)
    }

    override fun onSlide(sheet: View, slideOffset: Float) = Unit
}
