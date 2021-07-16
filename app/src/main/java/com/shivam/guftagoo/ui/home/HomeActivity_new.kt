package com.shivam.guftagoo.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseActivity
import com.shivam.guftagoo.databinding.ActivityHomeBinding
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.ui.walkthrough.WelcomeActivity
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
            var destinationFragment: Fragment? = null
            if(home_bottom_menu.selectedItemId == item.itemId){
                 false
            }else{
                when (item.itemId) {
                    R.id.account ->{
                        destinationFragment = AccountFragment.newInstance()
                    }
                    R.id.home ->{
                        destinationFragment = HomeFragment.newInstance()

                    }
                    R.id.superlike -> {
                        destinationFragment = HomeFragment.newInstance()

                    }
                }
                replaceFragment(destinationFragment!!, R.id.fragment_container)

                true
            }
        }
    }
}