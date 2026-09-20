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
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals

internal enum class InitialFlowState { INITIAL, FIRST, SECOND }

internal interface PerFileInitialValueFlowFixture {
    val flow: Flow<Any>

    fun moveTo(state: InitialFlowState)
    fun expectedInitialValue(state: InitialFlowState): Any
    fun expectedCallbackValue(previousState: InitialFlowState, state: InitialFlowState): Any
}

internal suspend fun PerFileInitialValueFlowFixture.assertStateAtCollection() {
    val changes = flow
    moveTo(InitialFlowState.FIRST)
    changes.test {
        assertEquals(expectedInitialValue(InitialFlowState.FIRST), awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}

internal suspend fun PerFileInitialValueFlowFixture.assertFreshStatePerCollection() {
    val changes = flow
    moveTo(InitialFlowState.FIRST)
    changes.test {
        assertEquals(expectedInitialValue(InitialFlowState.FIRST), awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
    moveTo(InitialFlowState.SECOND)
    changes.test {
        assertEquals(expectedInitialValue(InitialFlowState.SECOND), awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}

internal suspend fun PerFileInitialValueFlowFixture.assertCallbackAfterInitialValue() {
    flow.test {
        assertEquals(expectedInitialValue(InitialFlowState.INITIAL), awaitItem())
        moveTo(InitialFlowState.FIRST)
        assertEquals(
            expectedCallbackValue(InitialFlowState.INITIAL, InitialFlowState.FIRST),
            awaitItem(),
        )
        cancelAndIgnoreRemainingEvents()
    }
}

internal class PerFileFixture<S : Any, T : Any>(
    source: Flow<T>,
    private val states: List<S>,
    private val move: (S) -> Unit,
    normalize: (T) -> Any = { it },
    private val initialValue: (S) -> Any = { it },
    private val callbackValue: (S, S) -> Any = { _, state -> initialValue(state) },
) : PerFileInitialValueFlowFixture {

    override val flow: Flow<Any> = source.map(normalize)

    override fun moveTo(state: InitialFlowState) {
        move(states[state.ordinal])
    }

    override fun expectedInitialValue(state: InitialFlowState): Any = initialValue(states[state.ordinal])

    override fun expectedCallbackValue(previousState: InitialFlowState, state: InitialFlowState): Any = callbackValue(states[previousState.ordinal], states[state.ordinal])
}
