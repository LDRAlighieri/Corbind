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

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import com.google.android.material.appbar.AppBarLayout
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
 * Perform an action on the offset change in [AppBarLayout].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun AppBarLayout.offsetChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (offset in channel) action(offset)
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnOffsetChangedListener(listener)
    events.invokeOnCloseOnMain { removeOnOffsetChangedListener(listener) }
}

/**
 * Perform an action on the offset change in [AppBarLayout], in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun AppBarLayout.offsetChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    offsetChanges(this, capacity, action)
}

/**
 * Create a channel which emits the offset change in [AppBarLayout].
 *
 * Example:
 *
 * ```
 * launch {
 *      appBarLayout.offsetChanges(scope)
 *          .consumeEach { /* handle offset change */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun AppBarLayout.offsetChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addOnOffsetChangedListener(listener)
    awaitClose { removeOnOffsetChangedListener(listener) }
}

/**
 * Create a flow which emits the offset change in [AppBarLayout].
 *
 * Example:
 *
 * ```
 * appBarLayout.offsetChanges()
 *      .onEach { /* handle offset change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun AppBarLayout.offsetChanges(): Flow<Int> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addOnOffsetChangedListener(listener)
    awaitClose { removeOnOffsetChangedListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean,
) = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
    if (scope.isActive) emitter(verticalOffset)
}
