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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on destination change on [NavController].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun NavController.destinationChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (NavDestination) -> Unit
) {
    val events = scope.actor<NavDestination>(Dispatchers.Main.immediate, capacity) {
        for (destination in channel) action(destination)
    }

    val listener = listener(scope, events::offer)
    addOnDestinationChangedListener(listener)
    events.invokeOnClose { removeOnDestinationChangedListener(listener) }
}

/**
 * Perform an action on destination change on [NavController], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun NavController.destinationChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (NavDestination) -> Unit
) = coroutineScope {
    destinationChanges(this, capacity, action)
}

/**
 * Create a channel of destination change on [NavController].
 *
 * Example:
 *
 * ```
 * launch {
 *      navController.destinationChanges(scope)
 *          .consumeEach { /* handle destination change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun NavController.destinationChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<NavDestination> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::safeOffer)
    addOnDestinationChangedListener(listener)
    invokeOnClose { removeOnDestinationChangedListener(listener) }
}

/**
 * Create a flow of destination change on [NavController].
 *
 * Example:
 *
 * ```
 * navController.destinationChanges()
 *      .onEach { /* handle destination change */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun NavController.destinationChanges(): Flow<NavDestination> =
    channelFlow {
        val listener = listener(this, ::offer)
        addOnDestinationChangedListener(listener)
        awaitClose { removeOnDestinationChangedListener(listener) }
    }


@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (NavDestination) -> Boolean
) = NavController.OnDestinationChangedListener { _, destination, _ ->
    if (scope.isActive) { emitter(destination) }
}
