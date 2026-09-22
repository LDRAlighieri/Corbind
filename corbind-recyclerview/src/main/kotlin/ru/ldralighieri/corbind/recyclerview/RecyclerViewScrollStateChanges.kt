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

package ru.ldralighieri.corbind.recyclerview

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
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
 * Perform an action on scroll state changes on [RecyclerView].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun RecyclerView.scrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val scrollListener = listener(scope, events.corbindEventEmitter(scope))
    addOnScrollListener(scrollListener)
    events.invokeOnCloseOnMain { removeOnScrollListener(scrollListener) }
}

/**
 * Perform an action on scroll state changes on [RecyclerView], in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun RecyclerView.scrollStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    scrollStateChanges(this, capacity, action)
}

/**
 * Create a channel of scroll state changes on [RecyclerView].
 *
 * Example:
 *
 * ```
 * launch {
 *      recyclerView.scrollStateChanges(scope)
 *          .consumeEach { /* handle scroll state change */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun RecyclerView.scrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    val scrollListener = listener(scope, corbindEventEmitter())
    addOnScrollListener(scrollListener)
    awaitClose { removeOnScrollListener(scrollListener) }
}

/**
 * Create a flow of scroll state changes on [RecyclerView].
 *
 * Example:
 *
 * ```
 * recyclerView.scrollStateChanges()
 *      .onEach { /* handle scroll state change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RecyclerView.scrollStateChanges(): Flow<Int> = corbindCallbackFlow {
    val scrollListener = listener(this, corbindEventEmitter())
    addOnScrollListener(scrollListener)
    awaitClose { removeOnScrollListener(scrollListener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean,
) = object : RecyclerView.OnScrollListener() {

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (scope.isActive) emitter(newState)
    }
}
