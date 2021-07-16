package com.shivam.guftagoo.ui.onboarding

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.FragmentInterestBinding
import com.shivam.guftagoo.extensions.enable
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.models.SignInUser
import kotlinx.android.synthetic.main.fragment_interest.*

class InterestFragment private constructor(): Fragment() {

    private lateinit var binding: FragmentInterestBinding
    private val TAG = "Luv:InterestFragment"

    companion object {
        @JvmStatic
        fun newInstance() = InterestFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInterestBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        populateUI()
    }

    private fun initUI() {
        btn_continue.setOnClickListener{
            setSelectedInterests()
        }
    }

    private fun setSelectedInterests() {
        var interestList = ArrayList<String>()
        chips_user_interest.children
            .toList()
            .filter { (it as Chip).isChecked }
            .forEach {
                interestList.add((it as Chip).text.toString())
            }
        SignInUser.interestList.addAll(interestList)

        (activity as OnboardingActivity)
            .replaceFragment(
                ProfilePicFragment.newInstance(),
                R.id.containerFrame,
                backStackTag = OtpFragment.javaClass.simpleName
            )
    }

    private fun populateUI() {
        val categoryChipList: List<String> = listOf(
            "printing",
            "Coding",
            "Chess",
            "cook",
            "Blogging",
            "Chess",
            "Graphic design",
            "Photography",
            "Stock trading",
            "sleep",
            "Video editing",
            "Video game",
            "Coding",
            "Chess",
            "Graphic design",
            "Photography",
            "Stock trading",
            "Video game",
            "Coding",
            "Cricket",
            "football",
            "Video editing"
        )
        for (record in categoryChipList) {
            val mChip =
                this.layoutInflater.inflate(R.layout.item_list_category, null, false) as Chip
            mChip.text = record
            val paddingDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5f,
                resources.displayMetrics
            ).toInt()
            mChip.setOnClickListener{
                binding.btnContinue.enable(requireActivity(), enable = true)
            }
            mChip.setPadding(paddingDp, 0, paddingDp, 0)
            chips_user_interest.addView(mChip)
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(6)
    }

}