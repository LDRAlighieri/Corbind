
# corbind-navigation

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

More examples in source code
