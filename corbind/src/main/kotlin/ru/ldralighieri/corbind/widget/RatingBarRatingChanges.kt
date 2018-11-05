@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.RatingBar
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

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
): ReceiveChannel<Float> = corbindReceiveChannel {

    safeOffer(rating)
    onRatingBarChangeListener = listener(scope, ::safeOffer)
    invokeOnClose { onRatingBarChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Float) -> Boolean
) = RatingBar.OnRatingBarChangeListener { _, rating, _ ->

    if (scope.isActive) { emitter(rating) }
}