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
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on [OnBackPressedDispatcher.onBackPressed] call.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param owner The LifecycleOwner which controls when the callback should be invoked
 * @param action An action to perform
 */
fun OnBackPressedDispatcher.backPresses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    owner: LifecycleOwner,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val callback = callback(scope, events::offer)
    addCallback(owner, callback)
    events.invokeOnClose { callback.remove() }
}

/**
 * Perform an action on [OnBackPressedDispatcher.onBackPressed] call, inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param owner The LifecycleOwner which controls when the callback should be invoked
 * @param action An action to perform
 */
suspend fun OnBackPressedDispatcher.backPresses(
    capacity: Int = Channel.RENDEZVOUS,
    owner: LifecycleOwner,
    action: suspend () -> Unit
) = coroutineScope {
    backPresses(this, capacity, owner, action)
}

/**
 * Create a channel which emits on [OnBackPressedDispatcher.onBackPressed] call.
 *
 * Example:
 *
 * ```
 * launch {
 *      requireActivity().onBackPressedDispatcher.backPresses()
 *          .consumeEach { /* handle onBackPressed event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param owner The LifecycleOwner which controls when the callback should be invoked
 */
fun OnBackPressedDispatcher.backPresses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    owner: LifecycleOwner
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val callback = callback(scope, ::offerCatching)
    addCallback(owner, callback)
    invokeOnClose { callback.remove() }
}

/**
 * Create a flow which emits on [OnBackPressedDispatcher.onBackPressed] call.
 *
 * Example:
 *
 * ```
 * requireActivity().onBackPressedDispatcher.backPresses()
 *      .onEach { /* handle onBackPressed event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param owner The LifecycleOwner which controls when the callback should be invoked
 */
fun OnBackPressedDispatcher.backPresses(owner: LifecycleOwner): Flow<Unit> = channelFlow<Unit> {
    val callback = callback(this, ::offerCatching)
    addCallback(owner, callback)
    awaitClose { callback.remove() }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = object : OnBackPressedCallback(true) {

    override fun handleOnBackPressed() {
        if (scope.isActive) { emitter(Unit) }
    }
}
