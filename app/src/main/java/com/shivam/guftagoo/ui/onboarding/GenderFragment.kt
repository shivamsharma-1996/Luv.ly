package com.shivam.guftagoo.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.FragmentGenderBinding
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.models.SignInUser
import kotlinx.android.synthetic.main.fragment_gender.*

class GenderFragment private constructor(): Fragment() {
    private lateinit var binding: FragmentGenderBinding
    private val TAG = "Luv:GenderFragment"

    private var gender: String? = null

    companion object {
        @JvmStatic
        fun newInstance() = GenderFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenderBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        btn_woman.setOnClickListener{
            onGenderSelected("Woman")
        }
        btn_man.setOnClickListener{
            onGenderSelected("Man")
        }
        btn_others.setOnClickListener{
            onGenderSelected("Others")
        }
    }

    private fun onGenderSelected(gender: String) {
        SignInUser.gender = gender
        (activity as OnboardingActivity)
            .replaceFragment(
                InterestFragment.newInstance(),
                R.id.containerFrame,
                backStackTag = OtpFragment.javaClass.simpleName
            )
    }

    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(5)
    }
}