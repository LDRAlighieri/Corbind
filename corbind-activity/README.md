
# corbind-activity

To add androidx activity bindings, import `corbind-activity` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-activity:1.9.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**OnBackPressedDispatcher** | [`backPresses`][OnBackPressedDispatcher_backPresses] | Called when OnBackPressedDispatcher.onBackPressed triggered. OnBackPressed events only 
                         | [`backProgressed`][OnBackPressedDispatcher_backProgressed] | Called when OnBackPressedDispatcher.dispatchOnBackProgressed triggered. OnBackProgressed event only
                         | [`backEvents`][OnBackPressedDispatcher_backEvents] | Called when any callback event triggered. All events


## Simple examples

```kotlin
onBackPressedDispatcher.backEvents(lifecycleOwner = this)
    .onEach { event ->
        when (event) {
            is OnBackPressed -> { /* handle back pressed event */ }
            is OnBackCanceled -> { /* handle back cancel event */ }
            is OnBackStarted -> { /* handle back started event */ }
            is OnBackProgressed -> { /* handle back progressed event */ }
        }
    }
        .flowWithLifecycle(lifecycle)
        .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[OnBackPressedDispatcher_backPresses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-activity/src/main/kotlin/ru/ldralighieri/corbind/activity/OnBackPressedDispatcherBackPresses.kt
[OnBackPressedDispatcher_backProgressed]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-activity/src/main/kotlin/ru/ldralighieri/corbind/activity/OnBackPressedDispatcherBackProgressed.kt
[OnBackPressedDispatcher_backEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-activity/src/main/kotlin/ru/ldralighieri/corbind/activity/OnBackPressedDispatcherOnBackEvents.kt
