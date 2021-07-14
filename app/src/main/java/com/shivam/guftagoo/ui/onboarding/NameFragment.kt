package com.shivam.guftagoo.ui.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.FragmentNameBinding
import com.shivam.guftagoo.databinding.FragmentOtpBinding
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.SignInUser
import kotlinx.android.synthetic.main.fragment_name.*
import kotlinx.android.synthetic.main.fragment_name.btn_continue
import kotlinx.android.synthetic.main.fragment_phone.*


class NameFragment private constructor(): Fragment() {
    private lateinit var binding: FragmentNameBinding
    private val TAG = "Luv:NameFragment"

    companion object {
        @JvmStatic
        fun newInstance() = NameFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNameBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        btn_continue.setOnClickListener{
            onContinueClicked()
        }
    }

    private fun onContinueClicked() {
        val name = et_name.text.toString().trim()
        if(name.isNotEmpty()){
            SignInUser.name = name
            (activity as OnboardingActivity)
                .replaceFragment(
                    DobFragment.newInstance(),
                    R.id.containerFrame,
                    backStackTag = OtpFragment.javaClass.simpleName
                )
        }else{
            showSnack("Name is required")
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(3)
    }
}