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

package ru.ldralighieri.corbind.material

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

internal fun <T> assertCallbackFlowContract(
    flow: Flow<T>,
    isRegistered: () -> Boolean,
    dispatch: () -> Unit,
    expected: T,
    isCleaned: () -> Boolean,
) = runBlocking {
    assertFalse("Listener registered before collection", isRegistered())
    flow.test {
        assertTrue("Listener not registered on collection", isRegistered())
        dispatch()
        assertEquals(expected, awaitItem())
        expectNoEvents()
    }
    assertTrue("Listener not removed on cancellation", isCleaned())
}
