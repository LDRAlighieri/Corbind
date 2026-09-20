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

import android.content.Context
import android.os.Build
import android.os.Looper
import app.cash.turbine.test
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class MaterialCardViewCheckedChangesTest {

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        materialCardViewCheckedChangesFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        materialCardViewCheckedChangesFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        materialCardViewCheckedChangesFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }

    @Test
    fun `cleanup preserves ordinary click listener`() = runBlocking {
        val card = TrackingMaterialCardView(materialTestContext()).apply { isCheckable = true }
        var clicks = 0
        card.setOnClickListener { clicks++ }
        card.checkedChanges().test {
            assertEquals(false, awaitItem())
            assertEquals(1, card.nonNullCheckedListenerCount)
            assertEquals(0, card.nullCheckedListenerCount)
            cancelAndIgnoreRemainingEvents()
            card.isChecked = true
            assertEquals(1, card.nonNullCheckedListenerCount)
            assertEquals(1, card.nullCheckedListenerCount)
            assertTrue(card.performClick())
            assertEquals(1, clicks)
        }
    }

    private class TrackingMaterialCardView(context: Context) : MaterialCardView(context) {
        var nonNullCheckedListenerCount = 0
            private set
        var nullCheckedListenerCount = 0
            private set

        override fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
            if (listener == null) nullCheckedListenerCount++ else nonNullCheckedListenerCount++
            super.setOnCheckedChangeListener(listener)
        }
    }
}

private fun materialCardViewCheckedChangesFixture(context: Context): PerFileInitialValueFlowFixture {
    val card = MaterialCardView(context).apply {
        isCheckable = true
        isChecked = false
    }
    return PerFileFixture(card.checkedChanges(), listOf(false, true, false), { card.isChecked = it })
}
