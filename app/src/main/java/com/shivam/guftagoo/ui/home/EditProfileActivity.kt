package com.shivam.guftagoo.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import androidx.core.os.postDelayed
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.ActivityEditProfileBinding
import com.shivam.guftagoo.databinding.ActivitySettingsBinding
import com.shivam.guftagoo.util.AppUtil
import com.shivam.guftagoo.util.Constants
import com.shivam.guftagoo.util.retrieveString
import kotlinx.android.synthetic.main.activity_edit_profile.*

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = getColor(R.color.black)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        populateUI()
        setupUI()
    }

    private fun populateUI() {
        val items: List<String> = retrieveString(Constants.KEY_DOB).split("-")

        val day = items[0]
        val month= items[1]
        val year = items[2]
        val age = AppUtil.getAge(year.toInt(), month.toInt(), day.toInt())
        binding.tvUserNameAge.text = retrieveString(Constants.KEY_NAME) + ", " +age

        Glide.with(this)
            .load(retrieveString(Constants.KEY_PROFILE_PIC_URL))
            .into(binding.ivUserPic)

        Handler().postDelayed(1500){
            binding.ivPicAnchor.setImageResource(R.drawable.ic_close)
        }
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener{
            finish()
        }
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