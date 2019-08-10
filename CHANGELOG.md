# ChangeLog

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
