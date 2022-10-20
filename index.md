---
layout: page
title: Corbind
subtitle: Coroutines binding APIs for Android UI widgets from the platform and support libraries.
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,receivechannel,actor,flow,data binding]
---

<div style="text-align: center">
    <img src="img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

[![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind.svg)](https://search.maven.org/search?q=g:ru.ldralighieri.corbind)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-v1.7.20-blue.svg)](https://kotlinlang.org)
[![Kotlin Coroutines Version](https://img.shields.io/badge/Coroutines-v1.6.4-blue.svg)](https://kotlinlang.org/docs/reference/coroutines-overview.html)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a1c9a1b1d1ce4ca7a201ab93492bf6e0)](https://app.codacy.com/gh/LDRAlighieri/Corbind)
[![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)

[![Google Dev Library](https://img.shields.io/badge/Featured%20in%20devlibrary.withgoogle.com-Corbind-blue)](https://devlibrary.withgoogle.com/products/android/repos/LDRAlighieri-Corbind)
[![Android Weekly](https://androidweekly.net/issues/issue-377/badge)](https://androidweekly.net/issues/issue-377)


Kotlin Coroutines binding APIs for Android UI widgets from the platform and support libraries. **Supports Flow, ReceiveChannel and Actor**.


## Description

This library is for Android applications only. Help you to transform Android UI events into cold [Flow][flow], hot [ReceiveChannel][channel] or just perform an action through an [Actor][actor].


## Download

Platform bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind:1.6.0'
```

AndroidX library bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind-activity:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-appcompat:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-core:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-drawerlayout:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-fragment:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-leanback:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-lifecycle:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-navigation:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-recyclerview:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-slidingpanelayout:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-viewpager:1.6.0'
implementation 'ru.ldralighieri.corbind:corbind-viewpager2:1.6.0'
```

Google 'material' library bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind-material:1.6.0'
```

Snapshot build:
```groovy
repositories {
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}

dependencies {
   implementation 'ru.ldralighieri.corbind:{module}:1.6.1-SNAPSHOT'
}
```


## List of extensions
You can find a list of extensions in the description of each module:<br>
[corbind]<br>
[corbind-activity]<br>
[corbind-appcompat]<br>
[corbind-core]<br>
[corbind-drawerlayout]<br>
[corbind-fragment]<br>
[corbind-leanback]<br>
[corbind-lifecycle]<br>
[corbind-material]<br>
[corbind-navigation]<br>
[corbind-recyclerview]<br>
[corbind-slidingpanelayout]<br>
[corbind-swiperefreshlayout]<br>
[corbind-viewpager]<br>
[corbind-viewpager2]


## How to use it?

If you need to get a text change events of EditText widget, simple use case with cold [Flow][flow] will look something like this:
```kotlin
findViewById<EditText>(R.id.et_name)
    .textChanges() // Flow<CharSequence>
    .onEach { /* handle text change events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

If you prefer hot [ReceiveChannel][channel] and you need to get a ViewPager page selection events, then the use case will transform in something like this:
```kotlin
launch {
    findViewById<ViewPager>(R.id.vp_slides)
        .pageSelections(scope) // ReceiveChannel<Int>
        .consumeEach {
            /* handle ViewPager events */
        }
}
```

And if you just need to perform an action on button click, the easiest way will be:
```kotlin
launch {
    findViewById<AppCompatButton>(R.id.bt_confirm)
        .clicks {
            /* perform an action on View click events */
        }
}
```

Just one more traditional example of login button enabling/disabling by email and password field validation:
```kotlin
combine(
    et_email.textChanges()
        .map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },

    et_password.textChanges()
        .map { it.length > 7 },

    transform = { email, password -> email && password }
)
    .onEach { bt_login.isEnabled = it }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in module descriptions and in [source code][source].


## Missed or forgot something?

If I forgot something or you have any ideas what can be added or corrected, please create an issue or contact me directly.


## Special thanks to

[Jake Wharton][jw]. This project is inspired by [RxBinding][rx].


[jw]: https://github.com/JakeWharton
[rx]: https://github.com/JakeWharton/RxBinding
[flow]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/index.html
[channel]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/index.html
[actor]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/actor.html

[source]: https://github.com/LDRAlighieri/Corbind

[corbind]: https://ldralighieri.github.io/Corbind/modules/corbind/
[corbind-activity]: https://ldralighieri.github.io/Corbind/modules/corbind-activity
[corbind-appcompat]: https://ldralighieri.github.io/Corbind/modules/corbind-appcompat
[corbind-core]: https://ldralighieri.github.io/Corbind/modules/corbind-core
[corbind-drawerlayout]: https://ldralighieri.github.io/Corbind/modules/corbind-drawerlayout
[corbind-fragment]: https://ldralighieri.github.io/Corbind/modules/corbind-fragment
[corbind-leanback]: https://ldralighieri.github.io/Corbind/modules/corbind-leanback
[corbind-lifecycle]: https://ldralighieri.github.io/Corbind/modules/corbind-lifecycle
[corbind-material]: https://ldralighieri.github.io/Corbind/modules/corbind-material
[corbind-navigation]: https://ldralighieri.github.io/Corbind/modules/corbind-navigation
[corbind-recyclerview]: https://ldralighieri.github.io/Corbind/modules/corbind-recyclerview
[corbind-slidingpanelayout]: https://ldralighieri.github.io/Corbind/modules/corbind-slidingpanelayout
[corbind-swiperefreshlayout]: https://ldralighieri.github.io/Corbind/modules/corbind-swiperefreshlayout
[corbind-viewpager]: https://ldralighieri.github.io/Corbind/modules/corbind-viewpager
[corbind-viewpager2]: https://ldralighieri.github.io/Corbind/modules/corbind-viewpager2
