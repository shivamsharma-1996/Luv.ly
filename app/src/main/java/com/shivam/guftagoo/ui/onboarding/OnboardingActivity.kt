package com.shivam.guftagoo.ui.onboarding

import android.os.Bundle
import com.shivam.guftagoo.base.BaseActivity
import com.shivam.guftagoo.databinding.ActivityOnboardingBinding

class OnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}