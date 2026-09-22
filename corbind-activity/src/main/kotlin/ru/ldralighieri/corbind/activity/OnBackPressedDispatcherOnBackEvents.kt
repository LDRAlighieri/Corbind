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
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
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

sealed interface OnBackEvent
data object OnBackPressed : OnBackEvent
data object OnBackCanceled : OnBackEvent
data class OnBackStarted(val backEvent: BackEventCompat) : OnBackEvent
data class OnBackProgressed(val backEvent: BackEventCompat) : OnBackEvent

/**
 * Perform an action on back events.
 *
 * @param scope Coroutine scope that owns the binding
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun OnBackPressedDispatcher.backEvents(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (OnBackEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<OnBackEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val callback = callback(scope, events.corbindEventEmitter(scope))
    addCallback(lifecycleOwner, callback)
    events.invokeOnCloseOnMain { callback.remove() }
}

/**
 * Perform an action on back events, in a new [CoroutineScope].
 *
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun OnBackPressedDispatcher.backEvents(
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (OnBackEvent) -> Unit,
) = coroutineScope {
    backEvents(this, lifecycleOwner, capacity, action)
}

/**
 * Create a channel that emits back events.
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
 * @param scope Coroutine scope that owns the binding
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
fun OnBackPressedDispatcher.backEvents(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<OnBackEvent> = corbindReceiveChannel(scope, capacity) {
    val callback = callback(scope, corbindEventEmitter())
    addCallback(lifecycleOwner, callback)
    awaitClose { callback.remove() }
}

/**
 * Create a flow that emits back events.
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
fun OnBackPressedDispatcher.backEvents(lifecycleOwner: LifecycleOwner): Flow<OnBackEvent> = corbindCallbackFlow {
    val callback = callback(this, corbindEventEmitter())
    addCallback(lifecycleOwner, callback)
    awaitClose { callback.remove() }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (OnBackEvent) -> Boolean,
) = object : OnBackPressedCallback(true) {

    override fun handleOnBackPressed() = onEvent(OnBackPressed)
    override fun handleOnBackCancelled() = onEvent(OnBackCanceled)
    override fun handleOnBackStarted(backEvent: BackEventCompat) = onEvent(OnBackStarted(backEvent))
    override fun handleOnBackProgressed(backEvent: BackEventCompat) = onEvent(OnBackProgressed(backEvent))

    private fun onEvent(event: OnBackEvent) {
        if (scope.isActive) emitter(event)
    }
}
