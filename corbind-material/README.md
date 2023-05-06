
# corbind-material

To add material bindings, import `corbind-material` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-material:1.7.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**AppBarLayout** | `offsetChanges` | Called when the AppBarLayout's layout offset has been changed
**View**<br>(BottomSheetBehavior) | `slides` | Called when the bottom sheet is being dragged.
                                  | `stateChanges` | Called when the bottom sheet changes its state.
**Chip** | `closeIconClicks` | Called when the chip’s close icon is clicked.
**ChipGroup** | `checkedChanges` | Called when the checked chips are changed.
**View**<br>(HideBottomViewOnScrollBehavior) | `bottomViewScrollStateChanges` | Called when the bottom view changes its scrolled state.
**MaskableFrameLayout** | `maskChanges` | Called when changes in a mask's RectF occur.
**MaterialButton** | `checkedChanges` | Called when the checked state of a MaterialButton has changed.
**MaterialButtonToggleGroup** | `buttonCheckedChangeEvents` | Called when a `MaterialButton` in this group is checked or unchecked (only *not* in single selection mode).
                              | `buttonCheckedChanges` | Called when a `MaterialButton` in this group is checked (only in single selection mode).
**MaterialCardView** | `checkedChanges` | Called when the card checked state changes.
**MaterialDatePicker** | `cancels` | Called when the user cancels the date picker via back button or a touch outside the view. It is not called when the user clicks the cancel button. To add a listener for use when the user clicks the cancel button, use `negativeClicks` extension.
                       | `dismisses` | Called whenever the date picker is dismissed, no matter how it is dismissed.
                       | `negativeClicks` | Called when the user clicks the date picker cancel button.
                       | `positiveClicks` | Called when the user confirms a valid selection of the date.
**MaterialTimePicker** | `cancels` | Called when the user cancels the time picker via back button or a touch outside the view. It is not called when the user clicks the cancel button. To add a listener for use when the user clicks the cancel button, use `negativeClicks` extension.
                       | `dismisses` | Called whenever the time picker is dismissed, no matter how it is dismissed.
                       | `negativeClicks` | Called when the user clicks the time picker cancel button.
                       | `positiveClicks` | Called when the user confirms a valid selection of the time.
**NavigationBarView** | `itemReselections` | Called when the currently selected navigation item is reselected.
                         | `itemSelections`| Called when a navigation item is selected.
**NavigationView** | `itemSelections` | Called when an item in the navigation menu is selected.
**RangeSlider** | `touches` | Called when a range slider's touch event is being started/stopped.
                | `valuesChanges` | Called a range slider's value is changed. This is called for all existing values to check all the current values use.
                | `valuesChangeEvents` | A more advanced version of the `valuesChanges`.
**SearchBar** | `navigationClicks` | Called whenever the user clicks the navigation button at the start of the searchbar.
**SearchView** | `transitionStateChanges` | Called when the given `SearchView's` transition state has changed.
            | `transitionStateChangeEvents` | A more advanced version of the `transitionStateChanges`.
**View**<br>(SideSheetBehavior) | `sideSheetSlides` | Called when the side sheet is being dragged.
                                | `sideSheetStateChanges` | Called when the side sheet changes its state.
**Slider** | `touches` | Called when a slider's touch event is being started/stopped.
           | `valueChanges` | Called a slider's value is changed.
           | `valueChangeEvents` | A more advanced version of the `valueChanges`.
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
    .onEach { /* handle checked ids */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in source code
