@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
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


fun View.focusChanges(
        scope: CoroutineScope,
        action: suspend (Boolean) -> Unit
) {

    val events = scope.actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}

suspend fun View.focusChanges(
        action: suspend (Boolean) -> Unit
) = coroutineScope {

    val events = actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(this, events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.focusChanges(
        scope: CoroutineScope
): ReceiveChannel<Boolean> = corbindReceiveChannel {

    safeOffer(hasFocus())
    onFocusChangeListener = listener(scope, ::safeOffer)
    invokeOnClose { onFocusChangeListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Boolean) -> Boolean
) = View.OnFocusChangeListener { _, hasFocus ->

    if (scope.isActive) { emitter(hasFocus) }
}