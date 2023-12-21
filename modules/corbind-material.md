---
layout: page
title: Corbind
subtitle: corbind-material module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Material bindings.
tags: [android,kotlin,flow,widget,ui,material,binding,recyclerview,coroutines,kotlin-extensions,kotlin-library,android-library,fragment,viewpager,activity,drawerlayout,appcompat,kotlin-coroutines,swiperefreshlayout,android-ui-widgets]
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

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind-material:1.10.0")
}
```

## List of extensions

Component | Extension | Description
--|---|--
**AppBarLayout** | [`offsetChanges`][AppBarLayout_offsetChanges] | Called when the AppBarLayout's layout offset has been changed
**View**<br>(BottomSheetBehavior) | [`slides`][BottomSheetBehavior_slides] | Called when the bottom sheet is being dragged.
                                  | [`stateChanges`][BottomSheetBehavior_stateChanges] | Called when the bottom sheet changes its state.
**Chip** | [`closeIconClicks`][Chip_closeIconClicks] | Called when the chip’s close icon is clicked.
**ChipGroup** | [`checkedChanges`][ChipGroup_checkedChanges] | Called when the checked chips are changed.
**View**<br>(HideBottomViewOnScrollBehavior) | [`bottomViewScrollStateChanges`][HideBottomViewOnScrollBehavior_bottomViewScrollStateChanges] | Called when the bottom view changes its scrolled state.
**MaskableFrameLayout** | [`maskChanges`][MaskableFrameLayout_maskChanges] | Called when changes in a mask's RectF occur.
**MaterialButton** | [`checkedChanges`][MaterialButton_checkedChanges] | Called when the checked state of a MaterialButton has changed.
**MaterialButtonToggleGroup** | [`buttonCheckedChangeEvents`][MaterialButtonToggleGroup_buttonCheckedChangeEvents] | Called when a `MaterialButton` in this group is checked or unchecked (only *not* in single selection mode).
                              | [`buttonCheckedChanges`][MaterialButtonToggleGroup_buttonCheckedChanges] | Called when a `MaterialButton` in this group is checked (only in single selection mode).
**MaterialCardView** | [`checkedChanges`][MaterialCardView_checkedChanges] | Called when the card checked state changes.
**MaterialDatePicker** | [`cancels`][MaterialDatePicker_cancels] | Called when the user cancels the date picker via back button or a touch outside the view. It is not called when the user clicks the cancel button. To add a listener for use when the user clicks the cancel button, use `negativeClicks` extension.
                       | [`dismisses`][MaterialDatePicker_dismisses] | Called whenever the date picker is dismissed, no matter how it is dismissed.
                       | [`negativeClicks`][MaterialDatePicker_negativeClicks] | Called when the user clicks the date picker cancel button.
                       | [`positiveClicks`][MaterialDatePicker_positiveClicks] | Called when the user confirms a valid selection of the date.
**MaterialTimePicker** | [`cancels`][MaterialTimePicker_cancels] | Called when the user cancels the time picker via back button or a touch outside the view. It is not called when the user clicks the cancel button. To add a listener for use when the user clicks the cancel button, use `negativeClicks` extension.
                       | [`dismisses`][MaterialTimePicker_dismisses] | Called whenever the time picker is dismissed, no matter how it is dismissed.
                       | [`negativeClicks`][MaterialTimePicker_negativeClicks] | Called when the user clicks the time picker cancel button.
                       | [`positiveClicks`][MaterialTimePicker_positiveClicks] | Called when the user confirms a valid selection of the time.
**NavigationBarView** | [`itemReselections`][NavigationBarView_itemReselections] | Called when the currently selected navigation item is reselected.
                         | [`itemSelections`][NavigationBarView_itemSelections] | Called when a navigation item is selected.
**NavigationView** | [`itemSelections`][NavigationView_itemSelections] | Called when an item in the navigation menu is selected.
**RangeSlider** | [`touches`][RangeSlider_touches] | Called when a range slider's touch event is being started/stopped.
                | [`valuesChanges`][RangeSlider_valuesChanges] | Called a range slider's value is changed. This is called for all existing values to check all the current values use.
                | [`valuesChangeEvents`][RangeSlider_valuesChangeEvents] | A more advanced version of the `valuesChanges`.
**SearchBar** | [`navigationClicks`][SearchBar_navigationClicks] | Called whenever the user clicks the navigation button at the start of the searchbar.
**SearchView** | [`transitionStateChanges`][SearchView_transitionStateChanges] | Called when the given `SearchView's` transition state has changed.
            | [`transitionStateChangeEvents`][SearchView_transitionStateChangeEvents] | A more advanced version of the `transitionStateChanges`.
**View**<br>(SideSheetBehavior) | [`sideSheetSlides`][SideSheetBehavior_sideSheetSlides] | Called when the side sheet is being dragged.
                                | [`sideSheetStateChanges`][SideSheetBehavior_sideSheetStateChanges] | Called when the side sheet changes its state.
**Slider** | [`touches`][Slider_touches] | Called when a slider's touch event is being started/stopped.
           | [`valueChanges`][Slider_valueChanges] | Called a slider's value is changed.
           | [`valueChangeEvents`][Slider_valueChangeEvents] | A more advanced version of the `valueChanges`.
