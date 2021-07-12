package com.shivam.guftagoo.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.shivam.guftagoo.R


inline fun <reified T : Any> Context.launchActivity(
    options: Bundle? = null,
    noinline init: Intent.() -> Unit = {}
) {
    val intent = newIntent<T>(this)
    intent.init()
    startActivity(intent, options)
}

inline fun <reified T : Any> newIntent(context: Context): Intent =
    Intent(context, T::class.java)

fun Activity?.isRunning(): Boolean = if (this == null) false else !(isDestroyed || isFinishing)

fun Any?.isActivityRunning(): Boolean = if (this == null) false else when {
    this is Activity -> isRunning()
    this is Fragment -> if (activity == null) false else activity!!.isRunning()
    else -> false
}

fun Activity.runOnUiThreadIfRunning(code: () -> Unit) {
    if (isRunning()) {
        Handler(Looper.getMainLooper()).post {
            code()
        }
    }
}

val mainHandler = Handler(Looper.getMainLooper())
inline fun runOnMain(crossinline code: () -> Unit) {
    mainHandler.post {
        code()
    }
}

fun Window.changeStatusBarColor(color: Int, windowLightStatusBar: Boolean) {
    //Pass value of windowLightStatusBar,
    // white, if statusBarColor is light. ex: White
    //black , if statusBarColor is dark. ex: Black
    statusBarColor = color
    WindowInsetsControllerCompat(this, this.decorView).isAppearanceLightStatusBars = windowLightStatusBar
}

