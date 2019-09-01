---
layout: page
title: Corbind
subtitle: corbind-viewpager module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx viewpager bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx viewpager bindings]
---

<div style="text-align: center">
    <img src="img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx viewpager bindings, import `corbind-viewpager` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-viewpager:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**ViewPager** | `pageScrollEvents` | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll.
              | `pageScrollStateChanges` | Called when the scroll state changes.
              | `pageSelections` | Called when a new page becomes selected.


## Example

```kotlin
vp_slides.pageSelections() // Flow<Int>
    .onEach { tv_message = "Page #$it selected" }
    .launchIn(scope)
```
