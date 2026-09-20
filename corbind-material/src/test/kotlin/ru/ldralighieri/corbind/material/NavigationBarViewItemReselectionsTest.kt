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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.flow.map
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class NavigationBarViewItemReselectionsTest {
    @Test
    fun `flow is cold emits reselected item and clears listener`() {
        val view = TrackingBottomNavigationView(materialTestContext())
        val item = view.menu.add(0, 1, 0, "First")
        assertCallbackFlowContract(
            flow = view.itemReselections().map { it.itemId },
            isRegistered = { view.listener != null },
            dispatch = { checkNotNull(view.listener).onNavigationItemReselected(item) },
            expected = 1,
            isCleaned = { view.listener == null && view.cleanupCount == 1 },
        )
    }

    private class TrackingBottomNavigationView(context: Context) : BottomNavigationView(context) {
        var listener: NavigationBarView.OnItemReselectedListener? = null
            private set
        var cleanupCount = 0
            private set

        override fun setOnItemReselectedListener(listener: NavigationBarView.OnItemReselectedListener?) {
            super.setOnItemReselectedListener(listener)
            this.listener = listener
            if (listener == null) cleanupCount++
        }
    }
}
