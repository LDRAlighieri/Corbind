[![Corbind](logo.svg)](https://ldralighieri.github.io/Corbind)

[![Kotlin Version](https://img.shields.io/badge/Kotlin-v2.4.20-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Kotlin Coroutines Version](https://img.shields.io/badge/Coroutines-v1.11.0-blue.svg)](https://kotlinlang.org/docs/coroutines-overview.html)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a1c9a1b1d1ce4ca7a201ab93492bf6e0)](https://app.codacy.com/gh/LDRAlighieri/Corbind)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com/tools/releases/platforms?hl=ru#6.0)
[![Publish status](https://github.com/LDRAlighieri/Corbind/actions/workflows/publish.yml/badge.svg)](https://github.com/LDRAlighieri/Corbind/actions)

[![Google Dev Library](https://img.shields.io/badge/Google_DevLibrary-Corbind-blue)](https://devlibrary.withgoogle.com/products/android/repos/LDRAlighieri-Corbind)
[![Android Weekly](https://androidweekly.net/issues/issue-377/badge)](https://androidweekly.net/issues/issue-377)

<br>

⚡ Kotlin Coroutines binding APIs for Android UI widgets from the platform, AndroidX, and Material libraries. Use cold [Flow][flow] bindings for new code; hot [ReceiveChannel][channel] and [actor][actor]-based action overloads remain available for existing integrations.

## Description

Corbind turns Android UI callbacks into cold [Flow][flow] bindings or hot [ReceiveChannel][channel] bindings. The action overloads use the [actor][actor] coroutine API, which is marked obsolete upstream.
Please consider giving this repository a star ⭐ if you like the project.

## Articles
* [⚡ Kotlin Coroutine binding with Flow support][kotlin-coroutine-binding]
* [What’s up Corbind! Release 1.7.0 🎉. It’s been a long road][release-1.7.0]

## Current versions

| Module                       | Version                                                                                                                                                                                                  |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [corbind-bom]                | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-bom.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-bom)                               |
| [corbind]                    | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind)                                       |
| [corbind-activity]           | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-activity.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-activity)                     |
| [corbind-appcompat]          | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-appcompat.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-appcompat)                   |
| [corbind-core]               | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-core.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-core)                             |
| [corbind-drawerlayout]       | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-drawerlayout.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-drawerlayout)             |
| [corbind-fragment]           | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-fragment.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-fragment)                     |
| [corbind-leanback]           | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-leanback.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-leanback)                     |
| [corbind-lifecycle]          | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-lifecycle.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-lifecycle)                   |
| [corbind-material]           | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-material.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-material)                     |
| [corbind-navigation]         | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-navigation.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-navigation)                 |
| [corbind-recyclerview]       | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-recyclerview.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-recyclerview)             |
| [corbind-slidingpanelayout]  | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-slidingpanelayout.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-slidingpanelayout)   |
| [corbind-swiperefreshlayout] | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-swiperefreshlayout.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-swiperefreshlayout) |
| [corbind-viewpager] (legacy) | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-viewpager.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-viewpager)                   |
| [corbind-viewpager2]         | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-viewpager2.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-viewpager2)                 |

## Using in your projects

Platform bindings:
```kotlin
dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2026.02.00"))
    implementation("ru.ldralighieri.corbind:corbind")
}
```

AndroidX library bindings:
```kotlin
dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2026.02.00"))
    implementation("ru.ldralighieri.corbind:corbind-activity")
    implementation("ru.ldralighieri.corbind:corbind-appcompat")
    implementation("ru.ldralighieri.corbind:corbind-core")
    implementation("ru.ldralighieri.corbind:corbind-drawerlayout")
    implementation("ru.ldralighieri.corbind:corbind-fragment")
    implementation("ru.ldralighieri.corbind:corbind-leanback")
    implementation("ru.ldralighieri.corbind:corbind-lifecycle")
    implementation("ru.ldralighieri.corbind:corbind-navigation")
    implementation("ru.ldralighieri.corbind:corbind-recyclerview")
    implementation("ru.ldralighieri.corbind:corbind-slidingpanelayout")
    implementation("ru.ldralighieri.corbind:corbind-swiperefreshlayout")
    implementation("ru.ldralighieri.corbind:corbind-viewpager") // legacy
    implementation("ru.ldralighieri.corbind:corbind-viewpager2")
}
```

Use `corbind-viewpager2` for new screens. The `corbind-viewpager` module supports the older `androidx.viewpager.widget.ViewPager`; see the [ViewPager2 migration guide][viewpager2-migration].

Google 'material' library bindings:
```kotlin
dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2026.02.00"))
    implementation("ru.ldralighieri.corbind:corbind-material")
}
```

Snapshot build:
```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2026.03.00-SNAPSHOT"))
    implementation("ru.ldralighieri.corbind:{module}")
}
```

## List of extensions

