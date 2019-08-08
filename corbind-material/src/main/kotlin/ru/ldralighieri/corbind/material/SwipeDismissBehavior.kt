/*
 * Copyright 2019 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

/**
 * Perform an action on the dismiss events from [View] on [SwipeDismissBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
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

/**
 * Perform an action on the dismiss events from [View] on [SwipeDismissBehavior] inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
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

/**
 * Create a channel which emits the dismiss events from [View] on [SwipeDismissBehavior].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.dismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<View> = corbindReceiveChannel(capacity) {
    val behavior = getBehavior(this@dismisses)
    behavior.setListener(listener(scope, ::safeOffer))
    invokeOnClose { behavior.setListener(null) }
}

/**
 * Create a flow which emits the dismiss events from [View] on [SwipeDismissBehavior].
 */
@CheckResult
fun View.dismisses(): Flow<View> = channelFlow {
    val behavior = getBehavior(this@dismisses)
    behavior.setListener(listener(this, ::offer))
    awaitClose { behavior.setListener(null) }
}

@CheckResult
private fun getBehavior(view: View): SwipeDismissBehavior<*> {
    val params = view.layoutParams as? CoordinatorLayout.LayoutParams
            ?: throw IllegalArgumentException("The view is not in a Coordinator Layout.")
    return params.behavior as SwipeDismissBehavior<*>?
            ?: throw IllegalStateException("There's no behavior set on this view.")
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (View) -> Boolean
) = object : SwipeDismissBehavior.OnDismissListener {

    override fun onDismiss(view: View) {
        if (scope.isActive) { emitter(view) }
    }

    override fun onDragStateChanged(state: Int) { }
}
