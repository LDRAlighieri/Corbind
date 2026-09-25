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
import android.view.ViewTreeObserver
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
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
 * Perform an action on draws on [View].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun View.draws(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    viewTreeObserver.addOnDrawListener(listener)
    events.invokeOnCloseOnMain { viewTreeObserver.removeOnDrawListener(listener) }
}

/**
 * Perform an action on draws on [View], in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun View.draws(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    draws(this, capacity, action)
}

/**
 * Create a channel for draws on [View].
 *
 * Example:
 *
 * ```
 * launch {
 *      view.draws(scope)
 *          .consumeEach { /* handle draw */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun View.draws(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    viewTreeObserver.addOnDrawListener(listener)
    awaitClose { viewTreeObserver.removeOnDrawListener(listener) }
}

/**
 * Create a flow for draws on [View].
 *
 * Example:
 *
 * ```
 * view.draws()
 *      .onEach { /* handle draw */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.draws(): Flow<Unit> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    viewTreeObserver.addOnDrawListener(listener)
    awaitClose { viewTreeObserver.removeOnDrawListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean,
) = ViewTreeObserver.OnDrawListener {
    if (scope.isActive) emitter(Unit)
}
