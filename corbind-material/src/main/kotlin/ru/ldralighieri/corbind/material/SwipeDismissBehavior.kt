@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import android.view.View
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.SwipeDismissBehavior
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


fun View.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (View) -> Unit
) {

    val events = scope.actor<View>(Dispatchers.Main, capacity) {
        for (view in channel) action(view)
    }

    val behavior = getBehavior(this)
    behavior.setListener(listener(scope, events::offer))
    events.invokeOnClose { behavior.setListener(null) }
}

suspend fun View.dismisses(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (View) -> Unit
) = coroutineScope {

    val events = actor<View>(Dispatchers.Main, capacity) {
        for (view in channel) action(view)
    }

    val behavior = getBehavior(this@dismisses)
    behavior.setListener(listener(this, events::offer))
    events.invokeOnClose { behavior.setListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.dismisses(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<View> = corbindReceiveChannel(capacity) {

    val behavior = getBehavior(this@dismisses)
    behavior.setListener(listener(scope, ::safeOffer))
    invokeOnClose { behavior.setListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun getBehavior(view: View): SwipeDismissBehavior<*> {
    val params = view.layoutParams as? CoordinatorLayout.LayoutParams
            ?: throw IllegalArgumentException("The view is not in a Coordinator Layout.")
    return params.behavior as SwipeDismissBehavior<*>?
            ?: throw IllegalStateException("There's no behavior set on this view.")
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (View) -> Boolean
) = object : SwipeDismissBehavior.OnDismissListener {

    override fun onDismiss(view: View) {
        if (scope.isActive) { emitter(view) }
    }

    override fun onDragStateChanged(state: Int) {  }
}

