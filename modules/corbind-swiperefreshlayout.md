---
layout: page
title: Corbind
subtitle: corbind-swiperefreshlayout module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx swiperefreshlayout bindings.
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

To add androidx swiperefreshlayout bindings, import `corbind-swiperefreshlayout` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.7.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**SwipeRefreshLayout** | `refreshes` | Called when a swipe gesture triggers a refresh. open or closed.


## Example

```kotlin
swipe.refreshes() // Flow<Unit>
    .onEach { /* handle refresh events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(this)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-swiperefreshlayout
