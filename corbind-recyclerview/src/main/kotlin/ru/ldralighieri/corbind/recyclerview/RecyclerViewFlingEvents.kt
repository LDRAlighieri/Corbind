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
import androidx.recyclerview.widget.RecyclerView
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

data class RecyclerViewFlingEvent(
    val view: RecyclerView,
    val velocityX: Int,
    val velocityY: Int,
)

/**
 * Perform an action on [fling events][RecyclerViewFlingEvent] on [RecyclerView].
 *
 * *Warning:* The created actor uses [RecyclerView.setOnFlingListener]. Only one actor can be used
 * at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RecyclerView.flingEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RecyclerViewFlingEvent) -> Unit,
) {
    val events = scope.actor<RecyclerViewFlingEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    onFlingListener = listener(scope, this, events::trySend)
    events.invokeOnClose { onFlingListener = null }
}

/**
 * Perform an action on [fling events][RecyclerViewFlingEvent] on [RecyclerView], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [RecyclerView.setOnFlingListener]. Only one actor can be used
 * at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RecyclerView.flingEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RecyclerViewFlingEvent) -> Unit,
) = coroutineScope {
    flingEvents(this, capacity, action)
}

/**
 * Create a channel of [fling events][RecyclerViewFlingEvent] on [RecyclerView].
 *
 * *Warning:* The created channel uses [RecyclerView.setOnFlingListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      recyclerView.flingEvents(scope)
 *          .consumeEach { /* handle fling event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RecyclerView.flingEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<RecyclerViewFlingEvent> = corbindReceiveChannel(capacity) {
    onFlingListener = listener(scope, this@flingEvents, ::trySend)
    invokeOnClose { onFlingListener = null }
}

/**
 * Create a flow of [fling events][RecyclerViewFlingEvent] on [RecyclerView].
 *
 * *Warning:* The created flow uses [RecyclerView.setOnFlingListener]. Only one flow can be used at
 * a time.
 *
 * Example:
 *
 * ```
 * recyclerView.flingEvents()
 *      .onEach { /* handle fling event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RecyclerView.flingEvents(): Flow<RecyclerViewFlingEvent> = channelFlow {
    onFlingListener = listener(this, this@flingEvents, ::trySend)
    awaitClose { onFlingListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    recyclerView: RecyclerView,
    emitter: (RecyclerViewFlingEvent) -> Unit,
) = object : RecyclerView.OnFlingListener() {

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        if (scope.isActive) {
            emitter(RecyclerViewFlingEvent(recyclerView, velocityX, velocityY))
        }
        return false
    }
}
