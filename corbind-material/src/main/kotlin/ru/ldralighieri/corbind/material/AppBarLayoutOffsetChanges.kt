@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.appbar.AppBarLayout
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
 * Perform an action on the offset change in [AppBarLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun AppBarLayout.offsetChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (offset in channel) action(offset)
    }

    val listener = listener(scope, events::offer)
    addOnOffsetChangedListener(listener)
    events.invokeOnClose { removeOnOffsetChangedListener(listener) }
}

/**
 * Perform an action on the offset change in [AppBarLayout] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun AppBarLayout.offsetChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (offset in channel) action(offset)
    }

    val listener = listener(this, events::offer)
    addOnOffsetChangedListener(listener)
    events.invokeOnClose { removeOnOffsetChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits the offset change in [AppBarLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun AppBarLayout.offsetChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::safeOffer)
    addOnOffsetChangedListener(listener)
    invokeOnClose { removeOnOffsetChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits the offset change in [AppBarLayout].
 */
@CheckResult
fun AppBarLayout.offsetChanges(): Flow<Int> = channelFlow {
    val listener = listener(this, ::offer)
    addOnOffsetChangedListener(listener)
    awaitClose { removeOnOffsetChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
    if (scope.isActive) { emitter(verticalOffset) }
}
