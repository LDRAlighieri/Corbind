---
layout: page
title: Corbind
subtitle: corbind-lifecycle module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx leanback bindings.
tags: [lifecycle,android,kotlin,flow,widget,ui,material,binding,recyclerview,coroutines,kotlin-extensions,kotlin-library,android-library,fragment,viewpager,activity,drawerlayout,appcompat,kotlin-coroutines,swiperefreshlayout,android-ui-widgets]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx lifecycle bindings, import `corbind-lifecycle` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-lifecycle:1.8.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**Lifecycle** | [`events`][Lifecycle_events] | Called when any lifecycle event change.


## Simple examples

```kotlin
lifecycle.events() // Flow<Lifecycle.Event>
    .filter { it == Lifecycle.Event.ON_RESUME }
    .onEach { /* handle lifecycle onResume event */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-lifecycle

[Lifecycle_events]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-lifecycle/src/main/kotlin/ru/ldralighieri/corbind/lifecycle/LifecycleEvents.kt
