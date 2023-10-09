---
layout: page
title: Corbind
subtitle: corbind module
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Platform bindings.
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

To add platform bindings, import `corbind` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind:1.9.0")
}
```

## List of extensions

### app
Component | Extension | Description
--|---|--
**DatePickerDialog** | [`dateSetEvents`][DatePickerDialog_dateSetEvents] | Called when the user sets the date

### content
Component | Extension | Description
--|---|--
**Context** | [`receivesBroadcast`][Context_receivesBroadcast] | Called with any broadcast Intent that matches filter

### view

Component | Extension | Description
--|---|--
**View** | [`attachEvents`][View_attachEvents] | Called when the view is attached to a window.
         | [`attaches`][View_attaches] | Called when the view is attached to a window.
         | [`detaches`][View_detaches] | Called when the view is detached from a window.
         | [`clicks`][View_clicks] | Called when a view has been clicked.
         | [`drags`][View_drags] | Called when a drag event is dispatched to a view.
         | [`focusChanges`][View_focusChanges] | Called when the focus state of a view has changed.
         | [`hovers`][View_hovers] | Called when a hover event is dispatched to a view.
         | [`keys`][View_keys] | Called when a hardware key is dispatched to a view.
         | [`layoutChanges`][View_layoutChanges] | Called when the layout bounds of a view changes due to layout processing.
         | [`layoutChangeEvents`][View_layoutChangeEvents] | A more advanced version of the `layoutChanges`.
         | [`longClicks`][View_longClicks] | Called when a view has been clicked and held.
         | [`scrollChangeEvents`][View_scrollChangeEvents] | Called when the scroll position of a view changes.
         | [`systemUiVisibilityChanges`][View_systemUiVisibilityChanges] | Called when the status bar changes visibility because of a call to View#setSystemUiVisibility(int). `Deprecated, use windowInsetsApplyEvents`.
         | [`touches`][View_touches] | Called when a touch event is dispatched to a view.
         | [`draws`][View_draws] | Called when the view tree is about to be drawn.
         | [`globalLayouts`][View_globalLayouts] | Called when the global layout state or the visibility of views within the view tree changes.
         | [`preDraws`][View_preDraws] | Callback method to be invoked when the view tree is about to be drawn.
         | [`windowInsetsApplyEvents`][View_windowInsetsApplyEvents] | Called when window insets applying on a view in a custom way.
**ViewGroup** | [`changeEvents`][ViewGroup_changeEvents] | Called when the hierarchy within this view changed. The hierarchy changes whenever a child is added to or removed from this view.
**MenuItem** | [`actionViewEvents`][MenuItem_actionViewEvents] | Called when a menu item is collapsed or collapsed.
             | [`clicks`][MenuItem_clicks] | Called when a menu item has been invoked.

### widget

Component | Extension | Description
--|---|--
**AbsListView** | [`scrollEvents`][AbsListView_scrollEvents] | Called when the list or grid has been scrolled.
**Adapter** | [`dataChanges`][Adapter_dataChanges] | Called when a data set has been changed
**AdapterView** | [`itemClicks`][AdapterView_itemClicks] | Called when an item in this AdapterView has been clicked.
            | [`itemClickEvents`][AdapterView_itemClickEvents] | A more advanced version of the `itemClicks`.
            | [`itemLongClicks`][AdapterView_itemLongClicks] | Called when an item in this view has been clicked and held.
            | [`itemLongClickEvents`][AdapterView_itemLongClickEvents] | A more advanced version of the `itemLongClicks`.
            | [`itemSelections`][AdapterView_itemSelections] | Called when an item in this view has been selected.
            | [`selectionEvents`][AdapterView_selectionEvents] | A more advanced version of the `itemSelections`.
**AutoCompleteTextView** | [`dismisses`][AutoCompleteTextView_dismisses] | Called whenever the AutoCompleteTextView's list of completion options has been dismissed.
                         | [`itemClickEvents`][AutoCompleteTextView_itemClickEvents] | Called when an item in AdapterView has been clicked.
**CalendarView** | [`dateChangeEvents`][CalendarView_dateChangeEvents] | Called upon change of the selected day.
**CompoundButton** | [`checkedChanges`][CompoundButton_checkedChanges] | Called when the checked state of a compound button has changed.
**DatePicker** | [`dateChangeEvents`][DatePicker_dateChangeEvents] | Called upon a date change.
**NumberPicker** | [`scrollStateChanges`][NumberPicker_scrollStateChanges] | Called when number picker scroll state has changed.
                 | [`valueChangeEvents`][NumberPicker_valueChangeEvents] | Called upon a change of the current value.
**PopupMenu** | [`dismisses`][PopupMenu_dismisses] | Called when the associated menu has been dismissed.
              | [`itemClicks`][PopupMenu_itemClicks] | Called when a menu item is clicked if the item itself did not already handle the event.
**RadioGroup** | [`checkedChanges`][RadioGroup_checkedChanges] | Called when the checked radio button has changed.
**RatingBar** | [`ratingChanges`][RatingBar_ratingChanges] | Called when the rating has changed.
              | [`ratingChangeEvents`][RatingBar_ratingChangeEvents] | A more advanced version of the `ratingChanges`.
**SearchView** | [`queryTextChanges`][SearchView_queryTextChanges] | Called when the query text is changed.
               | [`queryTextChangeEvents`][SearchView_queryTextChangeEvents] | A more advanced version of the `queryTextChanges`.
**SeekBar** | [`changes`][SeekBar_changes] | Called when the progress level has changed.
            | [`userChanges`][SeekBar_userChanges] | Called when the progress level has changed by user.
            | [`systemChanges`][SeekBar_systemChanges] | Called when the progress level has changed by system.
            | [`changeEvents`][SeekBar_changeEvents] | A more advanced version of previous events.
**TextView** | [`textChanges`][TextView_textChanges] | Called when the text has changed.
             | [`textChangeEvents`][TextView_textChangeEvents] | A more advanced version of the `textChanges`.
             | [`afterTextChangeEvents`][TextView_afterTextChangeEvents] | Called after text has been changed.
             | [`beforeTextChangeEvents`][TextView_beforeTextChangeEvents] | Called before text has been changed.
             | [`editorActions`][TextView_editorActions] | Called when an action is performed on the editor.
             | [`editorActionEvents`][TextView_editorActionEvents] | A more advanced version of the `editorActions`.
**TimePicker** | [`timeChangeEvents`][TimePicker_timeChangeEvents] | Called when time has been adjusted.
**Toolbar**  | [`itemClicks`][Toolbar_itemClicks] | Called when a menu item is clicked if the item itself did not already handle the event.
             | [`navigationClicks`][Toolbar_navigationClicks] | Called whenever the user clicks the navigation button at the start of the toolbar.


## Examples

Traditional example of login button enabling/disabling by email and password field validation:
```kotlin
combine(
    etEmail.textChanges() // Flow<CharSequence>
        .map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },

    etPassword.textChanges() // Flow<CharSequence>
        .map { it.length > 7 },

    transform = { email, password -> email && password }
)
    .onEach { btLogin.isEnabled = it }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

