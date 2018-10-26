package ru.ldralighieri.corbind.slidingpanelayout

import android.view.View
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun SlidingPaneLayout.panelSlides(
        scope: CoroutineScope,
        action: suspend (Float) -> Unit
) {
    val events = scope.actor<Float>(Dispatchers.Main, Channel.CONFLATED) {
        for (slide in channel) action(slide)
    }

    setPanelSlideListener(listener(events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}

suspend fun SlidingPaneLayout.panelSlides(
        action: suspend (Float) -> Unit
) = coroutineScope {
    val events = actor<Float>(Dispatchers.Main, Channel.CONFLATED) {
        for (slide in channel) action(slide)
    }

    setPanelSlideListener(listener(events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun SlidingPaneLayout.panelSlides(
        scope: CoroutineScope
): ReceiveChannel<Float> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setPanelSlideListener(listener(::offer))
    invokeOnClose { setPanelSlideListener(null) }
}

suspend fun SlidingPaneLayout.panelSlides(): ReceiveChannel<Float> = coroutineScope {

    produce<Float>(Dispatchers.Main, Channel.CONFLATED) {
        setPanelSlideListener(listener(::offer))
        invokeOnClose { setPanelSlideListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (Float) -> Boolean
) = object : SlidingPaneLayout.PanelSlideListener {

    override fun onPanelSlide(panel: View, slideOffset: Float) { emitter(slideOffset) }
    override fun onPanelOpened(panel: View) {  }
    override fun onPanelClosed(panel: View) {  }
}