package com.shivam.guftagoo.ui.onboarding

import android.os.Bundle
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseActivity
import com.shivam.guftagoo.databinding.ActivityOnboardingBinding
import com.shivam.guftagoo.extensions.addFragment
import com.shivam.guftagoo.extensions.popFragment
import kotlinx.android.synthetic.main.activity_onboarding.*

class OnboardingActivity : BaseActivity() {

    private val phoneFragment by lazy { PhoneFragment.newInstance()}
    private val processDoneFragment by lazy {  InterestFragment.newInstance()}

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        phoneFragment.arguments = Bundle()
        addFragment(phoneFragment, R.id.containerFrame)

        iv_back.setOnClickListener{
            val wasPopupSuccessful = popFragment()
            if(!wasPopupSuccessful){
                finish()  //when no user would be on very first fragment this condition will be satisfied
            }
        }
    }

    fun updateStepProgress(stepCount : Int){
        pb_steps.progress = stepCount
    }

}
