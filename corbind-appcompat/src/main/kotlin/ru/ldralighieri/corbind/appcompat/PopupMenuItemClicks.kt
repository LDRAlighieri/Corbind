@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.appcompat

import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.appcompat.widget.PopupMenu
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
 * Perform an action on clicked item in [PopupMenu].
 *
 * *Warning:* The created actor uses [PopupMenu.setOnMenuItemClickListener] to emmit dismiss
 * change. Only one actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun PopupMenu.itemClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Perform an action on clicked item in [PopupMenu] inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [PopupMenu.setOnMenuItemClickListener] to emmit dismiss
 * change. Only one actor can be used for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun PopupMenu.itemClicks(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits the clicked item in [PopupMenu].
 *
 * *Warning:* The created channel uses [PopupMenu.setOnMenuItemClickListener] to emmit dismiss
 * change. Only one channel can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun PopupMenu.itemClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnMenuItemClickListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits the clicked item in [PopupMenu].
 *
 * *Warning:* The created flow uses [PopupMenu.setOnMenuItemClickListener] to emmit dismiss
 * change. Only one flow can be used for a view at a time.
 */
@CheckResult
fun PopupMenu.itemClicks(): Flow<MenuItem> = channelFlow {
    setOnMenuItemClickListener(listener(this, ::offer))
    awaitClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (MenuItem) -> Boolean
) = PopupMenu.OnMenuItemClickListener {
    if (scope.isActive) { emitter(it) }
    return@OnMenuItemClickListener true
}
