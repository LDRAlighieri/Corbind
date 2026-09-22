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

package ru.ldralighieri.corbind.activity

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

/**
 * Perform an action when [OnBackPressedDispatcher.onBackPressed] is called.
 *
 * @param scope Coroutine scope that owns the binding
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun OnBackPressedDispatcher.backPresses(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val callback = callback(scope, events.corbindEventEmitter(scope))
    addCallback(lifecycleOwner, callback)
    events.invokeOnCloseOnMain { callback.remove() }
}

/**
 * Perform an action when [OnBackPressedDispatcher.onBackPressed] is called, in a new [CoroutineScope].
 *
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun OnBackPressedDispatcher.backPresses(
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    backPresses(this, lifecycleOwner, capacity, action)
}

/**
 * Create a channel that emits an event when [OnBackPressedDispatcher.onBackPressed] is called.
 *
 * Example:
 *
 * ```
 * launch {
 *      onBackPressedDispatcher.backPresses(scope, lifecycleOwner = this)
 *          .consumeEach { /* handle back pressed event */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
fun OnBackPressedDispatcher.backPresses(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    val callback = callback(scope, corbindEventEmitter())
    addCallback(lifecycleOwner, callback)
    awaitClose { callback.remove() }
}

/**
 * Create a flow that emits an event when [OnBackPressedDispatcher.onBackPressed] is called.
 *
 * Example:
 *
 * ```
 * onBackPressedDispatcher.backPresses(lifecycleOwner = this)
 *      .onEach { /* handle back pressed event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 */
fun OnBackPressedDispatcher.backPresses(lifecycleOwner: LifecycleOwner): Flow<Unit> = corbindCallbackFlow {
    val callback = callback(this, corbindEventEmitter())
    addCallback(lifecycleOwner, callback)
    awaitClose { callback.remove() }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean,
) = object : OnBackPressedCallback(true) {

    override fun handleOnBackPressed() {
        if (scope.isActive) emitter(Unit)
    }
}
