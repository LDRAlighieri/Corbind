
# corbind-activity

To add androidx activity bindings, import `corbind-activity` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-activity:1.5.2'
```

## List of extensions

Component | Extension | Description
--|---|--
**OnBackPressedDispatcher** | `backPresses` | Called when OnBackPressedDispatcher.onBackPressed triggered.


## Simple examples

```kotlin
requireActivity().onBackPressedDispatcher.backPresses()
    .onEach { /* handle onBackPressed event */ }
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
