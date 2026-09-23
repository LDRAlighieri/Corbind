/*
 * Copyright 2021 Vladimir Raupov
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

package ru.ldralighieri.corbind.internal

import androidx.annotation.RestrictTo
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.InitialValueFlow
import java.util.ArrayDeque

/**
 * Buffers callback values until the initial value has been queued. This keeps the initial snapshot
 * first even if listener registration invokes its callback synchronously or from another thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InitialValueFlowEmitter<T> internal constructor(
    private val scope: ProducerScope<T>,
) : (T) -> Boolean {

    private val eventEmitter = scope.corbindEventEmitter()
    private val pendingValues = ArrayDeque<PendingValue<T>>()
    private var initialValueSent = false

    override fun invoke(value: T): Boolean = synchronized(this) {
        when {
            !scope.isActive -> false

            initialValueSent -> eventEmitter(value)

            else -> {
                pendingValues.addLast(PendingValue(value))
                true
            }
        }
    }

    fun sendInitialValue(value: T) {
        synchronized(this) {
            check(!initialValueSent) { "Initial value has already been sent" }
            scope.sendInitialValue(value)
            while (pendingValues.isNotEmpty()) {
                eventEmitter(pendingValues.removeFirst().value)
            }
            initialValueSent = true
        }
    }

    private data class PendingValue<T>(val value: T)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> ProducerScope<T>.initialValueFlowEmitter(): InitialValueFlowEmitter<T> = InitialValueFlowEmitter(this)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> Flow<T>.asInitialValueFlow(): InitialValueFlow<T> = InitialValueFlow(this)
