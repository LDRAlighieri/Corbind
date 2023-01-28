---
layout: page
title: Corbind
subtitle: corbind-core module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx core bindings.
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

To add androidx core bindings, import `corbind-core` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-core:1.7.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**NestedScrollView** | `scrollChangeEvents` | Called when the scroll position of a view changes.


## Example

```kotlin
scrollView.scrollChangeEvents() // Flow<ViewScrollChangeEvent>
    .onEach { /* handle scroll change events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-core