You can find a list of extensions in the description of each module:  
* [corbind]
* [corbind-activity]
* [corbind-appcompat]
* [corbind-core]
* [corbind-drawerlayout]
* [corbind-fragment]
* [corbind-leanback]
* [corbind-lifecycle]
* [corbind-material]
* [corbind-navigation]
* [corbind-recyclerview]
* [corbind-slidingpanelayout]
* [corbind-swiperefreshlayout]
* [corbind-viewpager] (legacy)
* [corbind-viewpager2]

## How to use it?

For one cold Flow, `flowWithLifecycle` restarts collection when the Activity reaches `STARTED`. `textChanges()` returns `InitialValueFlow`, so each new collection begins with the current text:

```kotlin
findViewById<EditText>(R.id.etName)
    .textChanges()
    .onEach { text -> /* handle the current text and later changes */ }
    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
    .launchIn(lifecycleScope)
```

Use `repeatOnLifecycle` to scope collection explicitly, especially when collecting several flows in parallel ([Android lifecycle guidance][lifecycle-aware-collection]). In a Fragment, use `viewLifecycleOwner.lifecycle` and `viewLifecycleOwner.lifecycleScope` so collection ends when the view is destroyed:

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(
            etEmail.textChanges().map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },
            etPassword.textChanges().map { it.length > 7 },
        ) { email, password -> email && password }
            .collect { btLogin.isEnabled = it }
    }
}
```

Each resumed Flow collection registers a new listener and reads the current value again. Use `dropInitialValue()` on an `InitialValueFlow` when only later changes matter.

### Existing channel and action overloads

A `ReceiveChannel` is hot: the binding starts when the channel is created, not when it is consumed. Create it inside the lifecycle block and pass that block's scope so its listener is removed at `STOPPED`:

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        findViewById<ViewPager2>(R.id.vpSlides)
            .pageSelections(this) // ReceiveChannel<Int>
            .consumeEach { page -> /* handle the selected page */ }
    }
}
```

The passed scope's `Job` owns the channel. Cancelling that job or the returned channel removes the listener; cancelling the channel does not cancel the scope. A scope that is already cancelled registers nothing. Flow and channel bindings can be collected or created from any dispatcher, while Android listener registration and cleanup run on the main thread. Synchronous action overloads must be called on the main thread.

For new code, replace `view.clicks(scope)` with `view.clicks()` and collect the Flow. Replace actor-based `view.clicks(scope) { action() }` with a Flow collector:

```kotlin
findViewById<AppCompatButton>(R.id.btConfirm)
    .clicks()
    .onEach { /* perform the action */ }
    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
    .launchIn(lifecycleScope)
```

Some bindings use Android's single-listener `setOn...Listener` methods, such as `View.clicks()`. Give each such callback one active owner: a second binding or direct listener assignment can replace the first listener, and cleanup can clear the replacement. Share events downstream of one binding if several consumers need them.

Channel overloads default to `Channel.RENDEZVOUS`; Flow producers use a buffered channel. Both default to suspending overflow: when the buffer is full, Corbind queues sends in order without blocking Android callbacks. A slow consumer can therefore accumulate pending sends without bound. Use `Channel.CONFLATED` for channel bindings, or Flow `conflate()` / `buffer(..., onBufferOverflow = ...)` when discarding intermediate events is acceptable. Choose this explicitly for each event source.

More examples are in module descriptions and source code.

## Missed or forgot something?

If I forgot something or you have any ideas what can be added or corrected, please create an issue or contact me directly.

## Special thanks to

[Jake Wharton][jw]. This project is inspired by [RxBinding][rx].

## License

```
Copyright 2019-2026 Vladimir Raupov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[jw]: https://github.com/JakeWharton
[rx]: https://github.com/JakeWharton/RxBinding
[flow]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/
[channel]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/
[actor]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/actor.html

[kotlin-coroutine-binding]: https://medium.com/@ldralighieri/kotlin-coroutine-binding-with-flow-support-68499492a89c
[release-1.7.0]: https://medium.com/@ldralighieri/whats-up-corbind-release-1-7-0-it-s-been-a-long-road-eadf84db19c1
[viewpager2-migration]: https://developer.android.com/develop/ui/views/animations/vp2-migration
[lifecycle-aware-collection]: https://developer.android.com/topic/libraries/architecture/views/coroutines-views

[corbind-bom]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-bom
[corbind]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind
[corbind-activity]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-activity
[corbind-appcompat]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-appcompat
[corbind-core]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-core
[corbind-drawerlayout]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-drawerlayout
[corbind-fragment]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-fragment
[corbind-leanback]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-leanback
[corbind-lifecycle]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-lifecycle
[corbind-material]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-material
[corbind-navigation]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-navigation
[corbind-recyclerview]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-recyclerview
[corbind-slidingpanelayout]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-slidingpanelayout
[corbind-swiperefreshlayout]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-swiperefreshlayout
[corbind-viewpager]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-viewpager
[corbind-viewpager2]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-viewpager2
