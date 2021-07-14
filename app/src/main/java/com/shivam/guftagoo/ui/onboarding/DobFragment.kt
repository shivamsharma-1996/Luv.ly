package com.shivam.guftagoo.ui.onboarding

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.FragmentDobBinding
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.SignInUser
import kotlinx.android.synthetic.main.fragment_dob.*
import java.text.SimpleDateFormat
import java.util.*

class DobFragment private constructor(): Fragment() {

    private lateinit var binding: FragmentDobBinding
    private val TAG = "Luv:DobFragment"
    private var isDobSelected = false
    private var dob: String? = null

    companion object {
        @JvmStatic
        fun newInstance() = DobFragment()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDobBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        tv_dob.setOnClickListener {

            val cal = Calendar.getInstance()
            val dateSetListener =
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, monthOfYear)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    dob = "$dayOfMonth-$monthOfYear-$year"
                    showSnack("You Selected: $dob")
                    isDobSelected = true

                    tv_dob.text = dob
                }

            DatePickerDialog(
                requireContext(), dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btn_continue.setOnClickListener{
            onContinueClicked()
        }
    }

    private fun onContinueClicked() {
        if(isDobSelected){
            SignInUser.dob = dob!!

            (activity as OnboardingActivity)
                .replaceFragment(
                    GenderFragment.newInstance(),
                    R.id.containerFrame,
                    backStackTag = OtpFragment.javaClass.simpleName
                )
        } else{
            showSnack("Dob is required")
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(4)
    }
}