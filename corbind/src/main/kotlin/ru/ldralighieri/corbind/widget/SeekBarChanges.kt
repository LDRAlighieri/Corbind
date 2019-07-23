@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.SeekBar
import androidx.annotation.CheckResult
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


private fun SeekBar.changes(
        scope: CoroutineScope,
        capacity: Int,
        shouldBeFromUser: Boolean?,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (progress in channel) action(progress)
    }

    events.offer(progress)
    setOnSeekBarChangeListener(listener(scope, shouldBeFromUser, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}

private suspend fun SeekBar.changes(
        capacity: Int,
        shouldBeFromUser: Boolean?,
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
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
        capacity: Int,
        shouldBeFromUser: Boolean?
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    safeOffer(progress)
    setOnSeekBarChangeListener(listener(scope, shouldBeFromUser, ::safeOffer))
    invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun SeekBar.changes(
    shouldBeFromUser: Boolean?
): Flow<Int> = channelFlow {
    offer(progress)
    setOnSeekBarChangeListener(listener(this, shouldBeFromUser, ::offer))
    awaitClose { setOnSeekBarChangeListener(null) }
}


// ===============================================================================================


fun SeekBar.changes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(scope, capacity, null, action)

suspend fun SeekBar.changes(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(capacity, null, action)

@CheckResult
fun SeekBar.changes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
) = changes(scope, capacity, null)

@CheckResult
fun SeekBar.changes() = changes(null)


// -----------------------------------------------------------------------------------------------


fun SeekBar.userChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(scope, capacity, true, action)

suspend fun SeekBar.userChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(capacity, true, action)

@CheckResult
fun SeekBar.userChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
) = changes(scope, capacity, true)

@CheckResult
fun SeekBar.userChanges() = changes(true)


// -----------------------------------------------------------------------------------------------


fun SeekBar.systemChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(scope, capacity, false, action)

suspend fun SeekBar.systemChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(capacity, false, action)

@CheckResult
fun SeekBar.systemChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
) = changes(scope, capacity, false)

@CheckResult
fun SeekBar.systemChanges() = changes(false)


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
