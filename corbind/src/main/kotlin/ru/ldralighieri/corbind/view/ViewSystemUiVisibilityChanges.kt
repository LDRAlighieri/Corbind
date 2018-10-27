package ru.ldralighieri.corbind.view

import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

fun View.systemUiVisibilityChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (visibility in channel) action(visibility)
    }

    setOnSystemUiVisibilityChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}

suspend fun View.systemUiVisibilityChanges(
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (visibility in channel) action(visibility)
    }

    setOnSystemUiVisibilityChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.systemUiVisibilityChanges(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnSystemUiVisibilityChangeListener(listener(this, ::offer))
    invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
}

@CheckResult
suspend fun View.systemUiVisibilityChanges(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        setOnSystemUiVisibilityChangeListener(listener(this, ::offer))
        invokeOnClose { setOnSystemUiVisibilityChangeListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = View.OnSystemUiVisibilityChangeListener {

    if (scope.isActive) { emitter(it) }
}