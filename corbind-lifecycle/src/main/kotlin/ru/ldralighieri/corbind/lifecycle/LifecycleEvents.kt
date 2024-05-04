/*
 * Copyright 2021 Vladimir Raupov
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

package ru.ldralighieri.corbind.lifecycle

import androidx.annotation.CheckResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
 * Perform an action on any [lifecycle][Lifecycle] event change.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Lifecycle.events(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Lifecycle.Event) -> Unit,
) {
    val events = scope.actor<Lifecycle.Event>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val observer = observer(scope, events::trySend)
    addObserver(observer)
    events.invokeOnClose { removeObserver(observer) }
}

/**
 * Perform an action on any [lifecycle][Lifecycle] event change, inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Lifecycle.events(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Lifecycle.Event) -> Unit,
) = coroutineScope {
    events(this, capacity, action)
}

/**
 * Create a channel which emits on any [lifecycle][Lifecycle] event change.
 *
 * Example:
 *
 * ```
 * launch {
 *      lifecycle.events()
 *          .consumeEach { /* handle lifecycle event change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
fun Lifecycle.events(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Lifecycle.Event> = corbindReceiveChannel(capacity) {
    val observer = observer(scope, ::trySend)
    addObserver(observer)
    invokeOnClose { removeObserver(observer) }
}

/**
 * Create a flow which emits on any [lifecycle][Lifecycle] event change.
 *
 * Example:
 *
 * ```
 * lifecycle.events()
 *      .onEach { /* handle lifecycle event change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun Lifecycle.events(): Flow<Lifecycle.Event> = channelFlow {
    val observer = observer(this, ::trySend)
    addObserver(observer)
    awaitClose { removeObserver(observer) }
}

@CheckResult
private fun observer(
    scope: CoroutineScope,
    emitter: (Lifecycle.Event) -> Unit,
) = LifecycleEventObserver { _, event ->
    if (scope.isActive) emitter(event)
}
