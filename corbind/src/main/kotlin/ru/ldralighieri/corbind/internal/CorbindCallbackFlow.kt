/*
 * Copyright 2026 Vladimir Raupov
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Creates a cold callback flow whose registration and cleanup run on the Android main thread.
 * An additional downstream `flowOn` cannot move the callback lifecycle away from Main because the
 * closest dispatcher to the producer takes precedence during operator fusion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> corbindCallbackFlow(block: suspend ProducerScope<T>.() -> Unit): Flow<T> = callbackFlow {
    ensureActive()
    block()
}.flowOn(Dispatchers.Main.immediate)
