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
import kotlinx.coroutines.channels.SendChannel

// Since offer() can throw when the channel is closed (channel can close before the block within
// awaitClose), wrap `offer` calls inside `runCatching`.
// See: https://github.com/Kotlin/kotlinx.coroutines/issues/974
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> SendChannel<T>.offerCatching(element: T): Boolean =
    runCatching { offer(element) }.getOrDefault(false)
