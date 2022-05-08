---
layout: page
title: Corbind
subtitle: corbind-appcompat module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx appcompat bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx appcompat bindings,appcompat]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx appcompat bindings, import `corbind-appcompat` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-appcompat:1.5.4'
```

## List of extensions

Component | Extension | Description
--|---|--
**ActionMenuView** | `itemClicks` | Called when a menu item is clicked if the item itself did not already handle the event.
**PopupMenu** | `dismisses` | Called when the associated menu has been dismissed.
              | `itemClicks` | Called when a menu item is clicked if the item itself did not already handle the event.
**SearchView** | `queryTextChanges` | Called when the query text is changed by the user.
               | `queryTextChangeEvents` | A more advanced version of the `queryTextChanges`.
**Toolbar** | `itemClicks` | Called when a menu item is clicked if the item itself did not already handle the event.
            | `navigationClicks` | Called whenever the user clicks the navigation button at the start of the toolbar.


## Example

```kotlin
toolbar.itemClicks() // Flow<MenuItem>
    .onEach { /* handle menu item clicks events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-appcompat
