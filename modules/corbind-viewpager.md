---
layout: page
title: Corbind
subtitle: corbind-viewpager module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx viewpager bindings.
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

To add androidx viewpager bindings, import `corbind-viewpager` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-viewpager:1.9.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**ViewPager** | [`pageScrollEvents`][ViewPager_pageScrollEvents] | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll.
              | [`pageScrollStateChanges`][ViewPager_pageScrollStateChanges] | Called when the scroll state changes.
              | [`pageSelections`][ViewPager_pageSelections] | Called when a new page becomes selected.


## Example

```kotlin
vpSlides.pageSelections() // Flow<Int>
    .onEach { tvMessage = "Page #$it selected" }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-viewpager

[ViewPager_pageScrollEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager/src/main/kotlin/ru/ldralighieri/corbind/viewpager/ViewPagerPageScrollEvents.kt
[ViewPager_pageScrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager/src/main/kotlin/ru/ldralighieri/corbind/viewpager/ViewPagerPageScrollStateChanges.kt
[ViewPager_pageSelections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager/src/main/kotlin/ru/ldralighieri/corbind/viewpager/ViewPagerPageSelections.kt
