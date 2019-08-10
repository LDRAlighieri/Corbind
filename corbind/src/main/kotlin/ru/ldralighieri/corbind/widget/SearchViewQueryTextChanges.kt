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

package ru.ldralighieri.corbind.widget

import android.widget.SearchView
import androidx.annotation.CheckResult
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
import ru.ldralighieri.corbind.internal.offerElement

/**
 * Perform an action on character sequences for query text changes on [SearchView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SearchView.queryTextChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CharSequence) -> Unit
) {

    val events = scope.actor<CharSequence>(Dispatchers.Main, capacity) {
        for (chars in channel) action(chars)
    }

    events.offer(query)
    setOnQueryTextListener(listener(scope, events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

/**
 * Perform an action on character sequences for query text changes on [SearchView] inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchView.queryTextChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CharSequence) -> Unit
) = coroutineScope {

    val events = actor<CharSequence>(Dispatchers.Main, capacity) {
        for (chars in channel) action(chars)
    }

    events.offer(query)
    setOnQueryTextListener(listener(this, events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

/**
 * Create an observable of character sequences for query text changes on [SearchView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchView.queryTextChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<CharSequence> = corbindReceiveChannel(capacity) {
    offerElement(query)
    setOnQueryTextListener(listener(scope, ::offerElement))
    invokeOnClose { setOnQueryTextListener(null) }
}

/**
 * Create an observable of character sequences for query text changes on [SearchView].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun SearchView.queryTextChanges(): Flow<CharSequence> = channelFlow {
    offer(query)
    setOnQueryTextListener(listener(this, ::offer))
    awaitClose { setOnQueryTextListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (CharSequence) -> Boolean
) = object : SearchView.OnQueryTextListener {

    override fun onQueryTextChange(s: String): Boolean {
        if (scope.isActive) {
            emitter(s)
            return true
        }
        return false
    }

    override fun onQueryTextSubmit(query: String) = false
}
