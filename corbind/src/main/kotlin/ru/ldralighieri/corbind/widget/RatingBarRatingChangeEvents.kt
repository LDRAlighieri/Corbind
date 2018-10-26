package ru.ldralighieri.corbind.widget

import android.widget.RatingBar
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

data class RatingBarChangeEvent(
        val view: RatingBar,
        val rating: Float,
        val fromUser: Boolean
)

// -----------------------------------------------------------------------------------------------


fun RatingBar.ratingChangeEvents(
        scope: CoroutineScope,
        action: suspend (RatingBarChangeEvent) -> Unit
) {
    val events = scope.actor<RatingBarChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    onRatingBarChangeListener = listener(events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}

suspend fun RatingBar.ratingChangeEvents(
        action: suspend (RatingBarChangeEvent) -> Unit
) = coroutineScope {
    val events = actor<RatingBarChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RatingBar.ratingChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<RatingBarChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@ratingChangeEvents))
    onRatingBarChangeListener = listener(::offer)
    invokeOnClose { onRatingBarChangeListener = null }
}

@CheckResult
suspend fun RatingBar.ratingChangeEvents(): ReceiveChannel<RatingBarChangeEvent> = coroutineScope {

    produce<RatingBarChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        offer(initialValue(this@ratingChangeEvents))
        onRatingBarChangeListener = listener(::offer)
        invokeOnClose { onRatingBarChangeListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(ratingBar: RatingBar): RatingBarChangeEvent =
        RatingBarChangeEvent(ratingBar, ratingBar.rating, false)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        emitter: (RatingBarChangeEvent) -> Boolean
) = RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
    emitter(RatingBarChangeEvent(ratingBar, rating, fromUser))
}