---
layout: page
title: Corbind
subtitle: corbind-core module
show-avatar: false
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx core bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx core bindings]
---

# corbind-core

To add androidx core bindings, import `corbind-core` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-core:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**NestedScrollView** | `scrollChangeEvents` | Called when the scroll position of a view changes.


## Example

```kotlin
scrollView.scrollChangeEvents() // Flow<ViewScrollChangeEvent>
    .onEach { /* handle scroll change events */ }
    .launchIn(scope)
```
