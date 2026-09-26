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
import ru.ldralighieri.corbind.material.views.TrackingSwipeDismissBehavior

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class SwipeDismissBehaviorDragStateChangesTest {

    @Test
    fun `drag callback emits state and cancellation clears behavior listener`() = runBlocking {
        val behavior = TrackingSwipeDismissBehavior()
        val view = View(materialTestContext()).apply { installBehavior(behavior) }
        val changes = view.dragStateChanges()
        assertEquals(0, behavior.registered)

        changes.assertEventAndCleanup(
            expected = SwipeDismissBehavior.STATE_DRAGGING,
            isRegistered = { behavior.capturedListener != null },
            fire = {
                requireNotNull(behavior.capturedListener).onDragStateChanged(
                    SwipeDismissBehavior.STATE_DRAGGING,
                )
            },
            isRemoved = {
                behavior.capturedListener == null && behavior.registered == 1 && behavior.removed == 1
            },
        )
    }
}
