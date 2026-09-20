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
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class RangeSliderValuesChangeEventsTest {

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        rangeSliderValuesChangeEventsFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        rangeSliderValuesChangeEventsFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        rangeSliderValuesChangeEventsFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }

    @Test
    fun `previous values advance after every change`() = runBlocking {
        val slider = newRangeSlider(materialTestContext())
        slider.valuesChangeEvents().test {
            val initial = awaitItem()
            slider.values = RANGE_FIRST_VALUES
            val firstChange = awaitItem()
            awaitItem()
            slider.values = RANGE_SECOND_VALUES
            val secondChange = awaitItem()
            awaitItem()
            assertEquals(RANGE_INITIAL_VALUES, initial.previousValues)
            assertEquals(RANGE_INITIAL_VALUES, firstChange.previousValues)
            assertEquals(RANGE_FIRST_VALUES, secondChange.previousValues)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun rangeSliderValuesChangeEventsFixture(context: Context): PerFileInitialValueFlowFixture {
    val slider = newRangeSlider(context)
    return PerFileFixture(
        slider.valuesChangeEvents(),
        listOf(RANGE_INITIAL_VALUES, RANGE_FIRST_VALUES, RANGE_SECOND_VALUES),
        { slider.values = it },
        normalize = RangeSliderChangeEvent::toSnapshot,
        initialValue = { values -> RangeEventSnapshot(RangeSliderSide.INIT, values, values, false) },
        callbackValue = { previous, values ->
            RangeEventSnapshot(RangeSliderSide.LEFT, values, previous, false)
        },
    )
}

private fun newRangeSlider(context: Context): RangeSlider = RangeSlider(context).apply {
    valueFrom = 0f
    valueTo = 100f
    values = RANGE_INITIAL_VALUES
}

private data class RangeEventSnapshot(
    val changedSide: RangeSliderSide,
    val newValues: List<Float>,
    val previousValues: List<Float>,
    val fromUser: Boolean,
)

private fun RangeSliderChangeEvent.toSnapshot() = RangeEventSnapshot(
    changedSide,
    newValues.toList(),
    previousValues.toList(),
    fromUser,
)

private val RANGE_INITIAL_VALUES = listOf(10f, 60f)
private val RANGE_FIRST_VALUES = listOf(20f, 60f)
private val RANGE_SECOND_VALUES = listOf(30f, 60f)
