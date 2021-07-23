package com.shivam.guftagoo.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.patch.patchcalling.PatchResponseCodes
import com.patch.patchcalling.interfaces.PatchInitResponse
import com.patch.patchcalling.javaclasses.PatchInitOptions
import com.patch.patchcalling.javaclasses.PatchSDK
import com.shivam.guftagoo.BuildConfig
import com.shivam.guftagoo.MyApplication
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseActivity
import com.shivam.guftagoo.databinding.ActivityHomeBinding
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.extensions.runOnMain
import com.shivam.guftagoo.ui.walkthrough.WelcomeActivity
import com.shivam.guftagoo.util.Constants
import com.shivam.guftagoo.util.retrieveString
import kotlinx.android.synthetic.main.activity_home.*
import org.json.JSONException
import org.json.JSONObject

class HomeActivity_new : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val accountId = "5bce2ac24fc49415a3d67b48"
    private val apikey = "testkey"


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
        }else{
              initPatchSDK(Firebase.auth.currentUser!!)

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

    private fun initPatchSDK(currentUser: FirebaseUser) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isComplete) {
                val fcmToken = it.result.toString()
                val options = JSONObject()
                val missedCallActions = JSONObject()
                try {
                    options.put("accountID", accountId)
                    options.put("apikey", apikey)
                    options.put("profileUrl", retrieveString(Constants.KEY_PROFILE_PIC_URL))
                    options.put("cc", retrieveString(Constants.KEY_COUNTRY_CODE).replace("+", ""))
                    options.put("phone", retrieveString(Constants.KEY_PHONE_NUMBER))
                    options.put("name", retrieveString(Constants.KEY_NAME))
                    options.put("appId", BuildConfig.APPLICATION_ID)
                    options.put("cuid", currentUser.uid)
                    options.put("fcmToken", fcmToken)
                    missedCallActions.put("3", "dismiss")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                val patchInitBuilder = PatchInitOptions.Builder(options)
                    .build()

                PatchSDK.getInstance()
                    .init(MyApplication.appContext, patchInitBuilder, object : PatchInitResponse {
                        override fun onSuccess(s: Int) {
                            Toast.makeText(MyApplication.appContext, "SDK Initialized : $s", Toast.LENGTH_LONG).show()
                        }

                        override fun onFailure(s: Int) {
                            runOnMain {
                                if (s == PatchResponseCodes.ERR_NETWORK_NOT_AVAILABLE) {
                                    Toast.makeText(
                                        applicationContext,
                                        "No Internet Connection",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(MyApplication.appContext, "SDK failure : $s", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    })

            }
        }
    }

}