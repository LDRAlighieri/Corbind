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
import androidx.annotation.MainThread
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
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

/**
 * Perform an action on any [lifecycle][Lifecycle] event change.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun Lifecycle.events(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Lifecycle.Event) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Lifecycle.Event>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val observer = observer(scope, events.corbindEventEmitter(scope))
    addObserver(observer)
    events.invokeOnCloseOnMain { removeObserver(observer) }
}

/**
 * Perform an action on any [lifecycle][Lifecycle] event change, in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun Lifecycle.events(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Lifecycle.Event) -> Unit,
) = coroutineScope {
    events(this, capacity, action)
}

/**
 * Create a channel that emits any [lifecycle][Lifecycle] event change.
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
fun Lifecycle.events(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Lifecycle.Event> = corbindReceiveChannel(scope, capacity) {
    val observer = observer(scope, corbindEventEmitter())
    addObserver(observer)
    awaitClose { removeObserver(observer) }
}

/**
 * Create a flow that emits any [lifecycle][Lifecycle] event change.
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
fun Lifecycle.events(): Flow<Lifecycle.Event> = corbindCallbackFlow {
    val observer = observer(this, corbindEventEmitter())
    addObserver(observer)
    awaitClose { removeObserver(observer) }
}

@CheckResult
private fun observer(
    scope: CoroutineScope,
    emitter: (Lifecycle.Event) -> Boolean,
) = LifecycleEventObserver { _, event ->
    if (scope.isActive) emitter(event)
}
