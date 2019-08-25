package ru.ldralighieri.corbind.material

import android.view.View
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

/**
 * Perform an action on the slide offset events from [View] on [BottomSheetBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.slides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) {

    val events = scope.actor<Float>(Dispatchers.Main, capacity) {
        for (offset in channel) action(offset)
    }

    val behavior = getBehavior(this@slides)
    behavior.setBottomSheetCallback(callback(scope, events::offer))
    events.invokeOnClose { behavior.setBottomSheetCallback(null) }
}

/**
 * Perform an action on the slide offset events from [View] on [BottomSheetBehavior] inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.slides(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) = coroutineScope {

    val events = actor<Float>(Dispatchers.Main, capacity) {
        for (offset in channel) action(offset)
    }

    val behavior = getBehavior(this@slides)
    behavior.setBottomSheetCallback(callback(this, events::offer))
    events.invokeOnClose { behavior.setBottomSheetCallback(null) }
}

/**
 * Create a channel which emits the slide offset events from [View] on [BottomSheetBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.slides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Float> = corbindReceiveChannel(capacity) {
    val behavior = getBehavior(this@slides)
    behavior.setBottomSheetCallback(callback(scope, ::offerElement))
    invokeOnClose { behavior.setBottomSheetCallback(null) }
}

/**
 * Create a flow which emits the slide offset events from [View] on [BottomSheetBehavior].
 */
fun View.slides(): Flow<Float> = channelFlow {
    val behavior = getBehavior(this@slides)
    behavior.setBottomSheetCallback(callback(this, ::offer))
    awaitClose { behavior.setBottomSheetCallback(null) }
}

@CheckResult
private fun getBehavior(view: View): BottomSheetBehavior<*> {
    val params = view.layoutParams as? CoordinatorLayout.LayoutParams
        ?: throw IllegalArgumentException("The view is not in a Coordinator Layout.")
    return params.behavior as BottomSheetBehavior<*>?
        ?: throw IllegalStateException("There's no behavior set on this view.")
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Float) -> Boolean
) = object : BottomSheetBehavior.BottomSheetCallback() {

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        if (scope.isActive) { emitter(slideOffset) }
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) { }
}
