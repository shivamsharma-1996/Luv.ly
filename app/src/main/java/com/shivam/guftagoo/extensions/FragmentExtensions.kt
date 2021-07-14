package com.shivam.guftagoo.extensions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.shivam.guftagoo.R
import java.lang.Exception


fun AppCompatActivity.addFragment(fragment: Fragment,
                                  frameId: Int,
                                  backStackTag: String? = null) {
    supportFragmentManager.inTransaction {
        add(frameId, fragment)
        backStackTag?.let {
            addToBackStack(fragment.javaClass.name)
        }
    }
}

fun AppCompatActivity.replaceFragment(fragment: Fragment,
                                      frameId: Int,
                                      backStackTag: String? = null) {
    supportFragmentManager.inTransaction {
        setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        replace(frameId, fragment)
        backStackTag?.let { addToBackStack(fragment.javaClass.name) }
    }
}

fun AppCompatActivity.popFragment(): Boolean{
    try {
        supportFragmentManager?.apply {
            if(backStackEntryCount !=0){
                val entry = getBackStackEntryAt(backStackEntryCount - 1)
                popBackStack(entry.name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                return true
            }else{
             return false
            }
        }
    }catch (e: Exception){
        return false
    }
    // get last entry (you can get another if needed)
    return false
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> Unit) {
    val fragmentTransaction = beginTransaction()
    fragmentTransaction.func()
    fragmentTransaction.commit()
}