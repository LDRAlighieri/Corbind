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

package ru.ldralighieri.corbind.widget

import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

/**
 * Perform an action on editor actions on [TextView].
 *
 * *Warning:* The created actor uses [TextView.setOnEditorActionListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
fun TextView.editorActions(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (Int) -> Boolean = AlwaysTrue,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (actionId in channel) action(actionId)
    }

    setOnEditorActionListener(listener(scope, handled, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnEditorActionListener(null) }
}

/**
 * Perform an action on editor actions on [TextView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [TextView.setOnEditorActionListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
suspend fun TextView.editorActions(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (Int) -> Boolean = AlwaysTrue,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    editorActions(this, capacity, handled, action)
}

/**
 * Create a channel of editor actions on [TextView].
 *
 * *Warning:* The created channel uses [TextView.setOnEditorActionListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.hovers(scope)
 *          .consumeEach { /* handle action */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun TextView.editorActions(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (Int) -> Boolean = AlwaysTrue,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    setOnEditorActionListener(listener(scope, handled, corbindEventEmitter()))
    awaitClose { setOnEditorActionListener(null) }
}

/**
 * Create a flow of editor actions on [TextView].
 *
 * *Warning:* The created flow uses [TextView.setOnEditorActionListener]. Only one flow can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * textView.editorActions()
 *      .onEach { /* handle action */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [TextView.OnEditorActionListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun TextView.editorActions(handled: (Int) -> Boolean = AlwaysTrue): Flow<Int> = corbindCallbackFlow {
    setOnEditorActionListener(listener(this, handled, corbindEventEmitter()))
    awaitClose { setOnEditorActionListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (Int) -> Boolean,
    emitter: (Int) -> Boolean,
) = TextView.OnEditorActionListener { _, actionId, _ ->
    if (scope.isActive && handled(actionId)) {
        return@OnEditorActionListener emitter(actionId)
    }
    return@OnEditorActionListener false
}
