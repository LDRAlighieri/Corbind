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

// TODO Вернуться и отрефакторить
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


/**
 * Perform an action on progress value changes on [SeekBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SeekBar.changes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(scope, capacity, null, action)

/**
 * Perform an action on progress value changes on [SeekBar] inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SeekBar.changes(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(capacity, null, action)

/**
 * Create a channel of progress value changes on [SeekBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SeekBar.changes(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
) = changes(scope, capacity, null)

/**
 * Create a flow of progress value changes on [SeekBar].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun SeekBar.changes() = changes(null)


// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the user.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SeekBar.userChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(scope, capacity, true, action)

/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the user inside
 * new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SeekBar.userChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(capacity, true, action)

/**
 * Create a channel of progress value changes on [SeekBar] that were made only from the user.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SeekBar.userChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
) = changes(scope, capacity, true)

/**
 * Create a flow of progress value changes on [SeekBar] that were made only from the user.
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun SeekBar.userChanges() = changes(true)


// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the system.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SeekBar.systemChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(scope, capacity, false, action)

/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the system inside
 * new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SeekBar.systemChanges(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (Int) -> Unit
) = changes(capacity, false, action)

/**
 * Create a channel of progress value changes on [SeekBar] that were made only from the system.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SeekBar.systemChanges(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
) = changes(scope, capacity, false)

/**
 * Create a flow of progress value changes on [SeekBar] that were made only from the system.
 *
 * *Note:* A value will be emitted immediately on collect.
 */
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
