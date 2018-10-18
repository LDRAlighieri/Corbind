package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewGroup

// -----------------------------------------------------------------------------------------------

sealed class ViewGroupHierarchyChangeEvent {
    abstract val view: ViewGroup
    abstract val child: View
}

data class ViewGroupHierarchyChildViewAddEvent(
        override val view: ViewGroup,
        override val child: View
) : ViewGroupHierarchyChangeEvent()

data class ViewGroupHierarchyChildViewRemoveEvent(
        override val view: ViewGroup,
        override val child: View
) : ViewGroupHierarchyChangeEvent()