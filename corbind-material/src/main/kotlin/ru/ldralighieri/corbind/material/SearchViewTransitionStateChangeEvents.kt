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

data class SearchViewTransitionStateChangeEvent(
    val view: SearchView,
    val previousState: SearchView.TransitionState,
    val newState: SearchView.TransitionState
)

/**
 * Perform an action on the
 * [transition state change event][SearchViewTransitionStateChangeEvent] on [SearchView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SearchView.transitionStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewTransitionStateChangeEvent) -> Unit
) {
    val events = scope.actor<SearchViewTransitionStateChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::trySend)
    addTransitionListener(listener)
    events.invokeOnClose { removeTransitionListener(listener) }
}

/**
 * Perform an action on the
 * [transition state change event][SearchViewTransitionStateChangeEvent] on [SearchView], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchView.transitionStateChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewTransitionStateChangeEvent) -> Unit
) = coroutineScope {
    transitionStateChangeEvents(this, capacity, action)
}

/**
 * Create a channel which emits the
 * [transition state change event][SearchViewTransitionStateChangeEvent] on [SearchView].
 *
 * Example:
 *
 * ```
 * launch {
 *      searchView.transitionStateChangeEvents(scope)
 *          .consumeEach { /* handle transition state change even */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchView.transitionStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SearchViewTransitionStateChangeEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    addTransitionListener(listener)
    invokeOnClose { removeTransitionListener(listener) }
}

/**
 * Create a flow which emits the
 * [transition state change event][SearchViewTransitionStateChangeEvent] on [SearchView].
 *
 * Example:
 *
 * ```
 * searchView.transitionStateChangeEvents()
 *      .onEach { /* handle transition state change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SearchView.transitionStateChangeEvents(): Flow<SearchViewTransitionStateChangeEvent> =
    channelFlow {
        val listener = listener(this, ::trySend)
        addTransitionListener(listener)
        awaitClose { removeTransitionListener(listener) }
    }

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (SearchViewTransitionStateChangeEvent) -> Unit
) = SearchView.TransitionListener { searchView, previousState, newState ->
    if (scope.isActive) {
        emitter(SearchViewTransitionStateChangeEvent(searchView, previousState, newState))
    }
}
