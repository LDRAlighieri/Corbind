[![Corbind](logo.svg)](https://ldralighieri.github.io/Corbind)

[![Kotlin Version](https://img.shields.io/badge/Kotlin-v2.2.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Kotlin Coroutines Version](https://img.shields.io/badge/Coroutines-v1.10.2-blue.svg)](https://kotlinlang.org/docs/reference/coroutines-overview.html)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a1c9a1b1d1ce4ca7a201ab93492bf6e0)](https://app.codacy.com/gh/LDRAlighieri/Corbind)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://developer.android.com/tools/releases/platforms?hl=ru#5.0)
[![Publish status](https://github.com/LDRAlighieri/Corbind/actions/workflows/publish.yml/badge.svg)](https://github.com/LDRAlighieri/Corbind/actions)

[![Google Dev Library](https://img.shields.io/badge/Google_DevLibrary-Corbind-blue)](https://devlibrary.withgoogle.com/products/android/repos/LDRAlighieri-Corbind)
[![Android Weekly](https://androidweekly.net/issues/issue-377/badge)](https://androidweekly.net/issues/issue-377)

<br>

⚡ Kotlin Coroutines binding APIs for Android UI widgets from the platform and support libraries. **Supports Flow, ReceiveChannel and Actor**.


## Description

This library is for Android applications only. Help you to transform Android UI events into cold [Flow][flow], hot [ReceiveChannel][channel] or just perform an action through an [Actor][actor].  
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
| [corbind-viewpager]          | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-viewpager.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-viewpager)                   |
| [corbind-viewpager2]         | [![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind-viewpager2.svg)](https://mvnrepository.com/artifact/ru.ldralighieri.corbind/corbind-viewpager2)                 |


## Using in your projects

Platform bindings:
```kotlin
dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2025.09.00"))
    implementation("ru.ldralighieri.corbind:corbind")
}
```

AndroidX library bindings:
```kotlin
dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2025.09.00"))
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
    implementation("ru.ldralighieri.corbind:corbind-viewpager")
    implementation("ru.ldralighieri.corbind:corbind-viewpager2")
}
```

Google 'material' library bindings:
```kotlin
dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2025.09.00"))
    implementation("ru.ldralighieri.corbind:corbind-material")
}
```

Snapshot build:
```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies { 
    implementation(platform("ru.ldralighieri.corbind:corbind-bom:2025.10.00-SNAPSHOT"))
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
* [corbind-viewpager]  
* [corbind-viewpager2]


## How to use it?

If you need to get a text change events of EditText widget, simple use case with cold [Flow][flow] will look something like this:
```kotlin
findViewById<EditText>(R.id.etName)
    .textChanges() // Flow<CharSequence>
    .onEach { /* handle text change events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

If you prefer hot [ReceiveChannel][channel] and you need to get a ViewPager page selection events, then the use case will transform in something like this:
```kotlin
launch {
    findViewById<ViewPager>(R.id.vpSlides)
        .pageSelections(scope) // ReceiveChannel<Int>
        .consumeEach {
            /* handle ViewPager events */
        }
}
```

And if you just need to perform an action on button click, the easiest way will be:
```kotlin
launch {
    findViewById<AppCompatButton>(R.id.btConfirm)
        .clicks {
            /* perform an action on View click events */
        }
}
```

Just one more traditional example of login button enabling/disabling by email and password field validation:
```kotlin
combine(
    etEmail.textChanges() // Flow<CharSequence>
        .map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },

    etPassword.textChanges() // Flow<CharSequence>
        .map { it.length > 7 },

    transform = { email, password -> email && password }
)
    .onEach { btLogin.isEnabled = it }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in module descriptions and in source code


## Missed or forgot something?

If I forgot something or you have any ideas what can be added or corrected, please create an issue or contact me directly.


## Special thanks to

[Jake Wharton][jw]. This project is inspired by [RxBinding][rx].


## License

```
Copyright 2019-2025 Vladimir Raupov

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
[flow]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/index.html
[channel]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/index.html
[actor]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/actor.html

[kotlin-coroutine-binding]: https://medium.com/@ldralighieri/kotlin-coroutine-binding-with-flow-support-68499492a89c
[release-1.7.0]: https://medium.com/@ldralighieri/whats-up-corbind-release-1-7-0-it-s-been-a-long-road-eadf84db19c1

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
