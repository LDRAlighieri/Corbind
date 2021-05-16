
# corbind-navigation

To add androidx navigation bindings, import `corbind-navigation` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-navigation:1.5.1'
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
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
