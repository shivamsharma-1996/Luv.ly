package com.shivam.guftagoo.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.chip.Chip
import com.shivam.guftagoo.R
import kotlinx.android.synthetic.main.activity_edit_profile.*

class EditProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)


        window.statusBarColor = getColor(R.color.black)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    }

    override fun onStart() {
        super.onStart()

        val categoryChipList: List<String> = listOf("Printing", "Coding", "Chess", "cook")
        for (record in categoryChipList) {
            val mChip =
                this.layoutInflater.inflate(R.layout.item_list_category, null, false) as Chip
            mChip.text = record
            mChip.isChecked = true
            val paddingDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5f,
                resources.displayMetrics
            ).toInt()
            mChip.setPadding(paddingDp, 0, paddingDp, 0)
            chips_user_interest.addView(mChip)
        }
    }
}