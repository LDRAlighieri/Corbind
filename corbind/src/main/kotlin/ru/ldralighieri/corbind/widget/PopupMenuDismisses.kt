package ru.ldralighieri.corbind.widget

import android.widget.PopupMenu
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




/**
 * Perform an action on [PopupMenu] dismiss events.
 *
 * *Warning:* The created actor uses [PopupMenu.setOnDismissListener] to emmit dismiss change.
 * Only one actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun PopupMenu.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}

/**
 * Perform an action on [PopupMenu] dismiss events inside new CoroutineScope.
 *
 * *Warning:* The created actor uses [PopupMenu.setOnDismissListener] to emmit dismiss change.
 * Only one actor can be used for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun PopupMenu.dismisses(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}





/**
 * Create a channel which emits on [PopupMenu] dismiss events
 *
 * *Warning:* The created channel uses [PopupMenu.setOnDismissListener] to emmit dismiss change.
 * Only one channel can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun PopupMenu.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnDismissListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnDismissListener(null) }
}





/**
 * Create a flow which emits on [PopupMenu] dismiss events
 *
 * *Warning:* The created flow uses [PopupMenu.setOnDismissListener] to emmit dismiss change.
 * Only one flow can be used for a view at a time.
 */
@CheckResult
fun PopupMenu.dismisses(): Flow<Unit> = channelFlow {
    setOnDismissListener(listener(this, ::offer))
    awaitClose { setOnDismissListener(null) }
}





@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = PopupMenu.OnDismissListener {
    if (scope.isActive) { emitter(Unit) }
}
