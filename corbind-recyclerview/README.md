
# corbind-recyclerview

To add androidx recyclerview bindings, import `corbind-recyclerview` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-recyclerview:1.8.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**RecyclerView.Adapter** | [`dataChanges`][RecyclerView_Adapte_dataChanges] | Called when the RecyclerView's adapter data has been changed
**RecyclerView** | [`childAttachStateChangeEvents`][RecyclerView_childAttachStateChangeEvents] | Called when a view is attached to or detached from the RecyclerView.
                  | [`flingEvents`][RecyclerView_flingEvents] | Handle a fling given the velocities in both x and y directions
                  | [`scrollEvents`][RecyclerView_scrollEvents] | Called when a scrolling event has occurred on that RecyclerView.
                  | [`scrollStateChanges`][RecyclerView_scrollStateChanges] | Called when RecyclerView's scroll state changes.


## Example

```kotlin
rv.scrollStateChanges() // Flow<Int>
    .onEach { /* handle RecyclerView scroll state change events */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code

[RecyclerView_Adapte_dataChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerAdapterDataChanges.kt
[RecyclerView_childAttachStateChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewChildAttachStateChangeEvents.kt
[RecyclerView_flingEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewFlingEvents.kt
[RecyclerView_scrollEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewScrollEvents.kt
[RecyclerView_scrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-recyclerview/src/main/kotlin/ru/ldralighieri/corbind/recyclerview/RecyclerViewScrollStateChanges.kt
