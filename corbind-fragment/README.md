
# corbind-fragment

To add androidx fragment bindings, import `corbind-fragment` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-fragment:1.10.0")
}
```

## List of extensions

| Component           | Extension                                      | Description                                                                 |
|---------------------|------------------------------------------------|-----------------------------------------------------------------------------|
| **FragmentManager** | [`resultEvents`][FragmentManager_resultEvents] | Called when any results set by setFragmentResult using the same requestKey. |

## Simple examples

```kotlin
lifecycleScope.launchWhenStarted {
    parentFragmentManager.resultEvents(
        requestKey = FRAGMENT_REQUEST_KEY,
        lifecycleOwner = this@CurrentFragment
    ) // Flow<FragmentResultEvent>
        .onEach { event -> /* handle result event */ }
        .launchIn(this@launchWhenStarted) // lifecycle-runtime-ktx
}
```

More examples in source code

[FragmentManager_resultEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-fragment/src/main/kotlin/ru/ldralighieri/corbind/fragment/FragmentManagerResultEvents.kt
