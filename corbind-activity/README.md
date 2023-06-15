
# corbind-activity

To add androidx activity bindings, import `corbind-activity` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-activity:1.8.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**OnBackPressedDispatcher** | [`backPresses`][OnBackPressedDispatcher_backPresses] | Called when OnBackPressedDispatcher.onBackPressed triggered.


## Simple examples

```kotlin
onBackPressedDispatcher.backPresses(lifecycleOwner = this) // Flow<Unit>
    .onEach { /* handle onBackPressed event */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[OnBackPressedDispatcher_backPresses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-activity/src/main/kotlin/ru/ldralighieri/corbind/activity/OnBackPressedDispatcherBackPresses.kt
