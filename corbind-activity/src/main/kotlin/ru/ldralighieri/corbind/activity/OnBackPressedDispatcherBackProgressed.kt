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

/**
 * Perform an action on [OnBackPressedDispatcher.dispatchOnBackProgressed] call.
 *
 * @param scope Root coroutine scope
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun OnBackPressedDispatcher.backProgressed(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) {
    val events = scope.actor<Float>(Dispatchers.Main.immediate, capacity) {
        for (progress in channel) action(progress)
    }

    val callback = callback(scope, events::trySend)
    addCallback(lifecycleOwner, callback)
    events.invokeOnClose { callback.remove() }
}

/**
 * Perform an action on [OnBackPressedDispatcher.dispatchOnBackProgressed] call, inside new
 * [CoroutineScope].
 *
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun OnBackPressedDispatcher.backProgressed(
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) = coroutineScope {
    backProgressed(this, lifecycleOwner, capacity, action)
}

/**
 * Create a channel which emits back progress on [OnBackPressedDispatcher.dispatchOnBackProgressed]
 * call.
 *
 * Example:
 *
 * ```
 * launch {
 *      onBackPressedDispatcher.backProgressed(scope, lifecycleOwner = this)
 *          .consumeEach { /* handle back progressed event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
fun OnBackPressedDispatcher.backProgressed(
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Float> = corbindReceiveChannel(capacity) {
    val callback = callback(scope, ::trySend)
    addCallback(lifecycleOwner, callback)
    invokeOnClose { callback.remove() }
}

/**
 * Create a flow which emits back progress on [OnBackPressedDispatcher.dispatchOnBackProgressed]
 * call.
 *
 * Example:
 *
 * ```
 * onBackPressedDispatcher.backProgressed(lifecycleOwner = this)
 *      .onEach { /* handle back progressed event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param lifecycleOwner The LifecycleOwner which controls when the callback should be invoked
 */
fun OnBackPressedDispatcher.backProgressed(lifecycleOwner: LifecycleOwner): Flow<Float> =
    channelFlow {
        val callback = callback(this, ::trySend)
        addCallback(lifecycleOwner, callback)
        awaitClose { callback.remove() }
    }

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Float) -> Unit
) = object : OnBackPressedCallback(true) {

    override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        if (scope.isActive) { emitter(backEvent.progress) }
    }

    override fun handleOnBackPressed() = Unit
}
