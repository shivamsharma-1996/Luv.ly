package com.shivam.guftagoo.ui.walkthrough

import android.os.Bundle
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseActivity

class WelcomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        setupUI()
    }

    private fun setupUI() {
    }
}