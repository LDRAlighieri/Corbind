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
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
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
 * Perform an action on the bottom view scroll state change events from [View] on
 * [HideBottomViewOnScrollBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.bottomViewScrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val behavior = getBehavior()
    val listener = listener(scope, events::trySend)
    behavior.addOnScrollStateChangedListener(listener)
    events.invokeOnClose { behavior.removeOnScrollStateChangedListener(listener) }
}

/**
 * Perform an action on the bottom view scroll state change events from [View] on
 * [HideBottomViewOnScrollBehavior], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.bottomViewScrollStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    bottomViewScrollStateChanges(this, capacity, action)
}

/**
 * Create a channel which emits the bottom view scroll state change events from [View] on
 * [HideBottomViewOnScrollBehavior].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * launch {
 *      bottomView.bottomViewScrollStateChanges(scope)
 *          .consumeEach { /* handle bottom view scroll state change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.bottomViewScrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val behavior = getBehavior()
    val listener = listener(scope, ::trySend)
    behavior.addOnScrollStateChangedListener(listener)
    invokeOnClose { behavior.removeOnScrollStateChangedListener(listener) }
}

/**
 * Create a flow which emits the bottom view scroll state change events from [View] on
 * [HideBottomViewOnScrollBehavior].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * bottomView.bottomViewScrollStateChanges()
 *      .onEach { /* handle bottom view scroll state change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.bottomViewScrollStateChanges(): Flow<Int> = channelFlow {
    val behavior = getBehavior()
    val listener = listener(this, ::trySend)
    behavior.addOnScrollStateChangedListener(listener)
    awaitClose { behavior.removeOnScrollStateChangedListener(listener) }
}

private fun View.getBehavior(): HideBottomViewOnScrollBehavior<*> {
    val params = layoutParams as? CoordinatorLayout.LayoutParams
        ?: throw IllegalArgumentException("The view is not in a Coordinator Layout.")
    return params.behavior as HideBottomViewOnScrollBehavior<*>?
        ?: throw IllegalStateException("There's no HideBottomViewOnScrollBehavior set on this view.")
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Unit
) = HideBottomViewOnScrollBehavior.OnScrollStateChangedListener { _, newState ->
    if (scope.isActive) { emitter(newState) }
}
