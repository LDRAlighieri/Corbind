---
layout: page
title: Corbind
subtitle: corbind-slidingpanelayout module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx slidingpanelayout bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx slidingpanelayout bindings]
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

```groovy
implementation 'ru.ldralighieri.corbind:corbind-slidingpanelayout:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**SlidingPaneLayoutr** | `panelOpens` | Called when a sliding pane becomes slid completely open or closed.
                       | `panelSlides` | Called when a sliding pane's position changes.


## Example

```kotlin
slider.panelOpens() // Flow<Boolean>
    .onEach { isOpen ->
      tv_message = "Panel completely ${ if (isOpen) "open" else "close"}"
    }
    .launchIn(scope)
```
