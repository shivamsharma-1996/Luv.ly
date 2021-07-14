package com.shivam.guftagoo.ui.walkthrough

import android.os.Bundle
import com.shivam.guftagoo.base.BaseActivity
import com.shivam.guftagoo.databinding.ActivityWelcomeBinding
import com.shivam.guftagoo.extensions.launchActivity
import com.shivam.guftagoo.ui.onboarding.OnboardingActivity
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : BaseActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        btn_phone_signin.setOnClickListener{
            overridePendingTransition(0,0)
            launchActivity<OnboardingActivity>()
        }
    }
}