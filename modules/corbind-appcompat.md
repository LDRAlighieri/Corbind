---
layout: page
title: Corbind
subtitle: corbind-appcompat module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx appcompat bindings.
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

To add androidx appcompat bindings, import `corbind-appcompat` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-appcompat:1.8.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**ActionMenuView** | [`itemClicks`][ActionMenuView_itemClicks] | Called when a menu item is clicked if the item itself did not already handle the event.
**PopupMenu** | [`dismisses`][PopupMenu_dismisses] | Called when the associated menu has been dismissed.
              | [`itemClicks`][PopupMenu_itemClicks] | Called when a menu item is clicked if the item itself did not already handle the event.
**SearchView** | [`queryTextChanges`][SearchView_queryTextChanges] | Called when the query text is changed by the user.
               | [`queryTextChangeEvents`][SearchView_queryTextChangeEvents] | A more advanced version of the `queryTextChanges`.
**Toolbar** | [`itemClicks`][Toolbar_itemClicks] | Called when a menu item is clicked if the item itself did not already handle the event.
            | [`navigationClicks`][Toolbar_navigationClicks] | Called whenever the user clicks the navigation button at the start of the toolbar.


## Example

```kotlin
toolbar.itemClicks() // Flow<MenuItem>
    .onEach { /* handle menu item clicks events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-appcompat

[ActionMenuView_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/ActionMenuViewItemClicks.kt
[PopupMenu_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/PopupMenuDismisses.kt
[PopupMenu_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/PopupMenuItemClicks.kt
[SearchView_queryTextChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/SearchViewQueryTextChanges.kt
[SearchView_queryTextChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/SearchViewQueryTextChangeEvents.kt
[Toolbar_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/ToolbarItemClicks.kt
[Toolbar_navigationClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/ToolbarNavigationClicks.kt
