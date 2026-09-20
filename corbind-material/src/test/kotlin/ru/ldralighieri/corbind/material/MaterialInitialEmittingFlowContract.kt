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

internal const val FIRST_MATERIAL_ITEM = 1
internal const val SECOND_MATERIAL_ITEM = 2

internal interface InitialEmittingFlowFixture {
    val registrationCount: Int
    val cleanupCount: Int
    val isListenerRegistered: Boolean

    fun flow(): Flow<String>
    fun select(itemId: Int)
    fun dispatchSelection(itemId: Int)
    fun dispatchSelectionDuringNextRegistration(itemId: Int)
}

internal object InitialEmittingFlowContract {
    fun coldAndReadsStateWhenCollected(fixture: InitialEmittingFlowFixture) = runBlocking {
        fixture.select(FIRST_MATERIAL_ITEM)
        val flow = fixture.flow()
        assertEquals(0, fixture.registrationCount)
        assertFalse(fixture.isListenerRegistered)

        fixture.select(SECOND_MATERIAL_ITEM)
        flow.test {
            assertEquals("selected:$SECOND_MATERIAL_ITEM", awaitItem())
        }
    }

    fun eachCollectionReadsFreshState(fixture: InitialEmittingFlowFixture) = runBlocking {
        val flow = fixture.flow()
        fixture.select(FIRST_MATERIAL_ITEM)
        flow.test {
            assertEquals("selected:$FIRST_MATERIAL_ITEM", awaitItem())
        }
        fixture.select(SECOND_MATERIAL_ITEM)
        flow.test {
            assertEquals("selected:$SECOND_MATERIAL_ITEM", awaitItem())
        }
        assertEquals(2, fixture.registrationCount)
        assertEquals(2, fixture.cleanupCount)
    }

    fun callbackFollowsInitialValue(fixture: InitialEmittingFlowFixture) = runBlocking {
        fixture.select(FIRST_MATERIAL_ITEM)
        fixture.flow().test {
            assertEquals("selected:$FIRST_MATERIAL_ITEM", awaitItem())
            fixture.dispatchSelection(SECOND_MATERIAL_ITEM)
            assertEquals("selected:$SECOND_MATERIAL_ITEM", awaitItem())
        }
        assertEquals(1, fixture.cleanupCount)
        assertFalse(fixture.isListenerRegistered)
    }

    fun registrationCallbackFollowsInitialValue(fixture: InitialEmittingFlowFixture) = runBlocking {
        fixture.select(FIRST_MATERIAL_ITEM)
        fixture.dispatchSelectionDuringNextRegistration(SECOND_MATERIAL_ITEM)
        fixture.flow().test {
            assertEquals("selected:$FIRST_MATERIAL_ITEM", awaitItem())
            assertEquals("selected:$SECOND_MATERIAL_ITEM", awaitItem())
        }
        assertEquals(1, fixture.registrationCount)
        assertEquals(1, fixture.cleanupCount)
        assertFalse(fixture.isListenerRegistered)
    }

    fun initialValueOnlyCleansUp(fixture: InitialEmittingFlowFixture) = runBlocking {
        fixture.select(FIRST_MATERIAL_ITEM)
        fixture.flow().test {
            assertEquals("selected:$FIRST_MATERIAL_ITEM", awaitItem())
        }
        assertEquals(1, fixture.registrationCount)
        assertEquals(1, fixture.cleanupCount)
        assertFalse(fixture.isListenerRegistered)
    }
}
