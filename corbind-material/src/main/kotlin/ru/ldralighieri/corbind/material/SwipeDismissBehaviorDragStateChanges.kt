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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.SwipeDismissBehavior
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
 * Perform an action on the drag state change events from [View] on [SwipeDismissBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.dragStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val behavior = getBehavior(this)
    behavior.listener = listener(scope, events::trySend)
    events.invokeOnClose { behavior.setListener(null) }
}

/**
 * Perform an action on the drag state change events from [View] on [SwipeDismissBehavior], inside
 * new [CoroutineScope].
 *
 * *Warning:* The created actor uses [SwipeDismissBehavior.setListener]. Only one actor can be used
 * at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.dragStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    dragStateChanges(this, capacity, action)
}

/**
 * Create a channel which emits the drag state change events from [View] on [SwipeDismissBehavior].
 *
 * *Warning:* The created channel uses [SwipeDismissBehavior.setListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      swipeDismissBehavior.dragStateChanges(scope)
 *          .consumeEach { /* handle drag state change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.dragStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val behavior = getBehavior(this@dragStateChanges)
    behavior.listener = listener(scope, ::trySend)
    invokeOnClose { behavior.setListener(null) }
}

/**
 * Create a flow which emits the drag state change events from [View] on [SwipeDismissBehavior].
 *
 * *Warning:* The created flow uses [SwipeDismissBehavior.setListener]. Only one flow can be used at
 * a time.
 *
 * Example:
 *
 * ```
 * swipeDismissBehavior.dragStateChanges()
 *      .onEach { /* handle drag state change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.dragStateChanges(): Flow<Int> = channelFlow {
    val behavior = getBehavior(this@dragStateChanges)
    behavior.listener = listener(this, ::trySend)
    awaitClose { behavior.setListener(null) }
}

@CheckResult
private fun getBehavior(view: View): SwipeDismissBehavior<*> {
    val params = view.layoutParams as? CoordinatorLayout.LayoutParams
        ?: throw IllegalArgumentException("The view is not in a Coordinator Layout.")
    return params.behavior as SwipeDismissBehavior<*>?
        ?: throw IllegalStateException("There's no behavior set on this view.")
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Unit,
) = object : SwipeDismissBehavior.OnDismissListener {

    override fun onDismiss(view: View) = Unit

    override fun onDragStateChanged(state: Int) {
        if (scope.isActive) emitter(state)
    }
}
