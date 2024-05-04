/*
 * Copyright 2022 Vladimir Raupov
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

package ru.ldralighieri.corbind.fragment

import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.fragment.app.FragmentManager
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

data class FragmentResultEvent(
    val requestKey: String,
    val bundle: Bundle,
)

/**
 * Perform an action on any results set by setFragmentResult using the [requestKey], once the given
 * [lifecycleOwner][LifecycleOwner] is at least in the STARTED state
 *
 * *Warning:* The created actor uses [FragmentManager.setFragmentResultListener]. Only one flow can
 * be used at a time
 *
 * @param scope Root coroutine scope
 * @param requestKey Used to identify the result
 * @param lifecycleOwner The LifecycleOwner for handling the result
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun FragmentManager.resultEvents(
    scope: CoroutineScope,
    requestKey: String,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (FragmentResultEvent) -> Unit,
) {
    val events = scope.actor<FragmentResultEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::trySend)
    setFragmentResultListener(requestKey, lifecycleOwner, listener)
    events.invokeOnClose { clearFragmentResultListener(requestKey) }
}

/**
 * Perform an action inside new [CoroutineScope] on any results set by setFragmentResult using the
 * [requestKey], once the given [lifecycleOwner][LifecycleOwner] is at least in the STARTED state
 *
 * *Warning:* The created actor uses [FragmentManager.setFragmentResultListener]. Only one flow can
 * be used at a time
 *
 * @param requestKey Used to identify the result
 * @param lifecycleOwner The LifecycleOwner for handling the result
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun FragmentManager.resultEvents(
    requestKey: String,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (FragmentResultEvent) -> Unit,
) = coroutineScope {
    resultEvents(this, requestKey, lifecycleOwner, capacity, action)
}

/**
 * Create a channel which emits any results set by setFragmentResult using the [requestKey], once
 * the given [lifecycleOwner][LifecycleOwner] is at least in the STARTED state
 *
 * *Warning:* The created channel uses [FragmentManager.setFragmentResultListener]. Only one flow
 * can be used at a time
 *
 * Example:
 *
 * ```
 * launch {
 *      parentFragmentManager.resultEvents(
 *          requestKey = FRAGMENT_REQUEST_KEY,
 *          lifecycleOwner = this@CurrentFragment
 *      )
 *          .consumeEach { /* handle result event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param requestKey Used to identify the result
 * @param lifecycleOwner The LifecycleOwner for handling the result
 */
fun FragmentManager.resultEvents(
    scope: CoroutineScope,
    requestKey: String,
    lifecycleOwner: LifecycleOwner,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<FragmentResultEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    setFragmentResultListener(requestKey, lifecycleOwner, listener)
    invokeOnClose { clearFragmentResultListener(requestKey) }
}

/**
 * Create a flow which emits any results set by setFragmentResult using the [requestKey], once the
 * given [lifecycleOwner][LifecycleOwner] is at least in the STARTED state
 *
 * *Warning:* The created flow uses [FragmentManager.setFragmentResultListener]. Only one flow can
 * be used at a time
 *
 * Example:
 * ```
 * lifecycleScope.launchWhenStarted {
 *      parentFragmentManager.resultEvents(
 *          requestKey = FRAGMENT_REQUEST_KEY,
 *          lifecycleOwner = this@CurrentFragment
 *      )
 *          .onEach { event -> /* handle result event */ }
 *          .launchIn(this@launchWhenStarted) // lifecycle-runtime-ktx
 * }
 * ```
 *
 * @param requestKey Used to identify the result
 * @param lifecycleOwner The LifecycleOwner for handling the result
 */
fun FragmentManager.resultEvents(
    requestKey: String,
    lifecycleOwner: LifecycleOwner,
): Flow<FragmentResultEvent> = channelFlow {
    val listener = listener(this, ::trySend)
    setFragmentResultListener(requestKey, lifecycleOwner, listener)
    awaitClose { clearFragmentResultListener(requestKey) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (FragmentResultEvent) -> Unit,
) = { requestKey: String, bundle: Bundle ->
    if (scope.isActive) emitter(FragmentResultEvent(requestKey, bundle))
}
