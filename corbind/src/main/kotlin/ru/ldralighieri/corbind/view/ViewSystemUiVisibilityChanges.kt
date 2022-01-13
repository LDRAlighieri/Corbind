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

/**
 * Perform an action on a new system UI visibility for [View].
 *
 * *Warning:* The created actor uses [View.setOnSystemUiVisibilityChangeListener]. Only one actor
 * can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 *
 * @deprecated OnSystemUiVisibilityChangeListener is deprecated. Use
 * {@link WindowInsets#isVisible(int)} to find out about system bar visibilities
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "OnSystemUiVisibilityChangeListener is deprecated. Use " +
        "{@link WindowInsets#isVisible(int)} to find out about system bar visibilities",
    replaceWith = ReplaceWith(
        expression = "windowInsetsApplyEvents(scope, capacity, action)",
        imports = ["ru.ldralighieri.corbind.view.windowInsetsApplies"]
    )
)
fun View.systemUiVisibilityChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (visibility in channel) action(visibility)
    }

    setOnSystemUiVisibilityChangeListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}

/**
 * Perform an action on a new system UI visibility for [View], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnSystemUiVisibilityChangeListener]. Only one actor
 * can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 *
 * @deprecated OnSystemUiVisibilityChangeListener is deprecated. Use
 * {@link WindowInsets#isVisible(int)} to find out about system bar visibilities
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "OnSystemUiVisibilityChangeListener is deprecated. Use " +
        "{@link WindowInsets#isVisible(int)} to find out about system bar visibilities",
    replaceWith = ReplaceWith(
        expression = "windowInsetsApplyEvents(capacity, action)",
        imports = ["ru.ldralighieri.corbind.view.windowInsetsApplies"]
    )
)
suspend fun View.systemUiVisibilityChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    systemUiVisibilityChanges(this, capacity, action)
}

/**
 * Create a channel of integers representing a new system UI visibility for [View].
 *
 * *Warning:* The created channel uses [View.setOnSystemUiVisibilityChangeListener]. Only one
 * channel can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.systemUiVisibilityChanges(scope)
 *          .consumeEach { /* handle system UI visibility */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 *
 * @deprecated OnSystemUiVisibilityChangeListener is deprecated. Use
 * {@link WindowInsets#isVisible(int)} to find out about system bar visibilities
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "OnSystemUiVisibilityChangeListener is deprecated. Use " +
        "{@link WindowInsets#isVisible(int)} to find out about system bar visibilities",
    replaceWith = ReplaceWith(
        expression = "windowInsetsApplyEvents(scope, capacity)",
        imports = ["ru.ldralighieri.corbind.view.windowInsetsApplies"]
    )
)
@CheckResult
fun View.systemUiVisibilityChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    setOnSystemUiVisibilityChangeListener(listener(scope, ::trySend))
    invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}

/**
 * Create a flow of integers representing a new system UI visibility for [View].
 *
 * *Warning:* The created flow uses [View.setOnSystemUiVisibilityChangeListener]. Only one flow can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * view.systemUiVisibilityChanges()
 *      .onEach { /* handle system UI visibility */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @deprecated OnSystemUiVisibilityChangeListener is deprecated. Use
 * {@link WindowInsets#isVisible(int)} to find out about system bar visibilities
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "OnSystemUiVisibilityChangeListener is deprecated. Use " +
        "{@link WindowInsets#isVisible(int)} to find out about system bar visibilities",
    replaceWith = ReplaceWith(
        expression = "windowInsetsApplyEvents()",
        imports = ["ru.ldralighieri.corbind.view.windowInsetsApplies"]
    )
)
@CheckResult
fun View.systemUiVisibilityChanges(): Flow<Int> = channelFlow {
    setOnSystemUiVisibilityChangeListener(listener(this, ::trySend))
    awaitClose { setOnSystemUiVisibilityChangeListener(null) }
}

@Suppress("DEPRECATION")
@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Unit
) = View.OnSystemUiVisibilityChangeListener {
    if (scope.isActive) { emitter(it) }
}
