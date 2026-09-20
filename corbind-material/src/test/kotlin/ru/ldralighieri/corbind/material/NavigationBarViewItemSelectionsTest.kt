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
import android.view.Menu
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class NavigationBarViewItemSelectionsTest {
    private fun fixture() = NavigationBarSelectionsFixture(materialTestContext())

    @Test
    fun `flow is cold and reads state when collection starts`() {
        InitialEmittingFlowContract.coldAndReadsStateWhenCollected(fixture())
    }

    @Test
    fun `each collection reads a fresh state snapshot`() {
        InitialEmittingFlowContract.eachCollectionReadsFreshState(fixture())
    }

    @Test
    fun `callback after initial value is delivered`() {
        InitialEmittingFlowContract.callbackFollowsInitialValue(fixture())
    }

    @Test
    fun `callback during registration follows initial value`() {
        InitialEmittingFlowContract.registrationCallbackFollowsInitialValue(fixture())
    }

    @Test
    fun `collecting only initial value cleans up listener`() {
        InitialEmittingFlowContract.initialValueOnlyCleansUp(fixture())
    }
}

private class NavigationBarSelectionsFixture(context: Context) : InitialEmittingFlowFixture {
    private val view = TrackingSelectionBottomNavigationView(context).apply {
        menu.add(1, FIRST_MATERIAL_ITEM, Menu.NONE, "First")
        menu.add(1, SECOND_MATERIAL_ITEM, Menu.NONE, "Second")
        menu.setGroupCheckable(1, true, true)
        selectedItemId = FIRST_MATERIAL_ITEM
        resetTracking()
    }

    override val registrationCount: Int get() = view.registrationCount
    override val cleanupCount: Int get() = view.cleanupCount
    override val isListenerRegistered: Boolean get() = view.itemSelectedListener != null

    override fun flow(): Flow<String> = view.itemSelections().map { "selected:${it.itemId}" }
    override fun select(itemId: Int) {
        view.selectedItemId = itemId
    }
    override fun dispatchSelection(itemId: Int) {
        checkNotNull(view.itemSelectedListener).onNavigationItemSelected(view.menu.findItem(itemId))
    }
    override fun dispatchSelectionDuringNextRegistration(itemId: Int) {
        view.dispatchSelectionDuringNextRegistration(itemId)
    }
}

private class TrackingSelectionBottomNavigationView(context: Context) : BottomNavigationView(context) {
    var registrationCount = 0
        private set
    var cleanupCount = 0
        private set
    var itemSelectedListener: NavigationBarView.OnItemSelectedListener? = null
        private set
    private var selectionDuringRegistration: Int? = null

    override fun setOnItemSelectedListener(listener: NavigationBarView.OnItemSelectedListener?) {
        super.setOnItemSelectedListener(listener)
        itemSelectedListener = listener
        if (listener == null) {
            cleanupCount++
        } else {
            registrationCount++
            selectionDuringRegistration?.let { itemId ->
                selectionDuringRegistration = null
                listener.onNavigationItemSelected(menu.findItem(itemId))
            }
        }
    }

    fun dispatchSelectionDuringNextRegistration(itemId: Int) {
        selectionDuringRegistration = itemId
    }
    fun resetTracking() {
        registrationCount = 0
        cleanupCount = 0
        itemSelectedListener = null
    }
}
