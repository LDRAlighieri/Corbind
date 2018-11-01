@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.appbar.AppBarLayout
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


fun AppBarLayout.offsetChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (offset in channel) action(offset)
    }

    val listener = listener(scope, events::offer)
    addOnOffsetChangedListener(listener)
    events.invokeOnClose { removeOnOffsetChangedListener(listener) }
}

suspend fun AppBarLayout.offsetChanges(
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (offset in channel) action(offset)
    }

    val listener = listener(this, events::offer)
    addOnOffsetChangedListener(listener)
    events.invokeOnClose { removeOnOffsetChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun AppBarLayout.offsetChanges(
        scope: CoroutineScope
): ReceiveChannel<Int> = corbindReceiveChannel {

    val listener = listener(scope, ::safeOffer)
    addOnOffsetChangedListener(listener)
    invokeOnClose { removeOnOffsetChangedListener(listener) }
}

@CheckResult
suspend fun AppBarLayout.offsetChanges(): ReceiveChannel<Int> = coroutineScope {

    corbindReceiveChannel<Int> {
        val listener = listener(this@coroutineScope, ::safeOffer)
        addOnOffsetChangedListener(listener)
        invokeOnClose { removeOnOffsetChangedListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->

    if (scope.isActive) { emitter(verticalOffset) }
}