package ru.ldralighieri.corbind.material

import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun AppBarLayout.offsetChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (offset in channel) action(offset)
    }

    val listener = listener(events::offer)
    addOnOffsetChangedListener(listener)
    events.invokeOnClose { removeOnOffsetChangedListener(listener) }
}

suspend fun AppBarLayout.offsetChanges(
        action: suspend (Int) -> Unit
) = coroutineScope {
    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (offset in channel) action(offset)
    }

    val listener = listener(events::offer)
    addOnOffsetChangedListener(listener)
    events.invokeOnClose { removeOnOffsetChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun AppBarLayout.offsetChanges(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(::offer)
    addOnOffsetChangedListener(listener)
    invokeOnClose { removeOnOffsetChangedListener(listener) }
}

suspend fun AppBarLayout.offsetChanges(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(::offer)
        addOnOffsetChangedListener(listener)
        invokeOnClose { removeOnOffsetChangedListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (Int) -> Boolean
) = AppBarLayout.OnOffsetChangedListener { _, verticalOffset -> emitter(verticalOffset) }