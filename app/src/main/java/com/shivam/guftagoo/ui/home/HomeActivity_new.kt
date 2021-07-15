package com.shivam.guftagoo.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseActivity
import com.shivam.guftagoo.databinding.ActivityHomeBinding
import com.shivam.guftagoo.extensions.addFragment
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.ui.walkthrough.WelcomeActivity
import com.yuyakaido.android.cardstackview.*
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity_new : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding

    private val TAG = "HomeActivity_new"
    private var adapter: CardStackAdapter? = null

    private val homeFragment by lazy { HomeFragment.newInstance()}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateUI(Firebase.auth.currentUser)

        replaceFragment(homeFragment, R.id.fragment_container)

        window.statusBarColor = getColor(R.color.black)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    }

    private fun updateUI(firebaseUser: FirebaseUser?) {
        if(firebaseUser ==null){
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        home_bottom_menu.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.account ->{
                    selectedFragment = AccountFragment.newInstance()
                }
                R.id.home ->{
                    selectedFragment = HomeFragment.newInstance()

                }
                R.id.superlike -> {
                    selectedFragment = HomeFragment.newInstance()

                }
            }
            replaceFragment(selectedFragment!!, R.id.fragment_container)

            true
        }
    }

}