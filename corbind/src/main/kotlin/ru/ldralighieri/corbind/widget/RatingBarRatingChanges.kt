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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

/**
 * Perform an action on rating changes on [RatingBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RatingBar.ratingChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) {

    val events = scope.actor<Float>(Dispatchers.Main, capacity) {
        for (rating in channel) action(rating)
    }

    events.offer(rating)
    onRatingBarChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}

/**
 * Perform an action on rating changes on [RatingBar] inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RatingBar.ratingChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) = coroutineScope {

    val events = actor<Float>(Dispatchers.Main, capacity) {
        for (rating in channel) action(rating)
    }

    events.offer(rating)
    onRatingBarChangeListener = listener(this, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}

/**
 * Create a change of the rating changes on [RatingBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RatingBar.ratingChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Float> = corbindReceiveChannel(capacity) {
    offerElement(rating)
    onRatingBarChangeListener = listener(scope, ::offerElement)
    invokeOnClose { onRatingBarChangeListener = null }
}

/**
 * Create a flow of the rating changes on [RatingBar].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun RatingBar.ratingChanges(): Flow<Float> = channelFlow {
    offer(rating)
    onRatingBarChangeListener = listener(this, ::offer)
    awaitClose { onRatingBarChangeListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Float) -> Boolean
) = RatingBar.OnRatingBarChangeListener { _, rating, _ ->
    if (scope.isActive) { emitter(rating) }
}
