---
layout: page
title: Corbind
subtitle: corbind-swiperefreshlayout module
show-avatar: false
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx swiperefreshlayout bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx swiperefreshlayout bindings]
---

# corbind-swiperefreshlayout

To add androidx swiperefreshlayout bindings, import `corbind-swiperefreshlayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**SwipeRefreshLayout** | `refreshes` | Called when a swipe gesture triggers a refresh. open or closed.


## Example

```kotlin
swipe.refreshes() // Flow<Unit>
    .onEach { /* handle refresh events */ }
    .launchIn(this)
```
