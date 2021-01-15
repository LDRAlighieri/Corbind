
# corbind-swiperefreshlayout

To add androidx swiperefreshlayout bindings, import `corbind-swiperefreshlayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.4.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**SwipeRefreshLayout** | `refreshes` | Called when a swipe gesture triggers a refresh. open or closed.


## Example

```kotlin
swipe.refreshes() // Flow<Unit>
    .onEach { /* handle refresh events */ }
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
