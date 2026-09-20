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

import android.os.Build
import android.view.View
import com.google.android.material.behavior.SwipeDismissBehavior
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class SwipeDismissBehaviorDesmissesTest {

    @Test
    fun `dismiss callback emits view and cancellation clears behavior listener`() = runBlocking {
        val behavior = DismissTrackingBehavior()
        val view = View(materialTestContext()).apply { installBehavior(behavior) }
        val dismisses = view.dismisses()
        assertEquals(0, behavior.registered)

        dismisses.assertEventAndCleanup(
            expected = view,
            isRegistered = { behavior.capturedListener != null },
            fire = { requireNotNull(behavior.capturedListener).onDismiss(view) },
            isRemoved = {
                behavior.capturedListener == null && behavior.registered == 1 && behavior.removed == 1
            },
        )
    }

    private class DismissTrackingBehavior : SwipeDismissBehavior<View>() {

        var capturedListener: OnDismissListener? = null
            private set
        var registered = 0
            private set
        var removed = 0
            private set

        override fun setListener(listener: OnDismissListener?) {
            capturedListener = listener
            if (listener == null) removed++ else registered++
            super.setListener(listener)
        }
    }
}
