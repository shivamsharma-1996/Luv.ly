package com.shivam.guftagoo.util

import android.app.Activity
import com.shivam.guftagoo.MyApplication

fun putString(key: String, value: String?) {
    val sp = MyApplication.sharedPreferences
    sp.edit().putString(key, value).apply()
}

fun retrieveString(key: String, default: String = ""): String {
    val sp = MyApplication.sharedPreferences
    return sp.getString(key, default)!!
}

fun putBoolean(key: String, value: Boolean) {
    val sp = MyApplication.sharedPreferences
    sp.edit().putBoolean(key, value).apply()
}

fun retrieveBoolean(key: String, default: Boolean = false): Boolean {
    val sp = MyApplication.sharedPreferences
    return sp.getBoolean(key, default)
}

fun putInt(key: String, value: Int) {
    val sp = MyApplication.sharedPreferences
    sp.edit().putInt(key, value).apply()
}

fun retrieveInt(key: String, default: Int = -1): Int {
    val sp = MyApplication.sharedPreferences
    return sp.getInt(key, default)
}

fun Activity.removeSharedPrefByKey(key: String) {
    val sp = MyApplication.sharedPreferences
    sp.edit().remove(key).apply()
}

fun clearPreferences() {
    val sp = MyApplication.sharedPreferences
    sp.edit().clear().apply()
}

