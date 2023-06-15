---
layout: page
title: Corbind
subtitle: corbind-viewpager2 module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx viewpager2 bindings.
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

To add androidx viewpager2 bindings, import `corbind-viewpager2` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-viewpager2:1.8.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**ViewPager2** | [`pageScrollEvents`][ViewPager2_pageScrollEvents] | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll.
               | [`pageScrollStateChanges`][ViewPager2_pageScrollStateChanges] | Called when the scroll state changes.
               | [`pageSelections`][ViewPager2_pageSelections] | Called when a new page becomes selected.


## Simple examples

```kotlin
vpSlides.pageSelections() // Flow<Int>
    .onEach { tvMessage = "Page #$it selected" }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-viewpager2

[ViewPager2_pageScrollEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager2/src/main/kotlin/ru/ldralighieri/corbind/viewpager2/ViewPager2PageScrollEvents.kt
[ViewPager2_pageScrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager2/src/main/kotlin/ru/ldralighieri/corbind/viewpager2/ViewPager2PageScrollStateChanges.kt
[ViewPager2_pageSelections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager2/src/main/kotlin/ru/ldralighieri/corbind/viewpager2/ViewPager2PageSelections.kt
