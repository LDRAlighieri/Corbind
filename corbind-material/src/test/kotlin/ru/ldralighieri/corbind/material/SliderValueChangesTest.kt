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
import com.google.android.material.slider.Slider
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
class SliderValueChangesTest {

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        sliderValueChangesFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        sliderValueChangesFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        sliderValueChangesFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }
}

private fun sliderValueChangesFixture(context: Context): PerFileInitialValueFlowFixture {
    val slider = Slider(context).apply {
        valueFrom = 0f
        valueTo = 100f
        value = SLIDER_INITIAL_VALUE
    }
    return PerFileFixture(
        slider.valueChanges(),
        listOf(SLIDER_INITIAL_VALUE, SLIDER_FIRST_VALUE, SLIDER_SECOND_VALUE),
        { slider.value = it },
    )
}

private const val SLIDER_INITIAL_VALUE = 10f
private const val SLIDER_FIRST_VALUE = 20f
private const val SLIDER_SECOND_VALUE = 30f
