
# corbind-appcompat

To add androidx appcompat bindings, import `corbind-appcompat` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-appcompat:1.12.1")
}
```

## List of extensions

| Component          | Extension                                                   | Description                                                                             |
|--------------------|-------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| **ActionMenuView** | [`itemClicks`][ActionMenuView_itemClicks]                   | Called when a menu item is clicked if the item itself did not already handle the event. |
| **PopupMenu**      | [`dismisses`][PopupMenu_dismisses]                          | Called when the associated menu has been dismissed.                                     |
|                    | [`itemClicks`][PopupMenu_itemClicks]                        | Called when a menu item is clicked if the item itself did not already handle the event. |
| **SearchView**     | [`queryTextChanges`][SearchView_queryTextChanges]           | Called when the query text is changed by the user.                                      |
|                    | [`queryTextChangeEvents`][SearchView_queryTextChangeEvents] | A more advanced version of the `queryTextChanges`.                                      |
| **Toolbar**        | [`itemClicks`][Toolbar_itemClicks]                          | Called when a menu item is clicked if the item itself did not already handle the event. |
|                    | [`navigationClicks`][Toolbar_navigationClicks]              | Called whenever the user clicks the navigation button at the start of the toolbar.      |

## Example

```kotlin
toolbar.itemClicks() // Flow<MenuItem>
    .onEach { /* handle menu item clicks events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[ActionMenuView_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/ActionMenuViewItemClicks.kt
[PopupMenu_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/PopupMenuDismisses.kt
[PopupMenu_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/PopupMenuItemClicks.kt
[SearchView_queryTextChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/SearchViewQueryTextChanges.kt
[SearchView_queryTextChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/SearchViewQueryTextChangeEvents.kt
[Toolbar_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/ToolbarItemClicks.kt
[Toolbar_navigationClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-appcompat/src/main/kotlin/ru/ldralighieri/corbind/appcompat/ToolbarNavigationClicks.kt
