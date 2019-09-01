---
layout: page
title: Corbind
subtitle: corbind-slidingpanelayout module
show-avatar: false
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx slidingpanelayout bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx slidingpanelayout bindings]
---

# corbind-slidingpanelayout

To add androidx slidingpanelayout bindings, import `corbind-slidingpanelayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-slidingpanelayout:1.1.0'
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
