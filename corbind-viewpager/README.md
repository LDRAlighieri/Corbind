
# corbind-viewpager

To add androidx viewpager bindings, import `corbind-viewpager` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-viewpager:1.10.0")
}
```

## List of extensions

| Component     | Extension                                                    | Description                                                                                                                              |
|---------------|--------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| **ViewPager** | [`pageScrollEvents`][ViewPager_pageScrollEvents]             | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll. |
|               | [`pageScrollStateChanges`][ViewPager_pageScrollStateChanges] | Called when the scroll state changes.                                                                                                    |
|               | [`pageSelections`][ViewPager_pageSelections]                 | Called when a new page becomes selected.                                                                                                 |

## Example

```kotlin
vpSlides.pageSelections() // Flow<Int>
    .onEach { tvMessage = "Page #$it selected" }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[ViewPager_pageScrollEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager/src/main/kotlin/ru/ldralighieri/corbind/viewpager/ViewPagerPageScrollEvents.kt
[ViewPager_pageScrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager/src/main/kotlin/ru/ldralighieri/corbind/viewpager/ViewPagerPageScrollStateChanges.kt
[ViewPager_pageSelections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-viewpager/src/main/kotlin/ru/ldralighieri/corbind/viewpager/ViewPagerPageSelections.kt
