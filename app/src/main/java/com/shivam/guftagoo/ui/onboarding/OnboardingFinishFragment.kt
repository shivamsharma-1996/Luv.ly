package com.shivam.guftagoo.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shivam.guftagoo.databinding.FragmentProcessDoneBinding
import com.shivam.guftagoo.extensions.delayedHandler
import com.shivam.guftagoo.ui.home.HomeActivity_new

class OnboardingFinishFragment : Fragment() {

    private lateinit var binding: FragmentProcessDoneBinding
    private val TAG = "Luv:ProcessDoneFragment"

    companion object {
        @JvmStatic
        fun newInstance() = OnboardingFinishFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProcessDoneBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(8)

        Handler().delayedHandler(2500){
            activity?.let {
                val intent = Intent(requireContext(), HomeActivity_new::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                requireActivity().startActivity(intent)

                requireActivity().finish()
            }
        }
    }
}