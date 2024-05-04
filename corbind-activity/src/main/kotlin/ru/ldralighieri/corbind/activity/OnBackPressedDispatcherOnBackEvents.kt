/*
 * Copyright 2023 Vladimir Raupov
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

package ru.ldralighieri.corbind.activity

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
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

sealed interface OnBackEvent
data object OnBackPressed : OnBackEvent
data object OnBackCanceled : OnBackEvent
data class OnBackStarted(val backEvent: BackEventCompat) : OnBackEvent
data class OnBackProgressed(val backEvent: BackEventCompat) : OnBackEvent

/**
 * Perform an action on back events.
 *
 * @param scope Root coroutine scope
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun OnBackPressedDispatcher.backEvents(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (OnBackEvent) -> Unit,
) {
    val events = scope.actor<OnBackEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val callback = callback(scope, events::trySend)
    addCallback(lifecycleOwner, callback)
    events.invokeOnClose { callback.remove() }
}

/**
 * Perform an action on back events, inside new [CoroutineScope].
 *
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun OnBackPressedDispatcher.backEvents(
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (OnBackEvent) -> Unit,
) = coroutineScope {
    backEvents(this, lifecycleOwner, capacity, action)
}

/**
 * Create a channel which emit back events.
 *
 * Example:
 *
 * ```
 * launch {
 *      onBackPressedDispatcher.backEvents(scope, lifecycleOwner = this)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is OnBackPressed -> { /* handle back pressed event */ }
 *                  is OnBackCanceled -> { /* handle back cancel event */ }
 *                  is OnBackStarted -> { /* handle back started event */ }
 *                  is OnBackProgressed -> { /* handle back progressed event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      tabLayout.backEvents(scope, lifecycleOwner = this)
 *          .filterIsInstance<OnBackProgressed>()
 *          .consumeEach { /* handle back progressed event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
fun OnBackPressedDispatcher.backEvents(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<OnBackEvent> = corbindReceiveChannel(capacity) {
    val callback = callback(scope, ::trySend)
    addCallback(lifecycleOwner, callback)
    invokeOnClose { callback.remove() }
}

/**
 * Create a flow which emit back events.
 *
 * Example:
 *
 * ```
 * onBackPressedDispatcher.backEvents(lifecycleOwner = this)
 *      .onEach { event ->
 *          when (event) {
 *              is OnBackPressed -> { /* handle back pressed event */ }
 *              is OnBackCanceled -> { /* handle back cancel event */ }
 *              is OnBackStarted -> { /* handle back started event */ }
 *              is OnBackProgressed -> { /* handle back progressed event */ }
 *          }
 *      }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * // handle one event
 * onBackPressedDispatcher.backEvents(lifecycleOwner = this)
 *      .filterIsInstance<OnBackProgressed>()
 *      .onEach { /* handle back progressed event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 */
fun OnBackPressedDispatcher.backEvents(lifecycleOwner: LifecycleOwner): Flow<OnBackEvent> =
    channelFlow {
        val callback = callback(this, ::trySend)
        addCallback(lifecycleOwner, callback)
        awaitClose { callback.remove() }
    }

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (OnBackEvent) -> Unit,
) = object : OnBackPressedCallback(true) {

    override fun handleOnBackPressed() = onEvent(OnBackPressed)
    override fun handleOnBackCancelled() = onEvent(OnBackCanceled)
    override fun handleOnBackStarted(backEvent: BackEventCompat) = onEvent(OnBackStarted(backEvent))
    override fun handleOnBackProgressed(backEvent: BackEventCompat) =
        onEvent(OnBackProgressed(backEvent))

    private fun onEvent(event: OnBackEvent) {
        if (scope.isActive) emitter(event)
    }
}
