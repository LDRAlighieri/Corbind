---
layout: page
title: Corbind
subtitle: corbind-navigation module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx leanback bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx navigation bindings,navigation]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx navigation bindings, import `corbind-navigation` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-navigation:1.5.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**NavController** | `destinationChanges` | Called when the `NavController` destination or its arguments change.
               | `destinationChangeEvents` | A more advanced version of the `destinationChanges`.


## Simple examples

```kotlin
navController.destinationChanges() // Flow<NavDestination>
    .onEach { destination ->
        if (destination.id == R.id.fragment_dashboard) {
            hideSoftInput()
        }
    }
    .launchIn(this)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-navigation
