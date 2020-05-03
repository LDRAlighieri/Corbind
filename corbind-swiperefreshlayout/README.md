
# corbind-swiperefreshlayout

To add androidx swiperefreshlayout bindings, import `corbind-swiperefreshlayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.3.2'
```

## List of extensions

Component | Extension | Description
--|---|--
**SwipeRefreshLayout** | `refreshes` | Called when a swipe gesture triggers a refresh. open or closed.


## Example

```kotlin
swipe.refreshes() // Flow<Unit>
    .onEach { /* handle refresh events */ }
    .launchIn(this)
```

More examples in source code
