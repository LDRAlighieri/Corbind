---
layout: page
title: Corbind
subtitle: corbind-slidingpanelayout module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx slidingpanelayout bindings.
tags: [slidingpanelayout,android,kotlin,flow,widget,ui,material,binding,recyclerview,coroutines,kotlin-extensions,kotlin-library,android-library,fragment,viewpager,activity,drawerlayout,appcompat,kotlin-coroutines,swiperefreshlayout,android-ui-widgets]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx slidingpanelayout bindings, import `corbind-slidingpanelayout` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-slidingpanelayout:1.8.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**SlidingPaneLayout** | [`panelOpens`][SlidingPaneLayout_panelOpens] | Called when a sliding pane becomes slid completely open or closed.
                       | [`panelSlides`][SlidingPaneLayout_panelSlides] | Called when a sliding pane's position changes.


## Example

```kotlin
slider.panelOpens() // Flow<Boolean>
    .onEach { isOpen ->
      tv_message = "Panel completely ${ if (isOpen) "open" else "close"}"
    }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-slidingpanelayout

[SlidingPaneLayout_panelOpens]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-slidingpanelayout/src/main/kotlin/ru/ldralighieri/corbind/slidingpanelayout/SlidingPaneLayoutPaneOpens.kt
[SlidingPaneLayout_panelSlides]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-slidingpanelayout/src/main/kotlin/ru/ldralighieri/corbind/slidingpanelayout/SlidingPaneLayoutSlides.kt
