---
layout: page
title: Corbind
subtitle: Coroutines binding APIs for Android UI widgets from the platform and support libraries.
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,receivechannel,flow,data binding]
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
[![Kotlin Version](https://img.shields.io/badge/Kotlin-v1.3.50-blue.svg)](https://kotlinlang.org)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
![GitHub stars](https://img.shields.io/github/stars/LDRAlighieri/Corbind?style=social)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a1c9a1b1d1ce4ca7a201ab93492bf6e0)](https://www.codacy.com/app/LDRAlighieri/Corbind?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=LDRAlighieri/Corbind&amp;utm_campaign=Badge_Grade)
[![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)


Kotlin Coroutines binding APIs for Android UI widgets from the platform and support libraries. **Supports Flow, ReceiveChannel and Actor**.


## Description

This library is for Android applications only. Help you to transform Android UI events into cold [Flow][flow], hot [ReceiveChannel][channel] or just perform an action through an [Actor][actor].


## Download

Platform bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind:1.1.0'
```

AndroidX library bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind-appcompat:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-core:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-drawerlayout:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-leanback:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-recyclerview:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-slidingpanelayout:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-viewpager:1.1.0'
implementation 'ru.ldralighieri.corbind:corbind-viewpager2:1.1.0'
```

Google 'material' library bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind-material:1.1.0'
```
<br>


## List of extensions
You can find a list of extensions in the description of each module.<br>
[corbind]<br>
[corbind-appcompat]<br>
[corbind-core]<br>
[corbind-drawerlayout]<br>
[corbind-leanback]<br>
[corbind-material]<br>
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
    .launchIn(scope)
```
<br>


## Missed or forgot something?

If I forgot something or you have any ideas what can be added or corrected, please create an issue or contact me directly.


## Special thanks to

[Jake Wharton][jw]. This project is inspired by [RxBinding][rx].


[jw]: https://github.com/JakeWharton
[rx]: https://github.com/JakeWharton/RxBinding
[flow]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/index.html
[channel]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/index.html
[actor]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/actor.html

[corbind]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind
[corbind-appcompat]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-appcompat
[corbind-core]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-core
[corbind-drawerlayout]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-drawerlayout
[corbind-leanback]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-leanback
[corbind-material]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-material
[corbind-recyclerview]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-recyclerview
[corbind-slidingpanelayout]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-slidingpanelayout
[corbind-swiperefreshlayout]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-swiperefreshlayout
[corbind-viewpager]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-viewpager
[corbind-viewpager2]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-viewpager2
