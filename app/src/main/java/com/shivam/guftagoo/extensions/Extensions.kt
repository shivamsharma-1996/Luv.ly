package com.shivam.guftagoo.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.shivam.guftagoo.BuildConfig
import com.shivam.guftagoo.R
import kotlinx.android.synthetic.main.snack_view.view.*


fun log(tag: String, message: String = "__") {
    if (BuildConfig.DEBUG) {
        Log.i(tag, message)
    }
}


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


fun Activity.showSnack(message: String) {
    runOnMain {
        if (isRunning()) {
            var isClosed = false
            var isGame = false

            val rootView = findViewById<View>(android.R.id.content).rootView as ViewGroup
            val snackView = layoutInflater.inflate(R.layout.snack_view, null)
            rootView.findViewWithTag<View>("snack")?.let {
                it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.top_exit))
                rootView.removeView(it)
            }

            snackView.message_tv.text = message
            rootView.addView(snackView)

            snackView.mainView.padding(top = getStatusBarHeight())

            snackView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.top_enter))

            Handler().postDelayed({
                if (!isClosed && isRunning() && snackView.isAttachedToWindow)
                    snackView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.top_exit))
                rootView.removeView(snackView)
            }, (if (isGame) 3000L else 5000L))
        }
    }
}

fun Context.getStatusBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}


fun Fragment.showSnack(message: String) {
    if (isActive()) {
        activity?.showSnack(message)
    }
}

fun Fragment.isActive(): Boolean = !(isRemoving || isDetached)

fun Handler.delayedHandler(delay: Int, runnable: () -> Unit) {
    this.postDelayed(runnable, delay.toLong())
}


fun String.capitalizeWords(): String = split(" ").map { it.capitalize() }.joinToString(" ")
