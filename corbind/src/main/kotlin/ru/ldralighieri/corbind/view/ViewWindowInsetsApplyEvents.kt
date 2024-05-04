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

package ru.ldralighieri.corbind.view

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
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

data class WindowInsetsEvent(
    val view: View,
    val insets: WindowInsets,
)

/**
 * Perform an action when window insets applying on a view in a custom way.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
fun View.windowInsetsApplyEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (WindowInsetsEvent) -> Unit,
) {
    val events = scope.actor<WindowInsetsEvent>(Dispatchers.Main.immediate, capacity) {
        for (insets in channel) action(insets)
    }

    setOnApplyWindowInsetsListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnApplyWindowInsetsListener(null) }
}

/**
 * Perform an action when window insets applying on a view in a custom way, inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
suspend fun View.windowInsetsApplyEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (WindowInsetsEvent) -> Unit,
) = coroutineScope {
    windowInsetsApplyEvents(this, capacity, action)
}

/**
 * Create a flow which emits when window insets applying on a view in a custom way.
 *
 * Example:
 *
 * ```
 * launch {
 *      decorView.windowInsetsApplyEvents(scope)
 *          .consumeEach { /* handle window insets event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
@CheckResult
fun View.windowInsetsApplyEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<WindowInsetsEvent> = corbindReceiveChannel(capacity) {
    setOnApplyWindowInsetsListener(listener(scope, ::trySend))
    invokeOnClose { setOnApplyWindowInsetsListener(null) }
}

/**
 * Create a flow which emits when window insets applying on a view in a custom way.
 *
 * Example:
 *
 * ```
 * decorView.windowInsetsApplyEvents()
 *      .onEach { /* handle window insets event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * ```
 * decorView.windowInsetsApplyEvents()
 *      .map { event ->
 *          with(event) {
 *              view.onApplyWindowInsets(insets)
 *          }
 *      }
 *      .map { insets ->
 *          insets.isVisible(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
 *       }
 *      .onEach { /* handle status bars or navigation bars visibility */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
@CheckResult
fun View.windowInsetsApplyEvents(): Flow<WindowInsetsEvent> = channelFlow {
    setOnApplyWindowInsetsListener(listener(this, ::trySend))
    awaitClose { setOnApplyWindowInsetsListener(null) }
}

@RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (WindowInsetsEvent) -> Unit,
) = View.OnApplyWindowInsetsListener { v, insets ->
    if (scope.isActive) emitter(WindowInsetsEvent(v, insets))
    insets
}
