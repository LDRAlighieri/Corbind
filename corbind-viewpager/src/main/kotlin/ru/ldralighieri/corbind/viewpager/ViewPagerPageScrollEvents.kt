@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.viewpager

import androidx.annotation.CheckResult
import androidx.viewpager.widget.ViewPager
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

data class ViewPagerPageScrollEvent(
        val viewPager: ViewPager,
        val position: Int,
        val positionOffset: Float,
        val positionOffsetPixels: Int
)

// -----------------------------------------------------------------------------------------------


fun ViewPager.pageScrollEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewPagerPageScrollEvent) -> Unit
) {

    val events = scope.actor<ViewPagerPageScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope = scope, viewPager = this, emitter = events::offer)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}

suspend fun ViewPager.pageScrollEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewPagerPageScrollEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewPagerPageScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope = this, viewPager = this@pageScrollEvents,
            emitter = events::offer)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun ViewPager.pageScrollEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewPagerPageScrollEvent> = corbindReceiveChannel(capacity) {

    val listener = listener(scope, this@pageScrollEvents, ::safeOffer)
    addOnPageChangeListener(listener)
    invokeOnClose { removeOnPageChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        viewPager: ViewPager,
        emitter: (ViewPagerPageScrollEvent) -> Boolean
) = object : ViewPager.OnPageChangeListener {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (scope.isActive) {
            val event = ViewPagerPageScrollEvent(viewPager, position, positionOffset,
                    positionOffsetPixels)
            emitter(event)
        }
    }

    override fun onPageSelected(position: Int) {  }
    override fun onPageScrollStateChanged(state: Int) {  }
}