package com.shivam.guftagoo.daos

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class SignInDao {
    private val auth = Firebase.auth

    fun sentOtp(phoneNumber: String, activity: FragmentActivity, otpVerificationCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        val options = let {
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(activity)                 // Activity (for callback binding)
                .setCallbacks(otpVerificationCallback)          // OnVerificationStateChangedCallbacks
                .build()
        }
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


}