Handle an authorization event, which can be started by pressing a button `bt_login` or by pressing an action `EditorInfo.IME_ACTION_DONE` on the keyboard:
```kotlin
merge(
    btLogin.clicks(), // Flow<Unit>

    etPassword.editorActionEvents() // Flow<TextViewEditorActionEvent>
        .filter { it.actionId == EditorInfo.IME_ACTION_DONE }
        .filter { btLogin.isEnabled }
)
    .onEach { /* handle an authorization event */}
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

Handle nfc adapter state changed
```kotlin
context
    .receivesBroadcast(
        IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
    ) // Flow<Intent>
    .onEach { /* handle nfc adapter state changed */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

Handle status bars or navigation bars visibility
```kotlin
window.decorView.windowInsetsApplyEvents() // Flow<WindowInsetsEvent>
    .map { event ->
        with(event) {
            view.onApplyWindowInsets(insets)
        }
    }
    .map { insets ->
        insets.isVisible(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    }
    .onEach { /* handle status bars or navigation bars visibility */ }
    .flowWithLifecycle(lifecycle)
    .launchIn(lifecycleScope) // lifecycle-runtime-ktx
```

More examples in [source code][source]

[source]: https://github.com/LDRAlighieri/Corbind/tree/master/corbind

[//]: # (app)
[DatePickerDialog_dateSetEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/app/DatePickerDialogDateSetEvents.kt

[//]: # (content)
[Context_receivesBroadcast]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/content/ContextReceivesBroadcast.kt

[//]: # (view)
[View_attachEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewAttachEvents.kt
[View_attaches]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewAttaches.kt#L40
[View_detaches]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewAttaches.kt#L118
[View_clicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewClicks.kt
[View_drags]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewDrags.kt
[View_focusChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewFocusChanges.kt
[View_hovers]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewHovers.kt
[View_keys]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewKeys.kt
[View_layoutChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewLayoutChanges.kt
[View_layoutChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewLayoutChangeEvents.kt
[View_longClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewLongClicks.kt
[View_scrollChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewScrollChangeEvents.kt
[View_systemUiVisibilityChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewSystemUiVisibilityChanges.kt
[View_touches]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewTouches.kt
[View_draws]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewTreeObserverDraws.kt
[View_globalLayouts]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewTreeObserverGlobalLayouts.kt
[View_preDraws]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewTreeObserverPreDraws.kt
[View_windowInsetsApplyEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewWindowInsetsApplyEvents.kt
[ViewGroup_changeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/ViewGroupHierarchyChangeEvents.kt
[MenuItem_actionViewEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/MenuItemActionViewEvents.kt
[MenuItem_clicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/view/MenuItemClicks.kt

[//]: # (widget)
[AbsListView_scrollEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AbsListViewScrollEvents.kt
[Adapter_dataChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AdapterDataChanges.kt
[AdapterView_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AdapterViewItemClicks.kt
[AdapterView_itemClickEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AdapterViewItemClickEvents.kt
[AdapterView_itemLongClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AdapterViewItemLongClicks.kt
[AdapterView_itemLongClickEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AdapterViewItemLongClickEvents.kt
[AdapterView_itemSelections]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AdapterViewItemSelections.kt
[AdapterView_selectionEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AdapterViewSelectionEvents.kt
[AutoCompleteTextView_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AutoCompleteTextViewDismisses.kt
[AutoCompleteTextView_itemClickEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/AutoCompleteTextViewItemClickEvents.kt
[CalendarView_dateChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/CalendarViewDateChangeEvents.kt
[CompoundButton_checkedChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/CompoundButtonCheckedChanges.kt
[DatePicker_dateChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/DatePickerChangedEvents.kt
[NumberPicker_scrollStateChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/NumberPickerScrollStateChanges.kt
[NumberPicker_valueChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/NumberPickerValueChangeEvents.kt
[PopupMenu_dismisses]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/PopupMenuDismisses.kt
[PopupMenu_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/PopupMenuItemClicks.kt
[RadioGroup_checkedChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/RadioGroupCheckedChanges.kt
[RatingBar_ratingChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/RatingBarRatingChanges.kt
[RatingBar_ratingChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/RatingBarRatingChangeEvents.kt
[SearchView_queryTextChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/SearchViewQueryTextChanges.kt
[SearchView_queryTextChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/SearchViewQueryTextChangeEvents.kt
[SeekBar_changes]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/SeekBarChanges.kt#L34
[SeekBar_userChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/SeekBarChanges.kt#L168
[SeekBar_systemChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/SeekBarChanges.kt#L253
[SeekBar_changeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/SeekBarChangeEvents.kt
[TextView_textChanges]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/TextViewTextChanges.kt
[TextView_textChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/TextViewTextChangeEvents.kt
[TextView_afterTextChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/TextViewAfterTextChangeEvents.kt
[TextView_beforeTextChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/TextViewBeforeTextChangeEvents.kt
[TextView_editorActions]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/TextViewEditorActions.kt
[TextView_editorActionEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/TextViewEditorActionEvents.kt
[TimePicker_timeChangeEvents]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/TimePickerChangedEvents.kt
[Toolbar_itemClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/ToolbarItemClicks.kt
[Toolbar_navigationClicks]: https://github.com/LDRAlighieri/Corbind/blob/master/corbind/src/main/kotlin/ru/ldralighieri/corbind/widget/ToolbarNavigationClicks.kt
