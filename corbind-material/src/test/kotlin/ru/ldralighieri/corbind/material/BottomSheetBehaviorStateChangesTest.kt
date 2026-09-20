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
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
class BottomSheetBehaviorStateChangesTest {

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        bottomSheetStateChangesFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        bottomSheetStateChangesFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        bottomSheetStateChangesFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }
}

private fun bottomSheetStateChangesFixture(context: Context): PerFileInitialValueFlowFixture {
    val behavior = BottomSheetStateTrackingBehavior()
    val view = View(context).apply {
        layoutParams = CoordinatorLayout.LayoutParams(1, 1).apply { this.behavior = behavior }
    }
    return PerFileFixture(
        view.stateChanges(),
        listOf(
            BottomSheetBehavior.STATE_COLLAPSED,
            BottomSheetBehavior.STATE_EXPANDED,
            BottomSheetBehavior.STATE_HALF_EXPANDED,
        ),
        { behavior.moveTo(view, it) },
    )
}

private class BottomSheetStateTrackingBehavior : BottomSheetBehavior<View>() {
    private val callbacks = mutableListOf<BottomSheetCallback>()

    override fun addBottomSheetCallback(callback: BottomSheetCallback) {
        callbacks += callback
    }

    override fun removeBottomSheetCallback(callback: BottomSheetCallback) {
        callbacks -= callback
    }

    fun moveTo(view: View, newState: Int) {
        state = newState
        callbacks.toList().forEach { it.onStateChanged(view, newState) }
    }
}
