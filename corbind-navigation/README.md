
# corbind-navigation

To add androidx navigation bindings, import `corbind-navigation` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-navigation:1.11.1")
}
```

## List of extensions

| Component         | Extension                                                          | Description                                                          |
|-------------------|--------------------------------------------------------------------|----------------------------------------------------------------------|
| **NavController** | [`destinationChanges`][NavController_destinationChanges]           | Called when the `NavController` destination or its arguments change. |
|                   | [`destinationChangeEvents`][NavController_destinationChangeEvents] | A more advanced version of the `destinationChanges`.                 |

## Simple examples

```kotlin
navController.destinationChanges() // Flow<NavDestination>
    .onEach { destination ->
        if (destination.id == R.id.fragment_dashboard) {
            hideSoftInput()
        }
    }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[NavController_destinationChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-navigation/src/main/kotlin/ru/ldralighieri/corbind/navigation/NavControllerOnDestinationChanges.kt
[NavController_destinationChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-navigation/src/main/kotlin/ru/ldralighieri/corbind/navigation/NavControllerOnDestinationChangeEvents.kt
