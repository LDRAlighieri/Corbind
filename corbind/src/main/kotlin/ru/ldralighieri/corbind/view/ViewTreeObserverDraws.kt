package ru.ldralighieri.corbind.view

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun View.draws(
        scope: CoroutineScope,
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = ViewTreeObserver.OnDrawListener { events.offer(this) }
    viewTreeObserver.addOnDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
suspend fun View.draws(
        action: suspend (View) -> Unit
) = coroutineScope {
    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val listener = ViewTreeObserver.OnDrawListener { events.offer(this@draws) }
    viewTreeObserver.addOnDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun View.draws(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = ViewTreeObserver.OnDrawListener { offer(this@draws) }
    viewTreeObserver.addOnDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
suspend fun View.draws(): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = ViewTreeObserver.OnDrawListener { offer(this@draws) }
        viewTreeObserver.addOnDrawListener(listener)
        invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
    }
}