package com.shivam.guftagoo.ui.onboarding

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.shivam.guftagoo.R
import kotlinx.android.synthetic.main.fragment_interest.*

class InterestFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_interest, container, false)
    }

    override fun onStart() {
        super.onStart()

        val categoryChipList: List<String> = listOf("printing", "Coding", "Chess", "cook", "Blogging", "Chess", "Graphic design",
        "Photography", "Stock trading","sleep", "Video editing", "Video game", "Coding", "Chess", "Graphic design",
            "Photography", "Stock trading", "Video game", "Coding", "Cricket", "football", "Video editing")
        for (record in categoryChipList) {
            val mChip =
                this.layoutInflater.inflate(R.layout.item_list_category, null, false) as Chip
            mChip.text = record
            val paddingDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5f,
                resources.displayMetrics
            ).toInt()
            mChip.setPadding(paddingDp, 0, paddingDp, 0)
            chips_user_interest.addView(mChip)
        }
    }

}