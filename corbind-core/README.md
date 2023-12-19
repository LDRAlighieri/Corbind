
# corbind-core

To add androidx core bindings, import `corbind-core` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-core:1.10.0")
}
```

## List of extensions

| Component            | Extension                                                   | Description                                        |
|----------------------|-------------------------------------------------------------|----------------------------------------------------|
| **NestedScrollView** | [`scrollChangeEvents`][NestedScrollView_scrollChangeEvents] | Called when the scroll position of a view changes. |

## Example

```kotlin
scrollView.scrollChangeEvents() // Flow<ViewScrollChangeEvent>
    .onEach { /* handle scroll change events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[NestedScrollView_scrollChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-core/src/main/kotlin/ru/ldralighieri/corbind/core/NestedScrollViewScrollChangeEvents.kt
