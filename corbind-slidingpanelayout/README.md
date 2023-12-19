
# corbind-slidingpanelayout

To add androidx slidingpanelayout bindings, import `corbind-slidingpanelayout` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-slidingpanelayout:1.10.0")
}
```

## List of extensions

| Component             | Extension                                      | Description                                                        |
|-----------------------|------------------------------------------------|--------------------------------------------------------------------|
| **SlidingPaneLayout** | [`panelOpens`][SlidingPaneLayout_panelOpens]   | Called when a sliding pane becomes slid completely open or closed. |
|                       | [`panelSlides`][SlidingPaneLayout_panelSlides] | Called when a sliding pane's position changes.                     |

## Example

```kotlin
slider.panelOpens() // Flow<Boolean>
    .onEach { isOpen ->
      tvMessage = "Panel completely ${ if (isOpen) "open" else "close"}"
    }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[SlidingPaneLayout_panelOpens]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-slidingpanelayout/src/main/kotlin/ru/ldralighieri/corbind/slidingpanelayout/SlidingPaneLayoutPaneOpens.kt
[SlidingPaneLayout_panelSlides]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-slidingpanelayout/src/main/kotlin/ru/ldralighieri/corbind/slidingpanelayout/SlidingPaneLayoutSlides.kt
