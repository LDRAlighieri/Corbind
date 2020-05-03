
# corbind-material

To add material bindings, import `corbind-material` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-material:1.3.2'
```

## List of extensions

Component | Extension | Description
--|---|--
**AppBarLayout** | `offsetChanges` | Called when the AppBarLayout's layout offset has been changed
**BottomNavigationView** | `itemReselections` | Called when the currently selected item in the bottom navigation menu is selected again.
                         | `itemSelections`| Called when an item in the bottom navigation menu is selected.
**View**<br>(BottomSheetBehavior) | `slides` | Called when the bottom sheet is being dragged.
                                  | `stateChanges` | Called when the bottom sheet changes its state.
**Chip** | `closeIconClicks` | Called when the chip’s close icon is clicked.
**ChipGroup** | `checkedChanges` | Called when the checked chip has changed (only in single selection mode).
**MaterialButton** | `checkedChanges` | Called when the checked state of a MaterialButton has changed.
**MaterialButtonToggleGroup** | `buttonCheckedChangeEvents` | Called when a `MaterialButton` in this group is checked or unchecked (only *not* in single selection mode).
                              | `buttonCheckedChanges` | Called when a `MaterialButton` in this group is checked (only in single selection mode).
**MaterialCardView** | `checkedChanges` | Called when the card checked state changes.
**MaterialDatePicker** | `cancels` | Called when the user cancels the picker via back button or a touch outside the view. It is not called when the user clicks the cancel button. To add a listener for use when the user clicks the cancel button, use `negativeClicks` extension.
                       | `dismisses` | Called whenever the DialogFragment is dismissed, no matter how it is dismissed.
                       | `negativeClicks` | Called when the user clicks the cancel button.
                       | `positiveClicks` | Called when the user confirms a valid selection.
**NavigationView** | `itemSelections` | Called when an item in the navigation menu is selected.
**Snackbar** | `dismisses` | Called when the given Snackbar has been dismissed, either through a time-out, having been manually dismissed, or an action being clicked.
             | `shown` | Called when the given Snackbar is visible.
**View**<br>(SwipeDismissBehavior) | `dismisses` | Called when view has been dismissed via swiping.
             | `dragStateChanges` | Called when the drag state has changed.
**TabLayout** | `selections` | Called when a tab enters the selected state.
              | `selectionEvents` | A more advanced version of the `selections`.
**TextInputLayout** | `endIconChanges` | Called when the end icon changes.
                    | `endIconClicks` | Called when the end icon is clicked.
                    | `endIconLongClicks` | Called when the end icon is long clicked.
                    | `startIconClicks` | Called when the start icon is clicked.
                    | `startIconLongClicks` | Called when the start icon is long clicked.


## Example

```kotlin
chipGroup.checkedChanges() // Flow<Int>
    .onEach {
      tv_message =
        if (it != View.NO_ID) "Chip #$it selected" else "No one сhip selected"
    }
    .launchIn(scope)
```

More examples in source code
