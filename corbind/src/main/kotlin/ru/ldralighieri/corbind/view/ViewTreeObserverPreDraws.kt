@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewTreeObserver
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


fun View.preDraws(
        scope: CoroutineScope,
        proceedDrawingPass: () -> Boolean,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(scope, proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}

suspend fun View.preDraws(
        proceedDrawingPass: () -> Boolean,
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(this, proceedDrawingPass, events::offer)
    viewTreeObserver.addOnPreDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.preDraws(
        scope: CoroutineScope,
        proceedDrawingPass: () -> Boolean
): ReceiveChannel<Unit> = corbindReceiveChannel {

    val listener = listener(scope, proceedDrawingPass, ::safeOffer)
    viewTreeObserver.addOnPreDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnPreDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        proceedDrawingPass: () -> Boolean,
        emitter: (Unit) -> Boolean
) = ViewTreeObserver.OnPreDrawListener {

    if (scope.isActive) {
        emitter(Unit)
        return@OnPreDrawListener proceedDrawingPass()
    }
    return@OnPreDrawListener true
}