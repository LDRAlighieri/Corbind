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

package ru.ldralighieri.corbind.navigation

import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.navigation.NavController
import androidx.navigation.NavDestination
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
import ru.ldralighieri.corbind.internal.offerCatching

data class NavControllerOnDestinationChangeEvent(
    val controller: NavController,
    val destination: NavDestination,
    val arguments: Bundle?
)

/**
 * Perform an action on [destination change events][NavControllerOnDestinationChangeEvent] on
 * [NavController].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun NavController.destinationChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (NavControllerOnDestinationChangeEvent) -> Unit
) {
    val events = scope
        .actor<NavControllerOnDestinationChangeEvent>(Dispatchers.Main.immediate, capacity) {
            for (event in channel) action(event)
        }

    val listener = listener(scope, events::offer)
    addOnDestinationChangedListener(listener)
    events.invokeOnClose { removeOnDestinationChangedListener(listener) }
}

/**
 * Perform an action on [destination change events][NavControllerOnDestinationChangeEvent] on
 * [NavController], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun NavController.destinationChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (NavControllerOnDestinationChangeEvent) -> Unit
) = coroutineScope {
    destinationChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [destination change events][NavControllerOnDestinationChangeEvent] on
 * [NavController].
 *
 * Example:
 *
 * ```
 * launch {
 *      navController.destinationChangeEvents(scope)
 *          .consumeEach { /* handle destination change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun NavController.destinationChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<NavControllerOnDestinationChangeEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::offerCatching)
    addOnDestinationChangedListener(listener)
    invokeOnClose { removeOnDestinationChangedListener(listener) }
}

/**
 * Create a flow of [destination change events][NavControllerOnDestinationChangeEvent] on
 * [NavController].
 *
 * Example:
 *
 * ```
 * navController.destinationChangeEvents()
 *      .onEach { /* handle destination change event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun NavController.destinationChangeEvents(): Flow<NavControllerOnDestinationChangeEvent> =
    channelFlow {
        val listener = listener(this, ::offerCatching)
        addOnDestinationChangedListener(listener)
        awaitClose { removeOnDestinationChangedListener(listener) }
    }

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (NavControllerOnDestinationChangeEvent) -> Boolean
) = NavController.OnDestinationChangedListener { controller, destination, arguments ->
    if (scope.isActive) {
        emitter(NavControllerOnDestinationChangeEvent(controller, destination, arguments))
    }
}
