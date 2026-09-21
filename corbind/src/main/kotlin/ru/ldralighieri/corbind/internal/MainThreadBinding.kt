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

package ru.ldralighieri.corbind.internal

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import kotlinx.coroutines.channels.SendChannel

private val mainHandler by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Handler(Looper.getMainLooper())
}

/** Verifies the main-thread contract of the synchronous action binding overloads. */
@MainThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Corbind action bindings must be created on the Android main thread"
    }
}

/** Runs a channel cleanup callback on the Android main thread. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SendChannel<*>.invokeOnCloseOnMain(block: () -> Unit) {
    invokeOnClose {
        if (Looper.myLooper() === Looper.getMainLooper()) {
            block()
        } else {
            check(mainHandler.post { block() }) {
                "Unable to schedule Corbind cleanup on the Android main thread"
            }
        }
    }
}
