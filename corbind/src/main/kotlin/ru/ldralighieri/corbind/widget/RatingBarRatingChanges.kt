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
 * Perform an action on rating changes on [RatingBar].
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
fun RatingBar.ratingChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Float>(Dispatchers.Main.immediate, capacity) {
        for (rating in channel) action(rating)
    }

    events.corbindEventEmitter(scope)(rating)
    onRatingBarChangeListener = listener(scope, events.corbindEventEmitter(scope))
    events.invokeOnCloseOnMain { onRatingBarChangeListener = null }
}

/**
 * Perform an action on rating changes on [RatingBar], in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [RatingBar.setOnRatingBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun RatingBar.ratingChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) = coroutineScope {
    ratingChanges(this, capacity, action)
}

/**
 * Create a channel of rating changes on [RatingBar].
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
 *      ratingBar.ratingChanges(scope)
 *          .consumeEach { /* handle rating change */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun RatingBar.ratingChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Float> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(rating)
    onRatingBarChangeListener = listener(scope, corbindEventEmitter())
    awaitClose { onRatingBarChangeListener = null }
}

/**
 * Create a flow of the rating changes on [RatingBar].
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
 * ratingBar.ratingChanges()
 *      .onEach { /* handle rating change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * ratingBar.ratingChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle rating change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RatingBar.ratingChanges(): InitialValueFlow<Float> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    onRatingBarChangeListener = listener(this, emitter)
    emitter.sendInitialValue(rating)
    awaitClose { onRatingBarChangeListener = null }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Float) -> Boolean,
) = RatingBar.OnRatingBarChangeListener { _, rating, _ ->
    if (scope.isActive) emitter(rating)
}
