package ru.ldralighieri.corbind.internal

import androidx.annotation.RestrictTo

// -----------------------------------------------------------------------------------------------

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AlwaysTrue : () -> Boolean, (Any) -> Boolean {
    override fun invoke() = true
    override fun invoke(ignored: Any) = true
}
