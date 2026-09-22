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
 * Perform an action on [View] attach events.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun View.attaches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, true, events.corbindEventEmitter(scope))
    addOnAttachStateChangeListener(listener)
    events.invokeOnCloseOnMain { removeOnAttachStateChangeListener(listener) }
}

/**
 * Perform an action on [View] attach events, in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun View.attaches(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    attaches(this, capacity, action)
}

/**
 * Create a channel that emits [View] attach events.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.attaches(scope)
 *          .consumeEach { /* handle attach */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun View.attaches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, true, corbindEventEmitter())
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Create a flow that emits [View] attach events.
 *
 * Example:
 *
 * ```
 * view.attaches()
 *      .onEach { /* handle attach */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.attaches(): Flow<Unit> = corbindCallbackFlow {
    val listener = listener(this, true, corbindEventEmitter())
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Perform an action on [View] detach events.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun View.detaches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, false, events.corbindEventEmitter(scope))
    addOnAttachStateChangeListener(listener)
    events.invokeOnCloseOnMain { removeOnAttachStateChangeListener(listener) }
}

/**
 * Perform an action on [View] detach events, in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun View.detaches(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    detaches(this, capacity, action)
}

/**
 * Create a channel that emits [View] detach events.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.detaches(scope)
 *          .consumeEach { /* handle detach */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun View.detaches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, false, corbindEventEmitter())
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Create a flow that emits [View] detach events.
 *
 * Example:
 *
 * ```
 * view.detaches()
 *      .onEach { /* handle detach */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.detaches(): Flow<Unit> = corbindCallbackFlow {
    val listener = listener(this, false, corbindEventEmitter())
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    callOnAttach: Boolean,
    emitter: (Unit) -> Boolean,
) = object : View.OnAttachStateChangeListener {

    override fun onViewDetachedFromWindow(v: View) {
        if (callOnAttach && scope.isActive) emitter(Unit)
    }

    override fun onViewAttachedToWindow(v: View) {
        if (!callOnAttach && scope.isActive) emitter(Unit)
    }
}
