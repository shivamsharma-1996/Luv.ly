package com.shivam.guftagoo.base

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.shivam.guftagoo.R

open class BaseActivity : AppCompatActivity() {

    private var loadingDialog: Dialog? = null

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }*/

    fun showLoading() {
        if (loadingDialog == null) {
            loadingDialog = Dialog(this)
            loadingDialog?.setContentView(R.layout.dialog_loading)
            loadingDialog?.setCanceledOnTouchOutside(false)
            loadingDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            loadingDialog?.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        loadingDialog?.show()
    }

    fun hideLoading() {
        loadingDialog?.let {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }
}