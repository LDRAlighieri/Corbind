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

package ru.ldralighieri.corbind.appcompat

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.appcompat.widget.SearchView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

/**
 * Perform an action on character sequences for query text changes on [SearchView].
 *
 * *Warning:* The created actor uses [SearchView.setOnQueryTextListener]. Only one actor can be used
 * at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun SearchView.queryTextChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CharSequence) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<CharSequence>(Dispatchers.Main.immediate, capacity) {
        for (chars in channel) action(chars)
    }

    events.corbindEventEmitter(scope)(query)
    setOnQueryTextListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnQueryTextListener(null) }
}

/**
 * Perform an action on character sequences for query text changes on [SearchView], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [SearchView.setOnQueryTextListener]. Only one actor can be used
 * at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun SearchView.queryTextChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CharSequence) -> Unit,
) = coroutineScope {
    queryTextChanges(this, capacity, action)
}

/**
 * Create a channel of character sequences for query text changes on [SearchView].
 *
 * *Warning:* The created channel uses [SearchView.setOnQueryTextListener]. Only one channel can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      searchView.queryTextChanges(scope)
 *          .consumeEach { /* handle query text change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun SearchView.queryTextChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<CharSequence> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(query)
    setOnQueryTextListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnQueryTextListener(null) }
}

/**
 * Create a flow of character sequences for query text changes on [SearchView].
 *
 * *Warning:* The created flow uses [SearchView.setOnQueryTextListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * searchView.queryTextChanges()
 *      .onEach { /* handle query text change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * searchView.queryTextChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle query text change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SearchView.queryTextChanges(): InitialValueFlow<CharSequence> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    setOnQueryTextListener(listener(this, emitter))
    emitter.sendInitialValue(this@queryTextChanges.query)
    awaitClose { setOnQueryTextListener(null) }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (CharSequence) -> Boolean,
) = object : SearchView.OnQueryTextListener {

    override fun onQueryTextChange(s: String): Boolean {
        if (scope.isActive) {
            return emitter(s)
        }
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean = false
}
