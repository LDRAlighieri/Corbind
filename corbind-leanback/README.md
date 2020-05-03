
# corbind-leanback

To add androidx leanback bindings, import `corbind-leanback` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-leanback:1.3.2'
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

More examples in source code
