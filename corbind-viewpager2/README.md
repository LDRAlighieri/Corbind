﻿
# corbind-viewpager2

To add androidx viewpager2 bindings, import `corbind-viewpager2` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-viewpager2:1.6.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**ViewPager2** | `pageScrollEvents` | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll.
               | `pageScrollStateChanges` | Called when the scroll state changes.
               | `pageSelections` | Called when a new page becomes selected.


## Simple examples

```kotlin
vp_slides.pageSelections() // Flow<Int>
    .onEach { tv_message = "Page #$it selected" }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
