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

import android.widget.RatingBar
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
import ru.ldralighieri.corbind.internal.offerCatching

data class RatingBarChangeEvent(
    val view: RatingBar,
    val rating: Float,
    val fromUser: Boolean
)

/**
 * Perform an action on [rating change events][RatingBarChangeEvent] on [RatingBar].
 *
 * *Warning:* The created actor uses [RatingBar.setOnRatingBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RatingBar.ratingChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RatingBarChangeEvent) -> Unit
) {
    val events = scope.actor<RatingBarChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    onRatingBarChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}

/**
 * Perform an action on [rating change events][RatingBarChangeEvent] on [RatingBar], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [RatingBar.setOnRatingBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RatingBar.ratingChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RatingBarChangeEvent) -> Unit
) = coroutineScope {
    ratingChangeEvents(this, capacity, action)
}

/**
 * Create a channel of the [rating change events][RatingBarChangeEvent] on [RatingBar].
 *
 * *Warning:* The created channel uses [RatingBar.setOnRatingBarChangeListener]. Only one channel
 * can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      ratingBar.ratingChangeEvents(scope)
 *          .consumeEach { /* handle rating change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RatingBar.ratingChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<RatingBarChangeEvent> = corbindReceiveChannel(capacity) {
    offerCatching(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(scope, ::offerCatching)
    invokeOnClose { onRatingBarChangeListener = null }
}

/**
 * Create a flow of the [rating change events][RatingBarChangeEvent] on [RatingBar].
 *
 * *Warning:* The created flow uses [RatingBar.setOnRatingBarChangeListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * ratingBar.ratingChangeEvents()
 *      .onEach { /* handle rating change event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * ratingBar.ratingChangeEvents()
 *      .drop(1)
 *      .onEach { /* handle rating change event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RatingBar.ratingChangeEvents(): Flow<RatingBarChangeEvent> = channelFlow {
    offer(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(this, ::offerCatching)
    awaitClose { onRatingBarChangeListener = null }
}

@CheckResult
private fun initialValue(ratingBar: RatingBar): RatingBarChangeEvent =
    RatingBarChangeEvent(ratingBar, ratingBar.rating, false)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (RatingBarChangeEvent) -> Boolean
) = RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
    if (scope.isActive) { emitter(RatingBarChangeEvent(ratingBar, rating, fromUser)) }
}
