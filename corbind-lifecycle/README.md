
# corbind-lifecycle

To add androidx lifecycle bindings, import `corbind-lifecycle` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-lifecycle:1.5.3'
```

## List of extensions

Component | Extension | Description
--|---|--
**Lifecycle** | `events` | Called when any lifecycle event change.


## Simple examples

```kotlin
lifecycle.events()
    .filter { it == Lifecycle.Event.ON_RESUME }
    .onEach { /* handle lifecycle onResume event */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
