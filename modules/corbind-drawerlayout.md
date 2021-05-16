---
layout: page
title: Corbind
subtitle: corbind-drawerlayout module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx drawerlayout bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx drawerlayout bindings,drawerlayout]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx drawerlayout bindings, import `corbind-drawerlayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-drawerlayout:1.5.1'
```

## List of extensions

Component | Extension | Description
--|---|--
**DrawerLayout** | `drawerOpens` | Called when a drawer has settled in a completely open or close state.


## Example

```kotlin
drawer.drawerOpens() // Flow<Boolean>
    .onEach { isOpen ->
      tv_message = "Drawer completely ${ if (isOpen) "open" else "close"}"
    }
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-drawerlayout
