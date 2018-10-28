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
import kotlinx.coroutines.experimental.isActive

// -----------------------------------------------------------------------------------------------


fun RatingBar.ratingChanges(
        scope: CoroutineScope,
        action: suspend (Float) -> Unit
) {

    val events = scope.actor<Float>(Dispatchers.Main, Channel.CONFLATED) {
        for (rating in channel) action(rating)
    }

    events.offer(rating)
    onRatingBarChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}

suspend fun RatingBar.ratingChanges(
        action: suspend (Float) -> Unit
) = coroutineScope {

    val events = actor<Float>(Dispatchers.Main, Channel.CONFLATED) {
        for (rating in channel) action(rating)
    }

    events.offer(rating)
    onRatingBarChangeListener = listener(this, events::offer)
    events.invokeOnClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RatingBar.ratingChanges(
        scope: CoroutineScope
): ReceiveChannel<Float> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(rating)
    onRatingBarChangeListener = listener(this, ::offer)
    invokeOnClose { onRatingBarChangeListener = null }
}

@CheckResult
suspend fun RatingBar.ratingChanges(): ReceiveChannel<Float> = coroutineScope {

    produce<Float>(Dispatchers.Main, Channel.CONFLATED) {
        offer(rating)
        onRatingBarChangeListener = listener(this, ::offer)
        invokeOnClose { onRatingBarChangeListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Float) -> Boolean
) = RatingBar.OnRatingBarChangeListener { _, rating, _ ->

    if (scope.isActive) { emitter(rating) }
}