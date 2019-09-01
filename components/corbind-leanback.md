---
layout: page
title: Corbind
subtitle: corbind-leanback module
show-avatar: false
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx leanback bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx leanback bindings]
---

# corbind-leanback

To add androidx leanback bindings, import `corbind-leanback` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-leanback:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**SearchBar** | `searchQueryChanges` | Called when the search bar detects a change in the query.
              | `searchQueryChangeEvents` | A more advanced version of the `searchQueryChanges`.
**SearchEditText** | `keyboardDismisses` | Called when the keyboard is dismissed.


## Example

```kotlin
search.searchQueryChanges() // Flow<String>
    .map { it.toLowerCase(Locale.getDefault()) }
    .onEach { query -> filter.updateItems(query) }
    .launchIn(scope)
```
