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
import com.google.android.material.textfield.TextInputLayout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class TextInputLayoutEndIconChangesTest {
    @Test
    fun `flow is cold emits previous icon mode and removes listener`() {
        val layout = TrackingTextInputLayout(materialTestContext())
        assertCallbackFlowContract(
            flow = layout.endIconChanges(),
            isRegistered = { layout.listener != null },
            dispatch = { checkNotNull(layout.listener).onEndIconChanged(layout, TextInputLayout.END_ICON_NONE) },
            expected = TextInputLayout.END_ICON_NONE,
            isCleaned = { layout.listener == null && layout.cleanupCount == 1 },
        )
    }

    private class TrackingTextInputLayout(context: Context) : TextInputLayout(context) {
        var listener: OnEndIconChangedListener? = null
            private set
        var cleanupCount = 0
            private set

        override fun addOnEndIconChangedListener(listener: OnEndIconChangedListener) {
            super.addOnEndIconChangedListener(listener)
            this.listener = listener
        }

        override fun removeOnEndIconChangedListener(listener: OnEndIconChangedListener) {
            super.removeOnEndIconChangedListener(listener)
            if (this.listener === listener) this.listener = null
            cleanupCount++
        }
    }
}
