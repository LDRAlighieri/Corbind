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
import com.google.android.material.appbar.AppBarLayout
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
 * Perform an action on the offset change in [AppBarLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun AppBarLayout.offsetChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (offset in channel) action(offset)
    }

    val listener = listener(scope, events::trySend)
    addOnOffsetChangedListener(listener)
    events.invokeOnClose { removeOnOffsetChangedListener(listener) }
}

/**
 * Perform an action on the offset change in [AppBarLayout], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun AppBarLayout.offsetChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
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
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun AppBarLayout.offsetChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    addOnOffsetChangedListener(listener)
    invokeOnClose { removeOnOffsetChangedListener(listener) }
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
fun AppBarLayout.offsetChanges(): Flow<Int> = channelFlow {
    val listener = listener(this, ::trySend)
    addOnOffsetChangedListener(listener)
    awaitClose { removeOnOffsetChangedListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Unit
) = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
    if (scope.isActive) { emitter(verticalOffset) }
}
