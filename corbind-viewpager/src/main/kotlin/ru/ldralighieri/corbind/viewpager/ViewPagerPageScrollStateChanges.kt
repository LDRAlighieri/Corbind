@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.viewpager

import androidx.annotation.CheckResult
import androidx.viewpager.widget.ViewPager
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
 * Perform an action on scroll state change events on [ViewPager].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ViewPager.pageScrollStateChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (state in channel) action(state)
    }

    val listener = listener(scope, events::offer)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}

/**
 * Perform an action on scroll state change events on [ViewPager] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ViewPager.pageScrollStateChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (state in channel) action(state)
    }

    val listener = listener(this, events::offer)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of scroll state change events on [ViewPager].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ViewPager.pageScrollStateChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::safeOffer)
    addOnPageChangeListener(listener)
    invokeOnClose { removeOnPageChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of scroll state change events on [ViewPager].
 */
@CheckResult
fun ViewPager.pageScrollStateChanges(): Flow<Int> = channelFlow {
    val listener = listener(this, ::offer)
    addOnPageChangeListener(listener)
    awaitClose { removeOnPageChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = object : ViewPager.OnPageChangeListener {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {  }
    override fun onPageSelected(position: Int) {  }

    override fun onPageScrollStateChanged(state: Int) {
        if (scope.isActive) { emitter(state) }
    }

}
