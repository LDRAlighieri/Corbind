
# corbind-lifecycle

To add androidx lifecycle bindings, import `corbind-lifecycle` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-lifecycle:1.12.1")
}
```

## List of extensions

| Component     | Extension                    | Description                             |
|---------------|------------------------------|-----------------------------------------|
| **Lifecycle** | [`events`][Lifecycle_events] | Called when any lifecycle event change. |

## Simple examples

```kotlin
lifecycle.events() // Flow<Lifecycle.Event>
    .filter { it == Lifecycle.Event.ON_RESUME }
    .onEach { /* handle lifecycle onResume event */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[Lifecycle_events]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-lifecycle/src/main/kotlin/ru/ldralighieri/corbind/lifecycle/LifecycleEvents.kt
