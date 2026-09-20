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
import com.google.android.material.slider.Slider
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
class SliderValueChangeEventsTest {

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        sliderValueChangeEventsFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        sliderValueChangeEventsFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        sliderValueChangeEventsFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }

    @Test
    fun `previous value advances after every callback`() = runBlocking {
        val slider = newSlider(materialTestContext())
        slider.valueChangeEvents().test {
            val initial = awaitItem()
            slider.value = SLIDER_FIRST_VALUE
            val firstChange = awaitItem()
            slider.value = SLIDER_SECOND_VALUE
            val secondChange = awaitItem()
            assertEquals(SLIDER_INITIAL_VALUE, initial.previousValue)
            assertEquals(SLIDER_INITIAL_VALUE, firstChange.previousValue)
            assertEquals(SLIDER_FIRST_VALUE, secondChange.previousValue)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun sliderValueChangeEventsFixture(context: Context): PerFileInitialValueFlowFixture {
    val slider = newSlider(context)
    return PerFileFixture(
        slider.valueChangeEvents(),
        listOf(SLIDER_INITIAL_VALUE, SLIDER_FIRST_VALUE, SLIDER_SECOND_VALUE),
        { slider.value = it },
        normalize = SliderChangeEvent::toSnapshot,
        initialValue = { value -> SliderChangeSnapshot(value, value, false) },
        callbackValue = { previous, value -> SliderChangeSnapshot(value, previous, false) },
    )
}

private fun newSlider(context: Context): Slider = Slider(context).apply {
    valueFrom = 0f
    valueTo = 100f
    value = SLIDER_INITIAL_VALUE
}

private data class SliderChangeSnapshot(
    val value: Float,
    val previousValue: Float,
    val fromUser: Boolean,
)

private fun SliderChangeEvent.toSnapshot() = SliderChangeSnapshot(value, previousValue, fromUser)

private const val SLIDER_INITIAL_VALUE = 10f
private const val SLIDER_FIRST_VALUE = 20f
private const val SLIDER_SECOND_VALUE = 30f
