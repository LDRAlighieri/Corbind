---
layout: page
title: Corbind
subtitle: Changelog
show-avatar: false
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Changelog.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,receivechannel,flow,data binding,changelog]
---

## Version 1.2.0

* New: Bindings list:
    * Google "material" library bindings:
        * `corbind-material`:
            * `MaterialDatePicker`:
                * `cancels`
                * `dismisses`
                * `negativeClicks`
                * `positiveClicks`
* Update: Kotlin coroutines modules dependency to v1.3.2
* Update: Material components dependency to v1.1.0-beta01
* Update: Support registering multiple `BottomSheetCallbacks`


## Version 1.2.0-RC

* New: Bindings list:
  * Platform bindings:
    * `corbind`:
      * `DatePickerDialog`:
        * `dateSetEvents`
      * `CalendarView`:
      	* `dateChangeEvents`
      * `DatePicker`:
        * `dateChangeEvents`
      * `NumberPicker`:
        * `scrollStateChanges`
        * `valueChangeEvents`
      * `TimePicker`:
        * `timeChangeEvents`
  * Google "material" library bindings:
    * `corbind-material`:
        * `BottomNavigationView`:
          * `itemReselections`
        * `MaterialButton`:
          * `checkedChanges`
        * `MaterialButtonToggleGroup`:
          * `buttonCheckedChangeEvents` (only *not* in single selection mode)
          * `buttonCheckedChanges` (only in single selection mode)
        * `MaterialCardView`:
          * `checkedChanges`
        * `TextInputLayout`:
          * `endIconChanges`
          * `endIconClicks`
          * `endIconLongClicks`
          * `startIconClicks`
          * `startIconLongClicks`
* Update: Material components dependency to v1.1.0-alpha10
* Update: ViewPager2 dependency to v1.0.0-beta04


## Version 1.1.2

* Update: Kotlin coroutines modules dependency to v1.3.1
* Update: Android sdk to v29
* Update: Updated sample
* Fix: Fixed various inaccuracies, refactoring


## Version 1.1.0

* New: ViewPager2 module
* New: Bindings list:
    * AndroidX library bindings:
        * `corbind-viewpager2`:
			* `ViewPager2`:
				* `pageScrollEvents`
				* `pageScrollStateChanges`
				* `pageSelections`
	* Google "material" library bindings:
	    * `corbind-material`:
	        * `BottomSheetBehavior`:
	            * `slides`
	            * `stateChanges`
	        * `ChipGroup`:
	            * `checkedChanges` (only in single selection mode)
	        * `Snackbar`:
	            * `shown`
	        * `SwipeDismissBehavior`:
	            * `dragStateChanges`
* Fix: Fixed sources jars generation (#6)


## Version 1.0.1

* Fix: `TextView` `afterTextChangeEvents` access modifier changed (#1).
* Fix: `SeekBar` `changeEvents` access modifier changed.
* Fix: Fixed typo in RecyclerView artifact id (#2).


## Version 1.0.0

* New: Added a small sample.
* Update: Kotlin modules dependency to v1.3.50.
* Update: Kotlin coroutine modules dependency to v1.3.0.
* Fix: Fixed confusion with `Chip` extension name `clicks`, the correct name is `closeIconClicks`.
* Fix: Added `@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)` annotation for one of the `View` `draws` extensions.


## Version 1.0.0-RC

* New: Kotlin Coroutine's Flow support.
* New: AndroidX support.
* New: Bindings list:
	* Platform bindings:
		* `corbind`:
			* `MenuItem`:
				* `actionViewEvents`
				* `clicks`
			* `View`:
				* `attaches`
				* `detaches`
				* `attachEvents`
				* `clicks`
				* `drags`
				* `focusChanges`
				* `hovers`
				* `layoutChangeEvents`
				* `layoutChanges`
				* `longClicks`
				* `scrollChangeEvents`
				* `systemUiVisibilityChanges`
				* `touches`
				* `draws`
				* `globalLayouts`
				* `preDraws`
			* `ViewGroup`:
				* `changeEvents`
			* `AbsListView`:
				* `scrollEvents`
			* `Adapter`:
				* `dataChanges`
			* `AdapterView`:
				* `itemClickEvents`
				* `itemClicks`
				* `itemLongClickEvents`
				* `itemLongClicks`
				* `itemSelections`
				* `selectionEvents`
			* `AutoCompleteTextView`:
				* `itemClickEvents`
			* `CompoundButton`:
				* `checkedChanges`
			* `PopupMenu`:
				* `dismisses`
				* `itemClicks`
			* `RadioGroup`:
				* `checkedChanges`:
			* `RatingBar`:
				* `ratingChangeEvents`
				* `ratingChanges`
			* `SearchView`:
				* `queryTextChangeEvents`
				* `queryTextChanges`
			* `SeekBar`:
				* `changeEvents`
				* `changes`
				* `userChanges`
				* `systemChanges`
			* `TextView`:
				* `afterTextChangeEvents`
				* `beforeTextChangeEvents`
				* `editorActionEvents`
				* `editorActions`
				* `textChangeEvents`
				* `textChanges`
			* `Toolbar`:
				* `itemClicks`
				* `navigationClicks`
	* AndroidX library bindings:
		* `corbind-core`:
			* `NestedScrollView`:
				* `scrollChangeEvents`
		* `corbind-appcompat`:
			* `ActionMenuView`:
				* `itemClicks`
			* `PopupMenu`:
				* `dismisses`
				* `itemClicks`
			* `SearchView`:
				* `queryTextChangeEvents`
				* `queryTextChanges`
			* `Toolbar`:
				* `itemClicks`
				* `navigationClicks`
		* `corbind-drawerlayout`:
			* `DrawerLayout`:
				* `drawerOpens`
		* `corbind-leanback`:
			* `SearchBar`:
				* `searchQueryChangeEvents`
				* `searchQueryChanges`
			* `SearchEditText`:
				* `keyboardDismisses`
		* `corbind-recyclerview`:
			* `RecyclerView`:
				* `childAttachStateChangeEvents`
				* `flingEvents`
				* `scrollEvents`
				* `scrollStateChanges`
			* `RecyclerView.Adapter`:
				* `dataChanges`
		* `corbind-slidingpanelayout`:
			* `SlidingPaneLayout`:
				* `panelOpens`
				* `panelSlides`
		* `corbind-swiperefreshlayout`:
			* `SwipeRefreshLayout`:
				* `refreshes`
		* `corbind-viewpager`:
			* `ViewPager`:
				* `pageScrollEvents`
				* `pageScrollStateChanges`
				* `pageSelections`
	* Google "material" library bindings:
		* `corbind-material`:
			* `AppBarLayout`:
				* `offsetChanges`
			* `BottomNavigationView`:
				* `itemSelections`
			* `Chip`:
				* `closeIconClicks`
			* `NavigationView`:
				* `itemSelections`
			* `Snackbar`:
				* `dismisses`
			* `View`:
				* `dismisses`
			* `TabLayout`:
				* `selectionEvents`
				* `selections`
* Update: Kotlin modules dependency to v1.3.41.
* Update: Kotlin coroutine modules dependency to v1.3.0-RC2.
* Update: Minimum SDK version is now 14
* Fix: Internal `corbindReceiveChannel` emission
