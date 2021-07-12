package com.shivam.guftagoo.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.chip.Chip
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.ActivityHomeBinding
import com.shivam.guftagoo.databinding.ActivityOnboardingBinding
import com.shivam.guftagoo.extensions.changeStatusBarColor
import kotlinx.android.synthetic.main.fragment_interest.*

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = getColor(R.color.black)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    }

    override fun onStart() {
        super.onStart()

        val categoryChipList: List<String> = listOf("printing", "Coding", "Chess", "cook", "Blogging", "Chess")
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