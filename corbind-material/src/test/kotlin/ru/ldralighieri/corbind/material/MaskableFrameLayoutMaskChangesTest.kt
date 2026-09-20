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
import android.graphics.RectF
import android.os.Build
import com.google.android.material.carousel.MaskableFrameLayout
import com.google.android.material.carousel.OnMaskChangedListener
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class MaskableFrameLayoutMaskChangesTest {
    @Test
    fun `flow is cold emits mask and removes listener`() {
        val view = TrackingMaskableFrameLayout(materialTestContext())
        val mask = RectF(1f, 2f, 3f, 4f)
        assertCallbackFlowContract(
            flow = view.maskChanges(),
            isRegistered = { view.listener != null },
            dispatch = { checkNotNull(view.listener).onMaskChanged(mask) },
            expected = mask,
            isCleaned = { view.listener == null && view.cleanupCount == 1 },
        )
    }

    private class TrackingMaskableFrameLayout(context: Context) : MaskableFrameLayout(context) {
        var listener: OnMaskChangedListener? = null
            private set
        var cleanupCount = 0
            private set

        override fun setOnMaskChangedListener(listener: OnMaskChangedListener?) {
            super.setOnMaskChangedListener(listener)
            this.listener = listener
            if (listener == null) cleanupCount++
        }
    }
}
