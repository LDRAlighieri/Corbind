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
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

/**
 * Perform an action on the slide offset events from [View] on [BottomSheetBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.slides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) {
    val events = scope.actor<Float>(Dispatchers.Main.immediate, capacity) {
        for (offset in channel) action(offset)
    }

    val behavior = getBottomSheetBehavior()
    val callback = callback(scope, events::trySend)
    behavior.addBottomSheetCallback(callback)
    events.invokeOnClose { behavior.removeBottomSheetCallback(callback) }
}

/**
 * Perform an action on the slide offset events from [View] on [BottomSheetBehavior], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.slides(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) = coroutineScope {
    slides(this, capacity, action)
}

/**
 * Create a channel which emits the slide offset events from [View] on [BottomSheetBehavior].
 *
 * Example:
 *
 * ```
 * launch {
 *      bottomSheetBehavior.slides(scope)
 *          .consumeEach { /* handle slide offset */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.slides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Float> = corbindReceiveChannel(capacity) {
    val behavior = getBottomSheetBehavior()
    val callback = callback(scope, ::trySend)
    behavior.addBottomSheetCallback(callback)
    invokeOnClose { behavior.removeBottomSheetCallback(callback) }
}

/**
 * Create a flow which emits the slide offset events from [View] on [BottomSheetBehavior].
 *
 * Example:
 *
 * ```
 * bottomSheetBehavior.slides()
 *      .onEach { /* handle slide offset */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.slides(): Flow<Float> = channelFlow {
    val behavior = getBottomSheetBehavior()
    val callback = callback(this, ::trySend)
    behavior.addBottomSheetCallback(callback)
    awaitClose { behavior.removeBottomSheetCallback(callback) }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Float) -> Unit,
) = object : BottomSheetBehavior.BottomSheetCallback() {

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        if (scope.isActive) emitter(slideOffset)
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) = Unit
}
