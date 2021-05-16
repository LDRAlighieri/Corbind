---
layout: page
title: Corbind
subtitle: corbind-activity module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx leanback bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx activity bindings,activity]
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

```groovy
implementation 'ru.ldralighieri.corbind:corbind-activity:1.5.1'
```

## List of extensions

Component | Extension | Description
--|---|--
**OnBackPressedDispatcher** | `backPresses` | Called when OnBackPressedDispatcher.onBackPressed triggered.


## Simple examples

```kotlin
requireActivity().onBackPressedDispatcher.backPresses()
    .onEach { /* handle onBackPressed event */ }
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-activity
