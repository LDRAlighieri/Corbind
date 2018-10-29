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
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun View.dismisses(
        scope: CoroutineScope,
        action: suspend (View) -> Unit
) {

    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val behavior = getBehavior(this)
    behavior.setListener(listener(scope, events::offer))
    events.invokeOnClose { behavior.setListener(null) }
}

suspend fun View.dismisses(
        action: suspend (View) -> Unit
) = coroutineScope {

    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    val behavior = getBehavior(this@dismisses)
    behavior.setListener(listener(this, events::offer))
    events.invokeOnClose { behavior.setListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.dismisses(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val behavior = getBehavior(this@dismisses)
    behavior.setListener(listener(this, ::offer))
    invokeOnClose { behavior.setListener(null) }
}

@CheckResult
suspend fun View.dismisses(): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        val behavior = getBehavior(this@dismisses)
        behavior.setListener(listener(this, ::offer))
        invokeOnClose { behavior.setListener(null) }
    }
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

