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
import com.google.android.material.behavior.HideViewOnScrollBehavior
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
class HideViewOnScrollBehaviorScrollStateChangesTest {

    @Test
    fun `scroll callback emits state and cancellation removes listener`() = runBlocking {
        val behavior = TrackingHideViewBehavior()
        val view = View(materialTestContext()).apply { installBehavior(behavior) }
        val changes = view.hideOnScrollStateChanges()
        assertEquals(0, behavior.added)

        changes.assertEventAndCleanup(
            expected = HideViewOnScrollBehavior.STATE_SCROLLED_OUT,
            isRegistered = { behavior.listener != null },
            fire = {
                requireNotNull(behavior.listener).onStateChanged(
                    view,
                    HideViewOnScrollBehavior.STATE_SCROLLED_OUT,
                )
            },
            isRemoved = { behavior.listener == null && behavior.added == 1 && behavior.removed == 1 },
        )
    }

    private class TrackingHideViewBehavior : HideViewOnScrollBehavior<View>() {

        var listener: OnScrollStateChangedListener? = null
            private set
        var added = 0
            private set
        var removed = 0
            private set

        override fun addOnScrollStateChangedListener(listener: OnScrollStateChangedListener) {
            this.listener = listener
            added++
        }

        override fun removeOnScrollStateChangedListener(listener: OnScrollStateChangedListener) {
            if (this.listener === listener) this.listener = null
            removed++
        }
    }
}
