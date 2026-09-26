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

package ru.ldralighieri.corbind.material.views

import android.view.View
import com.google.android.material.behavior.SwipeDismissBehavior

internal class TrackingSwipeDismissBehavior : SwipeDismissBehavior<View>() {
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
