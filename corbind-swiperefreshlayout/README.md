
# corbind-swiperefreshlayout

To add androidx swiperefreshlayout bindings, import `corbind-swiperefreshlayout` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.11.1")
}
```

## List of extensions

| Component              | Extension                                   | Description                                                     |
|------------------------|---------------------------------------------|-----------------------------------------------------------------|
| **SwipeRefreshLayout** | [`refreshes`][SwipeRefreshLayout_refreshes] | Called when a swipe gesture triggers a refresh. open or closed. |

## Example

```kotlin
swipe.refreshes() // Flow<Unit>
    .onEach { /* handle refresh events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[SwipeRefreshLayout_refreshes]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-swiperefreshlayout/src/main/kotlin/ru/ldralighieri/corbind/swiperefreshlayout/SwipeRefreshLayoutRefreshes.kt
