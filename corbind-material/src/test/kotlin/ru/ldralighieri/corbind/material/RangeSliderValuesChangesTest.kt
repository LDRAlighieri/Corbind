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
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class RangeSliderValuesChangesTest {

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        rangeSliderValuesChangesFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        rangeSliderValuesChangesFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        rangeSliderValuesChangesFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }
}

private fun rangeSliderValuesChangesFixture(context: Context): PerFileInitialValueFlowFixture {
    val slider = RangeSlider(context).apply {
        valueFrom = 0f
        valueTo = 100f
        values = RANGE_INITIAL_VALUES
    }
    return PerFileFixture(
        slider.valuesChanges(),
        listOf(RANGE_INITIAL_VALUES, RANGE_FIRST_VALUES, RANGE_SECOND_VALUES),
        { slider.values = it },
        normalize = List<Float>::toList,
        initialValue = List<Float>::toList,
    )
}

private val RANGE_INITIAL_VALUES = listOf(10f, 60f)
private val RANGE_FIRST_VALUES = listOf(20f, 60f)
private val RANGE_SECOND_VALUES = listOf(30f, 60f)
