---
layout: page
title: Corbind
subtitle: corbind-leanback module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx leanback bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx leanback bindings,leanback]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add androidx leanback bindings, import `corbind-leanback` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-leanback:1.5.4'
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
    .flowWithLifecycle(lifecycle)
    .launchIn(scope)
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-leanback
