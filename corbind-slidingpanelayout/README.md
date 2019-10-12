
# corbind-slidingpanelayout

To add androidx slidingpanelayout bindings, import `corbind-slidingpanelayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-slidingpanelayout:1.2.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**SlidingPaneLayoutr** | `panelOpens` | Called when a sliding pane becomes slid completely open or closed.
                       | `panelSlides` | Called when a sliding pane's position changes.


## Example

```kotlin
slider.panelOpens() // Flow<Boolean>
    .onEach { isOpen ->
      tv_message = "Panel completely ${ if (isOpen) "open" else "close"}"
    }
    .launchIn(scope)
```

More examples in source code
