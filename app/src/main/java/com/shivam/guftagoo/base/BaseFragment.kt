package com.shivam.guftagoo.base

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.shivam.guftagoo.R
import java.lang.Exception

open class BaseFragment : Fragment(){
    private var loadingDialog: Dialog? = null

    fun showLoading() {
        try {
            if (loadingDialog == null) {
                loadingDialog = Dialog(requireContext())
                loadingDialog?.setContentView(R.layout.dialog_loading)
                loadingDialog?.setCanceledOnTouchOutside(false)
                loadingDialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                loadingDialog?.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            loadingDialog?.show()
        }catch (e:Exception){

        }
    }

    fun hideLoading() {
        try {
            loadingDialog?.let {
                loadingDialog?.dismiss()
                loadingDialog = null
            }
        }catch (e:Exception){

        }
    }
}