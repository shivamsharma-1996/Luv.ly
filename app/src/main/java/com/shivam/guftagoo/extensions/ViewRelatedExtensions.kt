package com.shivam.guftagoo.extensions

import android.view.View

fun View.padding(all: Int = 0, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): View {
    if (all != 0) {
        setPadding(all, all, all, all)
    } else {
        setPadding(left, top, right, bottom)
    }
    return this
}