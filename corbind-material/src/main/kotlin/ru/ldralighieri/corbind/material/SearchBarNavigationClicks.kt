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

package ru.ldralighieri.corbind.material

import android.os.Build
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import com.google.android.material.search.SearchBar
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
 * Perform an action on [SearchBar] navigation click events.
 *
 * *Warning:* The created actor uses [SearchBar.setNavigationOnClickListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun SearchBar.navigationClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setNavigationOnClickListener(listener(scope, events::trySend))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}

/**
 * Perform an action on [SearchBar] navigation click events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [SearchBar.setNavigationOnClickListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun SearchBar.navigationClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    navigationClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [SearchBar] navigation click events.
 *
 * *Warning:* The created channel uses [SearchBar.setNavigationOnClickListener]. Only one channel
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      searchbar.navigationClicks(scope)
 *          .consumeEach { /* handle navigation click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
fun SearchBar.navigationClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setNavigationOnClickListener(listener(scope, ::trySend))
    invokeOnClose { setNavigationOnClickListener(null) }
}

/**
 * Create a flow which emits on [SearchBar] navigation click events.
 *
 * *Warning:* The created flow uses [SearchBar.setNavigationOnClickListener]. Only one flow can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * searchbar.navigationClicks()
 *      .onEach { /* handle navigation click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
fun SearchBar.navigationClicks(): Flow<Unit> = channelFlow {
    setNavigationOnClickListener(listener(this, ::trySend))
    awaitClose { setNavigationOnClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Unit,
) = View.OnClickListener {
    if (scope.isActive) emitter(Unit)
}
