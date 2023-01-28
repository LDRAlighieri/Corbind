---
layout: page
title: Corbind
subtitle: corbind-fragment module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx activity bindings.
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

To add androidx fragment bindings, import `corbind-fragment` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-fragment:1.7.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**FragmentManager** | `resultEvents` | Called when any results set by setFragmentResult using the same requestKey.


## Simple examples

```kotlin
lifecycleScope.launchWhenStarted {
    parentFragmentManager.resultEvents(
        requestKey = FRAGMENT_REQUEST_KEY,
        lifecycleOwner = this@CurrentFragment
    )
        .onEach { event -> /* handle result event */ }
        .launchIn(this@launchWhenStarted) // lifecycle-runtime-ktx
}
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-fragment
