
# corbind-leanback

To add androidx leanback bindings, import `corbind-leanback` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-leanback:1.10.0")
}
```

## List of extensions

| Component          | Extension                                                      | Description                                               |
|--------------------|----------------------------------------------------------------|-----------------------------------------------------------|
| **SearchBar**      | [`searchQueryChanges`][SearchBar_searchQueryChanges]           | Called when the search bar detects a change in the query. |
|                    | [`searchQueryChangeEvents`][SearchBar_searchQueryChangeEvents] | A more advanced version of the `searchQueryChanges`.      |
| **SearchEditText** | [`keyboardDismisses`][SearchEditText_keyboardDismisses]        | Called when the keyboard is dismissed.                    |

## Example

```kotlin
search.searchQueryChanges() // Flow<String>
    .map { it.toLowerCase(Locale.getDefault()) }
    .onEach { query -> filter.updateItems(query) }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[SearchBar_searchQueryChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-leanback/src/main/kotlin/ru/ldralighieri/corbind/leanback/SearchBarSearchQueryChanges.kt
[SearchBar_searchQueryChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-leanback/src/main/kotlin/ru/ldralighieri/corbind/leanback/SearchBarSearchQueryChangeEvents.kt
[SearchEditText_keyboardDismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-leanback/src/main/kotlin/ru/ldralighieri/corbind/leanback/SearchEditTextKeyboardDismisses.kt
