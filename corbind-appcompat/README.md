
# corbind-appcompat

To add androidx appcompat bindings, import `corbind-appcompat` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-appcompat:1.6.0")
}
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
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
