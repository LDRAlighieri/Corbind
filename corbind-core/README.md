
# corbind-core

To add androidx core bindings, import `corbind-core` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-core:1.5.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**NestedScrollView** | `scrollChangeEvents` | Called when the scroll position of a view changes.


## Example

```kotlin
scrollView.scrollChangeEvents() // Flow<ViewScrollChangeEvent>
    .onEach { /* handle scroll change events */ }
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
