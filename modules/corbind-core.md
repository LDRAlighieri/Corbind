---
layout: page
title: Corbind
subtitle: corbind-core module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx core bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx core bindings]
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

```groovy
implementation 'ru.ldralighieri.corbind:corbind-core:1.5.1'
```

## List of extensions

Component | Extension | Description
--|---|--
**NestedScrollView** | `scrollChangeEvents` | Called when the scroll position of a view changes.


## Example

```kotlin
scrollView.scrollChangeEvents() // Flow<ViewScrollChangeEvent>
    .onEach { /* handle scroll change events */ }
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-core
