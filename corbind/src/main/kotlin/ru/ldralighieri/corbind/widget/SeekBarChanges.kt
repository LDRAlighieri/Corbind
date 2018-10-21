package ru.ldralighieri.corbind.widget

import android.widget.SeekBar
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


private fun SeekBar.changes(
        scope: CoroutineScope,
        shouldBeFromUser: Boolean?,
        action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (progress in channel) action(progress)
    }

    events.offer(progress)
    setOnSeekBarChangeListener(listener(shouldBeFromUser = shouldBeFromUser, emitter = events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}

private suspend fun SeekBar.changes(
        shouldBeFromUser: Boolean?,
        action: suspend (Int) -> Unit
) = coroutineScope {
    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (progress in channel) action(progress)
    }

    events.offer(progress)
    setOnSeekBarChangeListener(listener(shouldBeFromUser = shouldBeFromUser, emitter = events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


private fun SeekBar.changes(
        scope: CoroutineScope,
        shouldBeFromUser: Boolean?
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(progress)
    setOnSeekBarChangeListener(listener(shouldBeFromUser = shouldBeFromUser, emitter = ::offer))
    invokeOnClose { setOnSeekBarChangeListener(null) }
}

private suspend fun SeekBar.changes(
        shouldBeFromUser: Boolean?
): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        offer(progress)
        setOnSeekBarChangeListener(listener(shouldBeFromUser = shouldBeFromUser, emitter = ::offer))
        invokeOnClose { setOnSeekBarChangeListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


fun SeekBar.changes(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) = changes(scope, null, action)

suspend fun SeekBar.changes(action: suspend (Int) -> Unit) = changes(null, action)

fun SeekBar.changes(
        scope: CoroutineScope
) = changes(scope, null)

suspend fun SeekBar.changes() = changes(null)


// -----------------------------------------------------------------------------------------------


fun SeekBar.userChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) = changes(scope, true, action)

suspend fun SeekBar.userChanges(action: suspend (Int) -> Unit) = changes(true, action)

fun SeekBar.userChanges(
        scope: CoroutineScope
) = changes(scope, true)

suspend fun SeekBar.userChanges() = changes(true)


// -----------------------------------------------------------------------------------------------


fun SeekBar.systemChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) = changes(scope, false, action)

suspend fun SeekBar.systemChanges(action: suspend (Int) -> Unit) = changes(false, action)

fun SeekBar.systemChanges(
        scope: CoroutineScope
) = changes(scope, false)

suspend fun SeekBar.systemChanges() = changes(false)


// -----------------------------------------------------------------------------------------------


private fun listener(
        shouldBeFromUser: Boolean?,
        emitter: (Int) -> Boolean
) = object : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (shouldBeFromUser == null || shouldBeFromUser == fromUser) { emitter(progress) }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {  }
    override fun onStopTrackingTouch(seekBar: SeekBar) {  }
}