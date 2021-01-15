
# corbind-recyclerview

To add androidx recyclerview bindings, import `corbind-recyclerview` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-recyclerview:1.4.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**RecyclerView** | `childAttachStateChangeEvents` | Called when a view is attached to or detached from the RecyclerView.
                  | `flingEvents` | Handle a fling given the velocities in both x and y directions
                  | `scrollEvents` | Called when a scrolling event has occurred on that RecyclerView.
                  | `scrollStateChanges` | Called when RecyclerView's scroll state changes.
**RecyclerView.Adapter** | `dataChanges` | Called when the RecyclerView's adapter data has been changed  |   |


## Example

```kotlin
rv.scrollStateChanges() // Flow<Int>
    .onEach { /* handle RecyclerView scroll state change events */ }
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
