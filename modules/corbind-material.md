---
layout: page
title: Corbind
subtitle: corbind-material module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Material bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,material bindings,material]
---

<div style="text-align: center">
    <img src="https://ldralighieri.github.io/Corbind/img/corbind.svg" alt="Corbind logo"/>
</div>

<script async defer src="https://buttons.github.io/buttons.js"></script>
<div style="text-align: center">
  <a class="github-button" href="https://github.com/LDRAlighieri" data-size="large" aria-label="Follow @LDRAlighieri on GitHub">Follow</a>
  <a class="github-button" href="https://github.com/LDRAlighieri/Corbind" data-icon="octicon-star" data-size="large" aria-label="Star LDRAlighieri/Corbind on GitHub">Star</a>
</div>

To add material bindings, import `corbind-material` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-material:1.5.1'
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
**MaterialDatePicker** | `cancels` | Called when the user cancels the date picker via back button or a touch outside the view. It is not called when the user clicks the cancel button. To add a listener for use when the user clicks the cancel button, use `negativeClicks` extension.
                       | `dismisses` | Called whenever the date picker is dismissed, no matter how it is dismissed.
                       | `negativeClicks` | Called when the user clicks the date picker cancel button.
                       | `positiveClicks` | Called when the user confirms a valid selection of the date.
**MaterialTimePicker** | `cancels` | Called when the user cancels the time picker via back button or a touch outside the view. It is not called when the user clicks the cancel button. To add a listener for use when the user clicks the cancel button, use `negativeClicks` extension.
                       | `dismisses` | Called whenever the time picker is dismissed, no matter how it is dismissed.
                       | `negativeClicks` | Called when the user clicks the time picker cancel button.
                       | `positiveClicks` | Called when the user confirms a valid selection of the time.
**NavigationView** | `itemSelections` | Called when an item in the navigation menu is selected.
**RangeSlider** | `touches` | Called when a range slider's touch event is being started/stopped.
                | `valuesChanges` | Called a range slider's value is changed. This is called for all existing values to check all the current values use.
                | `valuesChangeEvents` | A more advanced version of the `valuesChanges`.
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
    .onEach {
      tv_message =
        if (it != View.NO_ID) "Chip #$it selected" else "No one сhip selected"
    }
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-material
