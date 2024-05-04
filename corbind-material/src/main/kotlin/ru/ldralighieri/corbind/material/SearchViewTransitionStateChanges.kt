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

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.search.SearchView
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
 * Perform an action on the transition state change events on [SearchView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SearchView.transitionStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchView.TransitionState) -> Unit,
) {
    val events = scope.actor<SearchView.TransitionState>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val listener = listener(scope, events::trySend)
    addTransitionListener(listener)
    events.invokeOnClose { removeTransitionListener(listener) }
}

/**
 * Perform an action on the transition state change events on [SearchView], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchView.transitionStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchView.TransitionState) -> Unit,
) = coroutineScope {
    transitionStateChanges(this, capacity, action)
}

/**
 * Create a channel which emits the transition state change events on [SearchView].
 *
 * Example:
 *
 * ```
 * launch {
 *      searchView.transitionStateChanges(scope)
 *          .consumeEach { /* handle transition state change even */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchView.transitionStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<SearchView.TransitionState> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    addTransitionListener(listener)
    invokeOnClose { removeTransitionListener(listener) }
}

/**
 * Create a flow which emits the transition state change events on [SearchView].
 *
 * Example:
 *
 * ```
 * searchView.transitionStateChanges()
 *      .onEach { /* handle transition state change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SearchView.transitionStateChanges(): Flow<SearchView.TransitionState> = channelFlow {
    val listener = listener(this, ::trySend)
    addTransitionListener(listener)
    awaitClose { removeTransitionListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (SearchView.TransitionState) -> Unit,
) = SearchView.TransitionListener { _, _, newState ->
    if (scope.isActive) emitter(newState)
}
