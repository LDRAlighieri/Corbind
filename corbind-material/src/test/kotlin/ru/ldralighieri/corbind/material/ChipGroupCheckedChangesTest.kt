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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
class ChipGroupCheckedChangesTest {

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        chipGroupCheckedChangesFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        chipGroupCheckedChangesFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        chipGroupCheckedChangesFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }
}

private fun chipGroupCheckedChangesFixture(context: Context): PerFileInitialValueFlowFixture {
    val firstId = View.generateViewId()
    val secondId = View.generateViewId()
    val group = ChipGroup(context).apply {
        addView(Chip(context).apply { id = firstId })
        addView(Chip(context).apply { id = secondId })
    }
    return PerFileFixture(
        group.checkedChanges(),
        listOf(emptyList(), listOf(firstId), listOf(firstId, secondId)),
        { checkedIds ->
            group.clearCheck()
            checkedIds.forEach(group::check)
        },
        normalize = List<Int>::toList,
        initialValue = List<Int>::toList,
    )
}
