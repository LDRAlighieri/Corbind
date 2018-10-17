package ru.ldralighieri.corbind.view

import android.view.MenuItem

// -----------------------------------------------------------------------------------------------

sealed class MenuItemActionViewEvent {
    abstract val menuItem: MenuItem
}

data class MenuItemActionViewCollapseEvent(
        override val menuItem: MenuItem
) : MenuItemActionViewEvent()

data class MenuItemActionViewExpandEvent(
        override val menuItem: MenuItem
) : MenuItemActionViewEvent()