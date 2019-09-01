
# corbind-material

To add material bindings, import `corbind-material` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-material:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**AppBarLayout** | `offsetChanges` | Called when the AppBarLayout's layout offset has been changed
**BottomNavigationView** | `itemSelections` | Called when an item in the bottom navigation menu is selected.
**View**<br>(BottomSheetBehavior) | `slides` | Called when the bottom sheet is being dragged.
                                  | `stateChanges` | Called when the bottom sheet changes its state.
**Chip** | `closeIconClicks` | Called when the chip’s close icon is clicked.
**ChipGroup** | `checkedChanges` | Called when the checked chip has changed (only in single selection mode).
**NavigationView** | `itemSelections` | Called when an item in the navigation menu is selected.
**Snackbar** | `dismisses` | Called when the given Snackbar has been dismissed, either through a time-out, having been manually dismissed, or an action being clicked.
             | `shown` | Called when the given Snackbar is visible.
**View**<br>(SwipeDismissBehavior) | `dismisses` | Called when view has been dismissed via swiping.
             | `dragStateChanges` | Called when the drag state has changed.
**TabLayout** | `selections` | Called when a tab enters the selected state.
              | `selectionEvents` | A more advanced version of the `selections`.


## Example

```kotlin
chipGroup.checkedChanges() // Flow<Int>
    .onEach {
      tv_message =
        if (it != View.NO_ID) "Chip #$it selected" else "No one сhip selected"
    }
    .launchIn(scope)
```
