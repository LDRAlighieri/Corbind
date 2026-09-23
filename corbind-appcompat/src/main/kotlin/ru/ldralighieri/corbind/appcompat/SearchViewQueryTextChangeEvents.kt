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
import ru.ldralighieri.corbind.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

data class SearchViewQueryTextEvent(
    val view: SearchView,
    val queryText: CharSequence,
    val isSubmitted: Boolean,
)

/**
 * Perform an action on [query text events][SearchViewQueryTextEvent] on [SearchView].
 *
 * *Warning:* The created actor uses [SearchView.setOnQueryTextListener]. Only one actor can be used
 * at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun SearchView.queryTextChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewQueryTextEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<SearchViewQueryTextEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.corbindEventEmitter(scope)(SearchViewQueryTextEvent(this, query, false))
    setOnQueryTextListener(listener(scope, this, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnQueryTextListener(null) }
}

/**
 * Perform an action on [query text events][SearchViewQueryTextEvent] on [SearchView], in a new
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
suspend fun SearchView.queryTextChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewQueryTextEvent) -> Unit,
) = coroutineScope {
    queryTextChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [query text events][SearchViewQueryTextEvent] on [SearchView].
 *
 * *Warning:* The created channel uses [SearchView.setOnQueryTextListener]. Only one channel can be
 * used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      searchView.queryTextChangeEvents(scope)
 *          .consumeEach { /* handle query text event */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun SearchView.queryTextChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<SearchViewQueryTextEvent> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(SearchViewQueryTextEvent(this@queryTextChangeEvents, query, false))
    setOnQueryTextListener(listener(scope, this@queryTextChangeEvents, corbindEventEmitter()))
    awaitClose { setOnQueryTextListener(null) }
}

/**
 * Create a flow of [query text events][SearchViewQueryTextEvent] on [SearchView].
 *
 * *Warning:* The created flow uses [SearchView.setOnQueryTextListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * searchView.queryTextChangeEvents()
 *      .onEach { /* handle query text event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * searchView.queryTextChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle query text event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SearchView.queryTextChangeEvents(): InitialValueFlow<SearchViewQueryTextEvent> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    setOnQueryTextListener(listener(this, this@queryTextChangeEvents, emitter))
    emitter.sendInitialValue(
        SearchViewQueryTextEvent(
            view = this@queryTextChangeEvents,
            queryText = this@queryTextChangeEvents.query,
            isSubmitted = false,
        ),
    )
    awaitClose { setOnQueryTextListener(null) }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    searchView: SearchView,
    emitter: (SearchViewQueryTextEvent) -> Boolean,
) = object : SearchView.OnQueryTextListener {

    override fun onQueryTextChange(s: String): Boolean = onEvent(SearchViewQueryTextEvent(searchView, s, false))

    override fun onQueryTextSubmit(query: String): Boolean = onEvent(SearchViewQueryTextEvent(searchView, query, true))

    private fun onEvent(event: SearchViewQueryTextEvent): Boolean {
        if (scope.isActive) {
            return emitter(event)
        }
        return false
    }
}
