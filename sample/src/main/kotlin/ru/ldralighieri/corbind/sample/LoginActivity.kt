/*
 * Copyright 2019 Vladimir Raupov
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

package ru.ldralighieri.corbind.sample

import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.*
import ru.ldralighieri.corbind.sample.core.CorbindActivity
import ru.ldralighieri.corbind.sample.core.extensions.hideSoftInput
import ru.ldralighieri.corbind.sample.databinding.ActivityLoginBinding
import ru.ldralighieri.corbind.swiperefreshlayout.refreshes
import ru.ldralighieri.corbind.view.clicks
import ru.ldralighieri.corbind.widget.editorActionEvents
import ru.ldralighieri.corbind.widget.textChanges

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val view = window.decorView
            view.systemUiVisibility = view.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindViews()
    }

    private fun bindViews() {
        with(binding) {
            combine(
                etEmail.textChanges()
                    .map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },

                etPassword.textChanges()
                    .map { it.length > 7 },

                transform = { email, password -> email && password }
            )
                .onEach { btLogin.isEnabled = it }
                .launchIn(lifecycleScope)

            merge(
                btLogin.clicks(),

                etPassword.editorActionEvents()
                    .filter { it.actionId == EditorInfo.IME_ACTION_DONE }
                    .filter { btLogin.isEnabled }
                    .onEach { hideSoftInput() }
            )
                .onEach {
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.login_success_message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .launchIn(lifecycleScope)

            swipe.refreshes()
                .onEach {
                    etEmail.text?.clear()
                    etPassword.text?.clear()
                    swipe.isRefreshing = false
                    hideSoftInput()
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.login_swipe_message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .launchIn(lifecycleScope)
        }
    }
}
