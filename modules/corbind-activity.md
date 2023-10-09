---
layout: page
title: Corbind
subtitle: corbind-activity module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx leanback bindings.
tags: [android,kotlin,flow,widget,ui,material,binding,recyclerview,coroutines,kotlin-extensions,kotlin-library,android-library,fragment,viewpager,activity,drawerlayout,appcompat,kotlin-coroutines,swiperefreshlayout,android-ui-widgets]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx activity bindings, import `corbind-activity` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-activity:1.9.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**OnBackPressedDispatcher** | [`backPresses`][OnBackPressedDispatcher_backPresses] | Called when OnBackPressedDispatcher.onBackPressed triggered. OnBackPressed events only
                         | [`backProgressed`][OnBackPressedDispatcher_backProgressed] | Called when OnBackPressedDispatcher.dispatchOnBackProgressed triggered. OnBackProgressed event only
                         | [`backEvents`][OnBackPressedDispatcher_backEvents] | Called when any callback event triggered. All events


## Simple examples

```kotlin
onBackPressedDispatcher.backEvents(lifecycleOwner = this)
    .onEach { event ->
        when (event) {
            is OnBackPressed -> { /* handle back pressed event */ }
            is OnBackCanceled -> { /* handle back cancel event */ }
            is OnBackStarted -> { /* handle back started event */ }
            is OnBackProgressed -> { /* handle back progressed event */ }
        }
    }
        .flowWithLifecycle(lifecycle)
        .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-activity

[OnBackPressedDispatcher_backPresses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-activity/src/main/kotlin/ru/ldralighieri/corbind/activity/OnBackPressedDispatcherBackPresses.kt
[OnBackPressedDispatcher_backProgressed]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-activity/src/main/kotlin/ru/ldralighieri/corbind/activity/OnBackPressedDispatcherBackProgressed.kt
[OnBackPressedDispatcher_backEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-activity/src/main/kotlin/ru/ldralighieri/corbind/activity/OnBackPressedDispatcherOnBackEvents.kt
