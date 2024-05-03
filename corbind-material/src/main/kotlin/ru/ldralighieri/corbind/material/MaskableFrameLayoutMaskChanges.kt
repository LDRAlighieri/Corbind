/*
 * Copyright 2023 Vladimir Raupov
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

import android.graphics.RectF
import androidx.annotation.CheckResult
import com.google.android.material.carousel.MaskableFrameLayout
import com.google.android.material.carousel.OnMaskChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel

/**
 * Perform an action when changes in a [mask's][MaskableFrameLayout] [RectF] occur.
 *
 * *Warning:* The created actor uses [OnMaskChangedListener]. Only one actor can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun MaskableFrameLayout.maskChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RectF) -> Unit,
) {
    val events = scope.actor<RectF>(Dispatchers.Main.immediate, capacity) {
        for (changes in channel) action(changes)
    }

    setOnMaskChangedListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnMaskChangedListener(null) }
}

/**
 * Perform an action when changes in a [mask's][MaskableFrameLayout] [RectF] occur, inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [OnMaskChangedListener]. Only one actor can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun MaskableFrameLayout.maskChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RectF) -> Unit,
) = coroutineScope {
    maskChanges(this, capacity, action)
}

/**
 * Create a channel which emits when changes in a [mask's][MaskableFrameLayout] [RectF] occur.
 *
 * *Warning:* The created channel uses [OnMaskChangedListener]. Only one channel can be used at a
 * time.
 *
 * Examples:
 *
 * ```
 * launch {
 *      (viewHolder.itemView as MaskableFrameLayout).maskChanges(scope)
 *          .consumeEach { /* handle mask change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun MaskableFrameLayout.maskChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<RectF> = corbindReceiveChannel(capacity) {
    setOnMaskChangedListener(listener(scope, ::trySend))
    invokeOnClose { setOnMaskChangedListener(null) }
}

/**
 * Create a flow which emits when changes in a [mask's][MaskableFrameLayout] [RectF] occur.
 *
 * *Warning:* The created flow uses [OnMaskChangedListener]. Only one flow can be used at a time.
 *
 * Examples:
 *
 * ```
 * (viewHolder.itemView as MaskableFrameLayout).maskChanges()
 *      .onEach { /* handle mask change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun MaskableFrameLayout.maskChanges() = channelFlow {
    setOnMaskChangedListener(listener(this, ::trySend))
    awaitClose { setOnMaskChangedListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (RectF) -> Unit,
) = OnMaskChangedListener { maskRect ->
    if (scope.isActive) emitter(maskRect)
}
