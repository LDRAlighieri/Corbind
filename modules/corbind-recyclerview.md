---
layout: page
title: Corbind
subtitle: corbind-recyclerview module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx recyclerview bindings.
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

To add androidx recyclerview bindings, import `corbind-recyclerview` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-recyclerview:1.10.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**RecyclerView.Adapter** | [`dataChanges`][RecyclerView_Adapte_dataChanges] | Called when the RecyclerView's adapter data has been changed
**RecyclerView** | [`childAttachStateChangeEvents`][RecyclerView_childAttachStateChangeEvents] | Called when a view is attached to or detached from the RecyclerView.
                  | [`flingEvents`][RecyclerView_flingEvents] | Handle a fling given the velocities in both x and y directions
                  | [`scrollEvents`][RecyclerView_scrollEvents] | Called when a scrolling event has occurred on that RecyclerView.
                  | [`scrollStateChanges`][RecyclerView_scrollStateChanges] | Called when RecyclerView's scroll state changes.


## Example

```kotlin
rv.scrollStateChanges() // Flow<Int>
    .onEach { /* handle RecyclerView scroll state change events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-recyclerview

[RecyclerView_Adapte_dataChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerAdapterDataChanges.kt
[RecyclerView_childAttachStateChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewChildAttachStateChangeEvents.kt
[RecyclerView_flingEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewFlingEvents.kt
[RecyclerView_scrollEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewScrollEvents.kt
[RecyclerView_scrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewScrollStateChanges.kt
