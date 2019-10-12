
# corbind-viewpager

To add androidx viewpager bindings, import `corbind-viewpager` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-viewpager:1.2.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**ViewPager** | `pageScrollEvents` | Called when the current page is scrolled, either as part of a programmatically initiated smooth scroll or a user initiated touch scroll.
              | `pageScrollStateChanges` | Called when the scroll state changes.
              | `pageSelections` | Called when a new page becomes selected.


## Example

```kotlin
vp_slides.pageSelections() // Flow<Int>
    .onEach { tv_message = "Page #$it selected" }
    .launchIn(scope)
```

More examples in source code
