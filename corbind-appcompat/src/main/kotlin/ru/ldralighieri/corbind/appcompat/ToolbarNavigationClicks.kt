package ru.ldralighieri.corbind.appcompat

import android.view.View
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
 * Perform an action on [Toolbar] navigation click events.
 *
 * *Warning:* The created actor uses [Toolbar.setNavigationOnClickListener] to emmit clicks. Only
 * one actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Toolbar.navigationClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(scope, events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}

/**
 * Perform an action on [Toolbar] navigation click events inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [Toolbar.setNavigationOnClickListener] to emmit clicks. Only
 * one actor can be used for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Toolbar.navigationClicks(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(this, events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}





/**
 * Create a channel which emits on [Toolbar] navigation click events.
 *
 * *Warning:* The created channel uses [Toolbar.setNavigationOnClickListener] to emmit clicks.
 * Only one channel can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Toolbar.navigationClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setNavigationOnClickListener(listener(scope, ::safeOffer))
    invokeOnClose { setNavigationOnClickListener(null) }
}





/**
 * Create a flow which emits on [Toolbar] navigation click events.
 *
 * *Warning:* The created flow uses [Toolbar.setNavigationOnClickListener] to emmit clicks. Only
 * one flow can be used for a view at a time.
 */
@CheckResult
fun Toolbar.navigationClicks(): Flow<Unit> = channelFlow {
    setNavigationOnClickListener(listener(this, ::offer))
    awaitClose { setNavigationOnClickListener(null) }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = View.OnClickListener {
    if (scope.isActive) { emitter(Unit) }
}
