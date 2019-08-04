@file:Suppress("EXPERIMENTAL_API_USAGE")

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
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

/**
 * Change event on a rating bar
 */
data class RatingBarChangeEvent(
        val view: RatingBar,
        val rating: Float,
        val fromUser: Boolean
)

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on rating change events on `view`.
 */
fun RatingBar.ratingChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (RatingBarChangeEvent) -> Unit
) {

    val events = scope.actor<RatingBarChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    onRatingBarChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}

/**
 * Perform an action on rating change events on `view` inside new CoroutineScope.
 */
suspend fun RatingBar.ratingChangeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (RatingBarChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<RatingBarChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(this, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of the rating change events on `view`.
 */
@CheckResult
fun RatingBar.ratingChangeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        scope: CoroutineScope
): ReceiveChannel<RatingBarChangeEvent> = corbindReceiveChannel(capacity) {
    safeOffer(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(scope, ::offer)
    invokeOnClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of the rating change events on `view`.
 */
@CheckResult
fun RatingBar.ratingChangeEvents(): Flow<RatingBarChangeEvent> = channelFlow {
    offer(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(this, ::offer)
    awaitClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


/**
 * Initial value
 */
@CheckResult
private fun initialValue(ratingBar: RatingBar): RatingBarChangeEvent =
        RatingBarChangeEvent(ratingBar, ratingBar.rating, false)


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `view` rating change
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (RatingBarChangeEvent) -> Boolean
) = RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
    if (scope.isActive) { emitter(RatingBarChangeEvent(ratingBar, rating, fromUser)) }
}
