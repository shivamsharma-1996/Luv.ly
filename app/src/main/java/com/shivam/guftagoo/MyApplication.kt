package com.shivam.guftagoo

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.patch.patchcalling.PatchResponseCodes
import com.patch.patchcalling.interfaces.PatchInitResponse
import com.patch.patchcalling.javaclasses.PatchInitOptions
import com.patch.patchcalling.javaclasses.PatchSDK
import com.shivam.guftagoo.extensions.runOnMain
import com.shivam.guftagoo.util.Constants
import com.shivam.guftagoo.util.retrieveString
import org.json.JSONException
import org.json.JSONObject

class MyApplication: Application() {

    private val accountId = "5bce2ac24fc49415a3d67b48"
    private val apikey = "testkey"

    companion object{
        var appContext: Context? = null
            private set

        val sharedPreferences: SharedPreferences by lazy {
            appContext!!.getSharedPreferences(
                Constants.KEY_PREFERENCE_NAME,
                Context.MODE_PRIVATE
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        if (Firebase.auth.currentUser != null){
            initPatchSDK(Firebase.auth.currentUser!!)
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
                    .init(appContext, patchInitBuilder, object : PatchInitResponse {
                        override fun onSuccess(s: Int) {
                            Toast.makeText(appContext, "SDK Initialized : $s", Toast.LENGTH_LONG).show()
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
                                    Toast.makeText(appContext, "SDK failure : $s", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    })

            }
        }
    }
}