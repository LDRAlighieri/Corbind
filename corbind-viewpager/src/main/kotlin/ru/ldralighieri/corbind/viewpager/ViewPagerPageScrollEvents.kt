package ru.ldralighieri.corbind.viewpager

import androidx.annotation.CheckResult
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

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
        action: suspend (ViewPagerPageScrollEvent) -> Unit
) {

    val events = scope.actor<ViewPagerPageScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(scope = scope, viewPager = this, emitter = events::offer)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}

suspend fun ViewPager.pageScrollEvents(
        action: suspend (ViewPagerPageScrollEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewPagerPageScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
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
        scope: CoroutineScope
): ReceiveChannel<ViewPagerPageScrollEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(scope = this, viewPager = this@pageScrollEvents, emitter = ::offer)
    addOnPageChangeListener(listener)
    invokeOnClose { removeOnPageChangeListener(listener) }
}

@CheckResult
suspend fun ViewPager.pageScrollEvents(): ReceiveChannel<ViewPagerPageScrollEvent> = coroutineScope {

    produce<ViewPagerPageScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(scope = this, viewPager = this@pageScrollEvents, emitter = ::offer)
        addOnPageChangeListener(listener)
        invokeOnClose { removeOnPageChangeListener(listener) }
    }
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