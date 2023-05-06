
# corbind

To add platform bindings, import `corbind` module:

```kotlin
dependencies {
    implementation("ru.ldralighieri.corbind:corbind:1.8.0")
}
```

## List of extensions

### app
Component | Extension | Description
--|---|--
**DatePickerDialog** | `dateSetEvents` | Called when the user sets the date

### content
Component | Extension | Description
--|---|--
**Context** | `receivesBroadcast` | Called with any broadcast Intent that matches filter

### view

Component | Extension | Description
--|---|--
**View** | `attachEvents` | Called when the view is attached to a window.
         | `detaches` | Called when the view is detached from a window.
         | `clicks` | Called when a view has been clicked.
         | `drags` | Called when a drag event is dispatched to a view.
         | `focusChanges` | Called when the focus state of a view has changed.
         | `hovers` | Called when a hover event is dispatched to a view.
         | `keys` | Called when a hardware key is dispatched to a view.
         | `layoutChanges` | Called when the layout bounds of a view changes due to layout processing.
         | `layoutChangeEvents` | A more advanced version of the `layoutChanges`.
         | `longClicks` | Called when a view has been clicked and held.
         | `scrollChangeEvents` | Called when the scroll position of a view changes.
         | `systemUiVisibilityChanges` | Called when the status bar changes visibility because of a call to View#setSystemUiVisibility(int). `Deprecated, use windowInsetsApplyEvents`.
         | `touches` | Called when a touch event is dispatched to a view.
         | `draws` | Called when the view tree is about to be drawn.
         | `globalLayouts` | Called when the global layout state or the visibility of views within the view tree changes.
         | `preDraws` | Callback method to be invoked when the view tree is about to be drawn.
         | `windowInsetsApplyEvents` | Called when window insets applying on a view in a custom way.
**ViewGroup** | `changeEvents` | Called when the hierarchy within this view changed. The hierarchy changes whenever a child is added to or removed from this view.
**MenuItem** | `actionViewEvents` | Called when a menu item is collapsed or collapsed.
             | `clicks` | Called when a menu item has been invoked.

### widget

Component | Extension | Description
--|---|--
**AbsListView** | `scrollEvents` | Called when the list or grid has been scrolled.
**Adapter** | `dataChanges` | Called when a data set has been changed
            | `itemClicks` | Called when an item in this AdapterView has been clicked.
            | `itemClickEvents` | A more advanced version of the `itemClicks`.
            | `itemLongClicks` | Called when an item in this view has been clicked and held.
            | `itemLongClickEvents` | A more advanced version of the `itemLongClicks`.
            | `itemSelections` | Called when an item in this view has been selected.
            | `selectionEvents` | A more advanced version of the `itemSelections`.
**AutoCompleteTextView** | `dismisses` | Called whenever the AutoCompleteTextView's list of completion options has been dismissed.
                         | `itemClickEvents` | Called when an item in AdapterView has been clicked.
**CalendarView** | `dateChangeEvents` | Called upon change of the selected day.
**CompoundButton** | `checkedChanges` | Called when the checked state of a compound button has changed.
**DatePicker** | `dateChangeEvents` | Called upon a date change.
**NumberPicker** | `scrollStateChanges` | Called when number picker scroll state has changed.
                 | `valueChangeEvents` | Called upon a change of the current value.
**PopupMenu** | `dismisses` | Called when the associated menu has been dismissed.
              | `itemClicks` | Called when a menu item is clicked if the item itself did not already handle the event.
**RadioGroup** | `checkedChanges` | Called when the checked radio button has changed.
**RatingBar** | `ratingChanges` | Called when the rating has changed.
              | `ratingChangeEvents` | A more advanced version of the `ratingChanges`.
**SearchView** | `queryTextChanges` | Called when the query text is changed.
               | `queryTextChangeEvents` | A more advanced version of the `queryTextChanges`.
**SeekBar** | `changes` | Called when the progress level has changed.
            | `userChanges` | Called when the progress level has changed by user.
            | `systemChanges` | Called when the progress level has changed by system.
            | `changeEvents` | A more advanced version of previous events.
**TextView** | `textChanges` | Called when the text has changed.
             | `textChangeEvents` | A more advanced version of the `textChanges`.
             | `afterTextChangeEvents` | Called after text has been changed.
             | `beforeTextChangeEvents` | Called before text has been changed.
             | `editorActions` | Called when an action is performed on the editor.
             | `editorActionEvents` | A more advanced version of the `editorActions`.
**TimePicker** | `timeChangeEvents` | Called when time has been adjusted.
**Toolbar**  | `itemClicks` | Called when a menu item is clicked if the item itself did not already handle the event.
             | `navigationClicks` | Called whenever the user clicks the navigation button at the start of the toolbar.


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

More examples in source code
