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


private fun SeekBar.changes(
        scope: CoroutineScope,
        shouldBeFromUser: Boolean?,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (progress in channel) action(progress)
    }

    events.offer(progress)
    setOnSeekBarChangeListener(listener(scope, shouldBeFromUser, events::offer))
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
    setOnSeekBarChangeListener(listener(this, shouldBeFromUser, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun SeekBar.changes(
        scope: CoroutineScope,
        shouldBeFromUser: Boolean?
): ReceiveChannel<Int> = corbindReceiveChannel {

    safeOffer(progress)
    setOnSeekBarChangeListener(listener(scope, shouldBeFromUser, ::safeOffer))
    invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun SeekBar.changes(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) = changes(scope, null, action)

suspend fun SeekBar.changes(action: suspend (Int) -> Unit) = changes(null, action)

@CheckResult
fun SeekBar.changes(
        scope: CoroutineScope
) = changes(scope, null)


// -----------------------------------------------------------------------------------------------


fun SeekBar.userChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) = changes(scope, true, action)

suspend fun SeekBar.userChanges(action: suspend (Int) -> Unit) = changes(true, action)

@CheckResult
fun SeekBar.userChanges(
        scope: CoroutineScope
) = changes(scope, true)


// -----------------------------------------------------------------------------------------------


fun SeekBar.systemChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) = changes(scope, false, action)

suspend fun SeekBar.systemChanges(action: suspend (Int) -> Unit) = changes(false, action)

@CheckResult
fun SeekBar.systemChanges(
        scope: CoroutineScope
) = changes(scope, false)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        shouldBeFromUser: Boolean?,
        emitter: (Int) -> Boolean
) = object : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (scope.isActive && (shouldBeFromUser == null || shouldBeFromUser == fromUser)) {
            emitter(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {  }
    override fun onStopTrackingTouch(seekBar: SeekBar) {  }
}