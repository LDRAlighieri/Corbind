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

data class ViewLayoutChangeEvent(
    val view: View,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val oldLeft: Int,
    val oldTop: Int,
    val oldRight: Int,
    val oldBottom: Int
)

/**
 * Perform an action on [layout change events][ViewLayoutChangeEvent] for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.layoutChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewLayoutChangeEvent) -> Unit
) {
    val events = scope.actor<ViewLayoutChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::trySend)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}

/**
 * Perform an action on [layout change events][ViewLayoutChangeEvent] for [View], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.layoutChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewLayoutChangeEvent) -> Unit
) = coroutineScope {
    layoutChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [layout change events][ViewLayoutChangeEvent] for [View].
 *
 * Example:
 *
 * ```
 * launch {
 *      view.layoutChangeEvents(scope)
 *          .consumeEach { /* handle layout change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.layoutChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewLayoutChangeEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    addOnLayoutChangeListener(listener)
    invokeOnClose { removeOnLayoutChangeListener(listener) }
}

/**
 * Create a flow of [layout change events][ViewLayoutChangeEvent] for [View].
 *
 * Example:
 *
 * ```
 * view.layoutChangeEvents()
 *      .onEach { /* handle layout change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.layoutChangeEvents(): Flow<ViewLayoutChangeEvent> = channelFlow {
    val listener = listener(this, ::trySend)
    addOnLayoutChangeListener(listener)
    awaitClose { removeOnLayoutChangeListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (ViewLayoutChangeEvent) -> Unit
) = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
    if (scope.isActive) {
        emitter(
            ViewLayoutChangeEvent(
                v, left, top, right, bottom, oldLeft, oldTop, oldRight,
                oldBottom
            )
        )
    }
}
