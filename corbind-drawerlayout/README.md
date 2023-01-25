
# corbind-drawerlayout

To add androidx drawerlayout bindings, import `corbind-drawerlayout` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-drawerlayout:1.7.0")
}
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
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
