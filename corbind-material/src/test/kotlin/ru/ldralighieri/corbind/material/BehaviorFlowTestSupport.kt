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

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import app.cash.turbine.test
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

internal fun View.installBehavior(behavior: CoordinatorLayout.Behavior<*>) {
    layoutParams = CoordinatorLayout.LayoutParams(1, 1).apply {
        this.behavior = behavior
    }
}

internal suspend fun <T> Flow<T>.assertEventAndCleanup(
    expected: T,
    isRegistered: () -> Boolean,
    fire: () -> Unit,
    isRemoved: () -> Boolean,
) {
    test {
        assertTrue(isRegistered())
        fire()
        assertEquals(expected, awaitItem())
        expectNoEvents()
    }
    assertTrue(isRemoved())
}

internal fun Snackbar.testCallbacks(): List<Snackbar.Callback> {
    val callbacksField = BaseTransientBottomBar::class.java.getDeclaredField("callbacks")
    callbacksField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return (callbacksField.get(this) as? List<Snackbar.Callback>).orEmpty()
}
