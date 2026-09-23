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
import androidx.annotation.MainThread
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

data class RatingBarChangeEvent(
    val view: RatingBar,
    val rating: Float,
    val fromUser: Boolean,
)

/**
 * Perform an action on [rating change events][RatingBarChangeEvent] on [RatingBar].
 *
 * *Warning:* The created actor uses [RatingBar.setOnRatingBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun RatingBar.ratingChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RatingBarChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<RatingBarChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.corbindEventEmitter(scope)(initialValue(this))
    onRatingBarChangeListener = listener(scope, events.corbindEventEmitter(scope))
    events.invokeOnCloseOnMain { onRatingBarChangeListener = null }
}

/**
 * Perform an action on [rating change events][RatingBarChangeEvent] on [RatingBar], in a new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [RatingBar.setOnRatingBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun RatingBar.ratingChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RatingBarChangeEvent) -> Unit,
) = coroutineScope {
    ratingChangeEvents(this, capacity, action)
}

/**
 * Create a channel of the [rating change events][RatingBarChangeEvent] on [RatingBar].
 *
 * *Warning:* The created channel uses [RatingBar.setOnRatingBarChangeListener]. Only one channel
 * can be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun RatingBar.ratingChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<RatingBarChangeEvent> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(scope, corbindEventEmitter())
    awaitClose { onRatingBarChangeListener = null }
}

/**
 * Create a flow of the [rating change events][RatingBarChangeEvent] on [RatingBar].
 *
 * *Warning:* The created flow uses [RatingBar.setOnRatingBarChangeListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * ratingBar.ratingChangeEvents()
 *      .onEach { /* handle rating change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * ratingBar.ratingChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle rating change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RatingBar.ratingChangeEvents(): InitialValueFlow<RatingBarChangeEvent> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    onRatingBarChangeListener = listener(this, emitter)
    emitter.sendInitialValue(initialValue(ratingBar = this@ratingChangeEvents))
    awaitClose { onRatingBarChangeListener = null }
}.asInitialValueFlow()

@CheckResult
private fun initialValue(ratingBar: RatingBar): RatingBarChangeEvent = RatingBarChangeEvent(ratingBar, ratingBar.rating, false)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (RatingBarChangeEvent) -> Boolean,
) = RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
    if (scope.isActive) {
        emitter(RatingBarChangeEvent(ratingBar, rating, fromUser))
    }
}