**Snackbar** | [`dismisses`][Snackbar_dismisses] | Called when the given Snackbar has been dismissed, either through a time-out, having been manually dismissed, or an action being clicked.
             | [`shown`][Snackbar_shown] | Called when the given Snackbar is visible.
**View**<br>(SwipeDismissBehavior) | [`dismisses`][SwipeDismissBehavior_dismisses] | Called when view has been dismissed via swiping.
             | [`dragStateChanges`][SwipeDismissBehavior_dragStateChanges] | Called when the drag state has changed.
**TabLayout** | [`selections`][TabLayout_selections] | Called when a tab enters the selected state.
              | [`selectionEvents`][TabLayout_selectionEvents] | A more advanced version of the `selections`.
**TextInputLayout** | [`endIconChanges`][TextInputLayout_endIconChanges] | Called when the end icon changes.
                    | [`endIconClicks`][TextInputLayout_endIconClicks] | Called when the end icon is clicked.
                    | [`endIconLongClicks`][TextInputLayout_endIconLongClicks] | Called when the end icon is long clicked.
                    | [`startIconClicks`][TextInputLayout_startIconClicks] | Called when the start icon is clicked.
                    | [`startIconLongClicks`][TextInputLayout_startIconLongClicks] | Called when the start icon is long clicked.


## Example

```kotlin
chipGroup.checkedChanges() // Flow<Int>
    .onEach { /* handle checked ids */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind-material

[AppBarLayout_offsetChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/AppBarLayoutOffsetChanges.kt
[BottomSheetBehavior_slides]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/BottomSheetBehaviorSlides.kt
[BottomSheetBehavior_stateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/BottomSheetBehaviorStateChanges.kt
[Chip_closeIconClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/ChipCloseIconClicks.kt
[ChipGroup_checkedChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/ChipGroupCheckedChanges.kt
[HideBottomViewOnScrollBehavior_bottomViewScrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/HideBottomViewOnScrollBehaviorScrollStateChanges.kt
[MaskableFrameLayout_maskChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaskableFrameLayoutMaskChanges.kt
[MaterialButton_checkedChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialButtonCheckedChanges.kt
[MaterialButtonToggleGroup_buttonCheckedChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialButtonToggleGroupCheckedChangeEvents.kt
[MaterialButtonToggleGroup_buttonCheckedChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialButtonToggleGroupCheckedChanges.kt
[MaterialCardView_checkedChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialCardViewCheckedChanges.kt
[MaterialDatePicker_cancels]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialDatePickerCancels.kt
[MaterialDatePicker_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialDatePickerDismisses.kt
[MaterialDatePicker_negativeClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialDatePickerNegativeClicks.kt
[MaterialDatePicker_positiveClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialDatePickerPositiveClicks.kt
[MaterialTimePicker_cancels]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialTimePickerCancels.kt
[MaterialTimePicker_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialTimePickerDismisses.kt
[MaterialTimePicker_negativeClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialTimePickerNegativeClicks.kt
[MaterialTimePicker_positiveClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/MaterialTimePickerPositiveClicks.kt
[NavigationBarView_itemReselections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/NavigationBarViewItemReselections.kt
[NavigationBarView_itemSelections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/NavigationBarViewItemSelections.kt
[NavigationView_itemSelections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/NavigationViewItemSelections.kt
[RangeSlider_touches]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/RangeSliderTouches.kt
[RangeSlider_valuesChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/RangeSliderValuesChanges.kt
[RangeSlider_valuesChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/RangeSliderValuesChangeEvents.kt
[SearchBar_navigationClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SearchBarNavigationClicks.kt
[SearchView_transitionStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SearchViewTransitionStateChanges.kt
[SearchView_transitionStateChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SearchViewTransitionStateChangeEvents.kt
[SideSheetBehavior_sideSheetSlides]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SideSheetBehaviorSlides.kt
[SideSheetBehavior_sideSheetStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SideSheetBehaviorStateChanges.kt
[Slider_touches]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SliderTouches.kt
[Slider_valueChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SliderValueChanges.kt
[Slider_valueChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SliderValueChangeEvents.kt
[Snackbar_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SnackbarDismisses.kt
[Snackbar_shown]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SnackbarShown.kt
[SwipeDismissBehavior_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SwipeDismissBehaviorDesmisses.kt
[SwipeDismissBehavior_dragStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/SwipeDismissBehaviorDragStateChanges.kt
[TabLayout_selections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/TabLayoutSelections.kt
[TabLayout_selectionEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/TabLayoutSelectionEvents.kt
[TextInputLayout_endIconChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/TextInputLayoutEndIconChanges.kt
[TextInputLayout_endIconClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/TextInputLayoutEndIconClicks.kt
[TextInputLayout_endIconLongClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/TextInputLayoutEndIconLongClicks.kt
[TextInputLayout_startIconClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/TextInputLayoutStartIconClicks.kt
[TextInputLayout_startIconLongClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind-material/src/main/kotlin/ru/ldralighieri/corbind/material/TextInputLayoutStartIconLongClicks.kt
