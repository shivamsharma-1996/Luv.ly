package com.shivam.guftagoo.extensions

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import com.shivam.guftagoo.R

fun View.padding(all: Int = 0, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): View {
    if (all != 0) {
        setPadding(all, all, all, all)
    } else {
        setPadding(left, top, right, bottom)
    }
    return this
}

fun TextView.enable(activity: Activity, enable:Boolean){
    if(enable){
        setBackground(activity.getDrawable(R.drawable.bg_solid_round_primary))
        alpha = 1f
        setTextColor(activity.resources.getColor(R.color.white, null))
    }else{
        setBackground(activity.getDrawable(R.drawable.bg_stroke_dim_translucent))
        alpha = 0.5f
        setTextColor(activity.resources.getColor(R.color.textSecondary, null))
    }

}