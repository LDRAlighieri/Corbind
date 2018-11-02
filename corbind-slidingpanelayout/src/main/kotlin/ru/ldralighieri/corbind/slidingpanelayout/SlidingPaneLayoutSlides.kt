@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.slidingpanelayout

import android.view.View
import androidx.annotation.CheckResult
import androidx.slidingpanelayout.widget.SlidingPaneLayout
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


fun SlidingPaneLayout.panelSlides(
        scope: CoroutineScope,
        action: suspend (Float) -> Unit
) {

    val events = scope.actor<Float>(Dispatchers.Main, Channel.CONFLATED) {
        for (slide in channel) action(slide)
    }

    setPanelSlideListener(listener(scope, events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}

suspend fun SlidingPaneLayout.panelSlides(
        action: suspend (Float) -> Unit
) = coroutineScope {

    val events = actor<Float>(Dispatchers.Main, Channel.CONFLATED) {
        for (slide in channel) action(slide)
    }

    setPanelSlideListener(listener(this, events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SlidingPaneLayout.panelSlides(
        scope: CoroutineScope
): ReceiveChannel<Float> = corbindReceiveChannel {

    setPanelSlideListener(listener(scope, ::safeOffer))
    invokeOnClose { setPanelSlideListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Float) -> Boolean
) = object : SlidingPaneLayout.PanelSlideListener {

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        if (scope.isActive) { emitter(slideOffset) }
    }

    override fun onPanelOpened(panel: View) {  }
    override fun onPanelClosed(panel: View) {  }
}