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

package ru.ldralighieri.corbind.recyclerview

import android.view.View
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
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

sealed interface RecyclerViewChildAttachStateChangeEvent {
    val view: RecyclerView
    val child: View
}

data class RecyclerViewChildAttachEvent(
    override val view: RecyclerView,
    override val child: View
) : RecyclerViewChildAttachStateChangeEvent

data class RecyclerViewChildDetachEvent(
    override val view: RecyclerView,
    override val child: View
) : RecyclerViewChildAttachStateChangeEvent

/**
 * Perform an action on [child attach state change events][RecyclerViewChildAttachStateChangeEvent]
 * on [RecyclerView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RecyclerView.childAttachStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RecyclerViewChildAttachStateChangeEvent) -> Unit
) {
    val events = scope.actor<RecyclerViewChildAttachStateChangeEvent>(
        Dispatchers.Main.immediate,
        capacity
    ) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, this, events::trySend)
    addOnChildAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}

/**
 * Perform an action on [child attach state change events][RecyclerViewChildAttachStateChangeEvent]
 * on [RecyclerView], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RecyclerView.childAttachStateChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RecyclerViewChildAttachStateChangeEvent) -> Unit
) = coroutineScope {
    childAttachStateChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [child attach state change events][RecyclerViewChildAttachStateChangeEvent]
 * on [RecyclerView].
 *
 * Examples:
 *
 * ```
 * // handle all events
 * launch {
 *      recyclerView.childAttachStateChangeEvents(scope)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is RecyclerViewChildAttachEvent -> { /* handle child attach event */ }
 *                  is RecyclerViewChildDetachEvent -> { /* handle child detach event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      recyclerView.childAttachStateChangeEvents(scope)
 *          .filterIsInstance<RecyclerViewChildAttachEvent>()
 *          .consumeEach { /* handle child attach event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RecyclerView.childAttachStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<RecyclerViewChildAttachStateChangeEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, this@childAttachStateChangeEvents, ::trySend)
    addOnChildAttachStateChangeListener(listener)
    invokeOnClose { removeOnChildAttachStateChangeListener(listener) }
}

/**
 * Create a flow of [child attach state change events][RecyclerViewChildAttachStateChangeEvent] on
 * [RecyclerView].
 *
 * Examples:
 *
 * ```
 * // handle all events
 * recyclerView.childAttachStateChangeEvents()
 *      .onEach { event ->
 *          when (event) {
 *              is RecyclerViewChildAttachEvent -> { /* handle child attach event */ }
 *              is RecyclerViewChildDetachEvent -> { /* handle child detach event */ }
 *          }
 *      }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // handle one event
 * recyclerView.childAttachStateChangeEvents()
 *      .filterIsInstance<RecyclerViewChildAttachEvent>()
 *      .onEach { /* handle child attach event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RecyclerView.childAttachStateChangeEvents(): Flow<RecyclerViewChildAttachStateChangeEvent> =
    channelFlow {
        val listener = listener(this, this@childAttachStateChangeEvents, ::trySend)
        addOnChildAttachStateChangeListener(listener)
        awaitClose { removeOnChildAttachStateChangeListener(listener) }
    }

@CheckResult
private fun listener(
    scope: CoroutineScope,
    recyclerView: RecyclerView,
    emitter: (RecyclerViewChildAttachStateChangeEvent) -> Unit
) = object : RecyclerView.OnChildAttachStateChangeListener {

    override fun onChildViewAttachedToWindow(childView: View) {
        onEvent(RecyclerViewChildAttachEvent(recyclerView, childView))
    }

    override fun onChildViewDetachedFromWindow(childView: View) {
        onEvent(RecyclerViewChildDetachEvent(recyclerView, childView))
    }

    private fun onEvent(event: RecyclerViewChildAttachStateChangeEvent) {
        if (scope.isActive) { emitter(event) }
    }
}
