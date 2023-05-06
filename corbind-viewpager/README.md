
# corbind-viewpager

To add androidx viewpager bindings, import `corbind-viewpager` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-viewpager:1.8.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**ViewPager** | `pageScrollEvents` | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll.
              | `pageScrollStateChanges` | Called when the scroll state changes.
              | `pageSelections` | Called when a new page becomes selected.


## Example

```kotlin
vpSlides.pageSelections() // Flow<Int>
    .onEach { tvMessage = "Page #$it selected" }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
