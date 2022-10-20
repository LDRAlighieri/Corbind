
# corbind-activity

To add androidx activity bindings, import `corbind-activity` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-activity:1.6.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**OnBackPressedDispatcher** | `backPresses` | Called when OnBackPressedDispatcher.onBackPressed triggered.


## Simple examples

```kotlin
onBackPressedDispatcher.backPresses(lifecycleOwner = this)
    .onEach { /* handle onBackPressed event */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
