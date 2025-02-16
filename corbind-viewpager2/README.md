
# corbind-viewpager2

To add androidx viewpager2 bindings, import `corbind-viewpager2` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-viewpager2:1.11.1")
}
```

## List of extensions

| Component      | Extension                                                     | Description                                                                                                                              |
|----------------|---------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| **ViewPager2** | [`pageScrollEvents`][ViewPager2_pageScrollEvents]             | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll. |
|                | [`pageScrollStateChanges`][ViewPager2_pageScrollStateChanges] | Called when the scroll state changes.                                                                                                    |
|                | [`pageSelections`][ViewPager2_pageSelections]                 | Called when a new page becomes selected.                                                                                                 |

## Simple examples

```kotlin
vpSlides.pageSelections() // Flow<Int>
    .onEach { tvMessage = "Page #$it selected" }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[ViewPager2_pageScrollEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager2/src/main/kotlin/ru/ldralighieri/corbind/viewpager2/ViewPager2PageScrollEvents.kt
[ViewPager2_pageScrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager2/src/main/kotlin/ru/ldralighieri/corbind/viewpager2/ViewPager2PageScrollStateChanges.kt
[ViewPager2_pageSelections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager2/src/main/kotlin/ru/ldralighieri/corbind/viewpager2/ViewPager2PageSelections.kt
