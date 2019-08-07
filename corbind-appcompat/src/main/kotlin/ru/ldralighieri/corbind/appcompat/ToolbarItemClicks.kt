package ru.ldralighieri.corbind.appcompat

import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.appcompat.widget.Toolbar
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




/**
 * Perform an action on the clicked item in [Toolbar] menu.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Toolbar.itemClicks(
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
 * Perform an action on the clicked item in [Toolbar] menu inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Toolbar.itemClicks(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}





/**
 * Create a channel which emits the clicked item in [Toolbar] menu.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Toolbar.itemClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnMenuItemClickListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}





/**
 * Create a flow which emits the clicked item in [Toolbar] menu.
 */
@CheckResult
fun Toolbar.itemClicks(): Flow<MenuItem> = channelFlow {
    setOnMenuItemClickListener(listener(this, ::offer))
    awaitClose { setOnMenuItemClickListener(null) }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (MenuItem) -> Boolean
) = Toolbar.OnMenuItemClickListener {
    if (scope.isActive) { emitter(it) }
    return@OnMenuItemClickListener true
}
