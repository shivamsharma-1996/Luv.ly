package com.shivam.guftagoo.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseActivity
import com.shivam.guftagoo.databinding.ActivityOnboardingBinding
import com.shivam.guftagoo.databinding.ActivitySettingsBinding
import com.shivam.guftagoo.extensions.delayedHandler
import com.shivam.guftagoo.extensions.launchActivity
import com.shivam.guftagoo.ui.walkthrough.WelcomeActivity

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        window.statusBarColor = getColor(R.color.black)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setupUI()
    }

    private fun setupUI() {
        binding.tvLogout.setOnClickListener{
            Firebase.auth.signOut()
            showLoading()
            Handler().delayedHandler(1500){
                hideLoading()
                launchActivity<WelcomeActivity> {  }
                finish()
            }

        }
    }
}