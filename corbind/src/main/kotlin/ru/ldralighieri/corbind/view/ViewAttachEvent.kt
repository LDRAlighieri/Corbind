package ru.ldralighieri.corbind.view

import android.view.View

// -----------------------------------------------------------------------------------------------

sealed class ViewAttachEvent {
    abstract val view: View
}

data class ViewAttachAttachedEvent(
        override val view: View
) : ViewAttachEvent()

data class ViewAttachDetachedEvent(
        override val view: View
) : ViewAttachEvent()