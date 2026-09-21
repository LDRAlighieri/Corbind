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

package ru.ldralighieri.corbind.drawerlayout

import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.drawerlayout.widget.DrawerLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

/**
 * Perform an action on the open state of the [DrawerLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param gravity Gravity of the drawer to check
 * @param action An action to perform
 */
@MainThread
fun DrawerLayout.drawerOpens(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    gravity: Int,
    action: suspend (Boolean) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (open in channel) action(open)
    }

    events.corbindEventEmitter(scope)(isDrawerOpen(gravity))
    val listener = listener(scope, gravity, events.corbindEventEmitter(scope))
    addDrawerListener(listener)
    events.invokeOnCloseOnMain { removeDrawerListener(listener) }
}

/**
 * Perform an action on the open state of the [DrawerLayout], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param gravity Gravity of the drawer to check
 * @param action An action to perform
 */
@MainThread
suspend fun DrawerLayout.drawerOpens(
    capacity: Int = Channel.RENDEZVOUS,
    gravity: Int,
    action: suspend (Boolean) -> Unit,
) = coroutineScope {
    drawerOpens(this, capacity, gravity, action)
}

/**
 * Create a channel of the open state of the [DrawerLayout].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      drawerLayout.drawerOpens(scope)
 *          .consumeEach { /* handle open state */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param gravity Gravity of the drawer to check
 */
@CheckResult
fun DrawerLayout.drawerOpens(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    gravity: Int,
): ReceiveChannel<Boolean> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(isDrawerOpen(gravity))
    val listener = listener(scope, gravity, corbindEventEmitter())
    addDrawerListener(listener)
    awaitClose { removeDrawerListener(listener) }
}

/**
 * Create a flow of the open state of the [DrawerLayout].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * drawerLayout.drawerOpens()
 *      .onEach { /* handle open state */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * adapter.dataChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle open state */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param gravity Gravity of the drawer to check
 */
@CheckResult
fun DrawerLayout.drawerOpens(gravity: Int): InitialValueFlow<Boolean> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    val listener = listener(this, gravity, emitter)
    addDrawerListener(listener)
    emitter.sendInitialValue(this@drawerOpens.isDrawerOpen(gravity))
    awaitClose { removeDrawerListener(listener) }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    gravity: Int,
    emitter: (Boolean) -> Boolean,
) = object : DrawerLayout.DrawerListener {

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
    override fun onDrawerOpened(drawerView: View) = onEvent(drawerView, true)
    override fun onDrawerClosed(drawerView: View) = onEvent(drawerView, false)
    override fun onDrawerStateChanged(newState: Int) = Unit

    private fun onEvent(drawerView: View, opened: Boolean) {
        if (scope.isActive) {
            val drawerGravity = (drawerView.layoutParams as DrawerLayout.LayoutParams).gravity
            if (drawerGravity == gravity) emitter(opened)
        }
    }
}
