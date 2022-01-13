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

package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
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

sealed class ViewGroupHierarchyChangeEvent {
    abstract val view: ViewGroup
    abstract val child: View
}

data class ViewGroupHierarchyChildViewAddEvent(
    override val view: ViewGroup,
    override val child: View
) : ViewGroupHierarchyChangeEvent()

data class ViewGroupHierarchyChildViewRemoveEvent(
    override val view: ViewGroup,
    override val child: View
) : ViewGroupHierarchyChangeEvent()

/**
 * Perform an action on [hierarchy change events][ViewGroupHierarchyChangeEvent] for [ViewGroup].
 *
 * *Warning:* The created actor uses [ViewGroup.setOnHierarchyChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ViewGroup.changeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewGroupHierarchyChangeEvent) -> Unit
) {
    val events = scope.actor<ViewGroupHierarchyChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    setOnHierarchyChangeListener(listener(scope, this, events::trySend))
    events.invokeOnClose { setOnHierarchyChangeListener(null) }
}

/**
 * Perform an action on [hierarchy change events][ViewGroupHierarchyChangeEvent] for [ViewGroup],
 * inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [ViewGroup.setOnHierarchyChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ViewGroup.changeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewGroupHierarchyChangeEvent) -> Unit
) = coroutineScope {
    changeEvents(this, capacity, action)
}

/**
 * Create a channel of [hierarchy change events][ViewGroupHierarchyChangeEvent] for [ViewGroup].
 *
 * *Warning:* The created channel uses [ViewGroup.setOnHierarchyChangeListener]. Only one channel
 * can be used at a time.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * launch {
 *      viewGroup.changeEvents(scope)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is ViewGroupHierarchyChildViewAddEvent -> { /* handle add event */ }
 *                  is ViewGroupHierarchyChildViewRemoveEvent -> { /* handle remove event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      viewGroup.changeEvents(scope)
 *          .filterIsInstance<ViewGroupHierarchyChildViewAddEvent>()
 *          .consumeEach { /* handle add event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ViewGroup.changeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewGroupHierarchyChangeEvent> = corbindReceiveChannel(capacity) {
    setOnHierarchyChangeListener(listener(scope, this@changeEvents, ::trySend))
    invokeOnClose { setOnHierarchyChangeListener(null) }
}

/**
 * Create a flow of [hierarchy change events][ViewGroupHierarchyChangeEvent] for [ViewGroup].
 *
 * *Warning:* The created flow uses [ViewGroup.setOnHierarchyChangeListener]. Only one flow can be
 * used at a time.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * viewGroup.changeEvents()
 *      .onEach { event ->
 *          when (event) {
 *              is ViewGroupHierarchyChildViewAddEvent -> { /* handle add event */ }
 *              is ViewGroupHierarchyChildViewRemoveEvent -> { /* handle remove event */ }
 *          }
 *      }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // handle one event
 * viewGroup.changeEvents()
 *      .filterIsInstance<ViewGroupHierarchyChildViewAddEvent>()
 *      .onEach { /* handle add event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun ViewGroup.changeEvents(): Flow<ViewGroupHierarchyChangeEvent> = channelFlow {
    setOnHierarchyChangeListener(listener(this, this@changeEvents, ::trySend))
    awaitClose { setOnHierarchyChangeListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    viewGroup: ViewGroup,
    emitter: (ViewGroupHierarchyChangeEvent) -> Unit
) = object : ViewGroup.OnHierarchyChangeListener {

    override fun onChildViewAdded(parent: View, child: View) {
        onEvent(ViewGroupHierarchyChildViewAddEvent(viewGroup, child))
    }

    override fun onChildViewRemoved(parent: View, child: View) {
        onEvent(ViewGroupHierarchyChildViewRemoveEvent(viewGroup, child))
    }

    private fun onEvent(event: ViewGroupHierarchyChangeEvent) {
        if (scope.isActive) { emitter(event) }
    }
}
