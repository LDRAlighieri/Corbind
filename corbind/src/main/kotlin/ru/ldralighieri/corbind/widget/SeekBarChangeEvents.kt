@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.SeekBar
import androidx.annotation.CheckResult
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

sealed class SeekBarChangeEvent {
    abstract val view: SeekBar
}

data class SeekBarProgressChangeEvent(
        override val view: SeekBar,
        val progress: Int,
        val fromUser: Boolean
) : SeekBarChangeEvent()

data class SeekBarStartChangeEvent(
        override val view: SeekBar
) : SeekBarChangeEvent()

data class SeekBarStopChangeEvent(
        override val view: SeekBar
) : SeekBarChangeEvent()

// -----------------------------------------------------------------------------------------------


private fun SeekBar.changeEvents(
        scope: CoroutineScope,
        action: suspend (SeekBarChangeEvent) -> Unit
) {

    val events = scope.actor<SeekBarChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    setOnSeekBarChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}

private suspend fun SeekBar.changeEvents(
        action: suspend (SeekBarChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<SeekBarChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun SeekBar.changeEvents(
        scope: CoroutineScope
): ReceiveChannel<SeekBarChangeEvent> = corbindReceiveChannel {

    offer(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(seekBar: SeekBar): SeekBarChangeEvent =
        SeekBarProgressChangeEvent(seekBar, seekBar.progress, false)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (SeekBarChangeEvent) -> Boolean
) = object : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        onEvent(SeekBarProgressChangeEvent(seekBar, progress, fromUser))
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        onEvent(SeekBarStartChangeEvent(seekBar))
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        onEvent(SeekBarStopChangeEvent(seekBar))
    }

    private fun onEvent(event: SeekBarChangeEvent)  {
        if (scope.isActive) { emitter(event) }
    }
}