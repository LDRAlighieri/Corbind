---
layout: page
title: Corbind
subtitle: corbind-drawerlayout module
show-avatar: false
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx drawerlayout bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx drawerlayout bindings]
---

# corbind-drawerlayout

To add androidx drawerlayout bindings, import `corbind-drawerlayout` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-drawerlayout:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**DrawerLayout** | `drawerOpens` | Called when a drawer has settled in a completely open or close state.


## Example

```kotlin
drawer.drawerOpens() // Flow<Boolean>
    .onEach { isOpen ->
      tv_message = "Drawer completely ${ if (isOpen) "open" else "close"}"
    }
    .launchIn(scope)
```
