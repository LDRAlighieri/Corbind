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
import androidx.annotation.MainThread
import com.google.android.material.search.SearchView
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

data class SearchViewTransitionStateChangeEvent(
    val view: SearchView,
    val previousState: SearchView.TransitionState,
    val newState: SearchView.TransitionState,
)

/**
 * Perform an action on the
 * [transition state change event][SearchViewTransitionStateChangeEvent] on [SearchView].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun SearchView.transitionStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewTransitionStateChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<SearchViewTransitionStateChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addTransitionListener(listener)
    events.invokeOnCloseOnMain { removeTransitionListener(listener) }
}

/**
 * Perform an action on the
 * [transition state change event][SearchViewTransitionStateChangeEvent] on [SearchView], in a new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun SearchView.transitionStateChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewTransitionStateChangeEvent) -> Unit,
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun SearchView.transitionStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<SearchViewTransitionStateChangeEvent> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addTransitionListener(listener)
    awaitClose { removeTransitionListener(listener) }
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
fun SearchView.transitionStateChangeEvents(): Flow<SearchViewTransitionStateChangeEvent> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addTransitionListener(listener)
    awaitClose { removeTransitionListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (SearchViewTransitionStateChangeEvent) -> Boolean,
) = SearchView.TransitionListener { searchView, previousState, newState ->
    if (scope.isActive) {
        emitter(SearchViewTransitionStateChangeEvent(searchView, previousState, newState))
    }
}
