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
 * Perform an action on pre-draws on [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param proceedDrawingPass Let drawing process proceed
 * @param action An action to perform
 */
fun View.preDraws(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    proceedDrawingPass: () -> Boolean,
    action: suspend () -> Unit,
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, proceedDrawingPass, events::trySend)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

/**
 * Perform an action on pre-draws on [View], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param proceedDrawingPass Let drawing process proceed
 * @param action An action to perform
 */
suspend fun View.preDraws(
    capacity: Int = Channel.RENDEZVOUS,
    proceedDrawingPass: () -> Boolean,
    action: suspend () -> Unit,
) = coroutineScope {
    preDraws(this, capacity, proceedDrawingPass, action)
}

/**
 * Create a channel for pre-draws on [View].
 *
 * Example:
 *
 * ```
 * launch {
 *      view.preDraws(scope)
 *          .consumeEach { /* handle pre-draws */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param proceedDrawingPass Let drawing process proceed
 */
@CheckResult
fun View.preDraws(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    proceedDrawingPass: () -> Boolean,
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, proceedDrawingPass, ::trySend)
    viewTreeObserver.addOnPreDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

/**
 * Create a flow for pre-draws on [View].
 *
 * Example:
 *
 * ```
 * view.preDraws()
 *      .onEach { /* handle pre-draws */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param proceedDrawingPass Let drawing process proceed
 */
@CheckResult
fun View.preDraws(
    proceedDrawingPass: () -> Boolean,
): Flow<Unit> = channelFlow {
    val listener = listener(this, proceedDrawingPass, ::trySend)
    viewTreeObserver.addOnPreDrawListener(listener)
    awaitClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    proceedDrawingPass: () -> Boolean,
    emitter: (Unit) -> Unit,
) = ViewTreeObserver.OnPreDrawListener {
    if (scope.isActive) {
        emitter(Unit)
        return@OnPreDrawListener proceedDrawingPass()
    }
    return@OnPreDrawListener true
}
