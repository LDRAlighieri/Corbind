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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class InitialValueFlow<T>(private val flow: Flow<T>) : Flow<T> by flow {
    fun dropInitialValue(): Flow<T> = drop(1)
    suspend fun asStateFlow(scope: CoroutineScope): StateFlow<T> = stateIn(scope = scope)
}

fun <T> Flow<T>.asInitialValueFlow(value: T): InitialValueFlow<T> = InitialValueFlow(
    onStart { emit(value) }
)
