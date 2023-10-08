package ru.ldralighieri.corbind.sample.core.extensions

import android.content.res.Resources
import android.util.TypedValue

val Number.toPx get() = TypedValue.applyDimension(
    /* unit = */ TypedValue.COMPLEX_UNIT_DIP,
    /* value = */ toFloat(),
    /* metrics = */ Resources.getSystem().displayMetrics
)