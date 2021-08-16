package com.shivam.guftagoo.ui.onboarding

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseFragment
import com.shivam.guftagoo.daos.SignInDao
import com.shivam.guftagoo.daos.UserDao
import com.shivam.guftagoo.databinding.FragmentOtpBinding
import com.shivam.guftagoo.extensions.log
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.User
import com.shivam.guftagoo.receivers.SmsBroadcastReceiver
import com.shivam.guftagoo.receivers.SmsBroadcastReceiverListener
import com.shivam.guftagoo.util.AppUtil
import com.shivam.guftagoo.util.Constants
import com.shivam.guftagoo.util.putString
import kotlinx.android.synthetic.main.fragment_otp.*
import java.util.regex.Matcher
import java.util.regex.Pattern


private const val ARG_COUNTRY_CODE = "country_code"
private const val ARG_PHONE_NUMBER = "phone_number"
private const val ARG_STORED_VERIFICATION_ID = "verification_id"

class OtpFragment : BaseFragment(), View.OnClickListener {
    private lateinit var binding: FragmentOtpBinding
    private val TAG = "OtpFragment"

    companion object {
        @JvmStatic
        fun newInstance(cc: String, phone: String, storedVerificationId: String) =
            OtpFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COUNTRY_CODE, cc)
                    putString(ARG_PHONE_NUMBER, phone)
                    putString(ARG_STORED_VERIFICATION_ID, storedVerificationId)
                }
            }
    }

    //OTP RESEND
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    //OTP AUTO READ
    private val REQ_USER_CONSENT = 200
    private var smsBroadcastReceiver: SmsBroadcastReceiver? = null

    // get reference of the firebase auth
    private lateinit var auth: FirebaseAuth

    //vars
    private var countryCode: String? = null
    private var phoneNumber: String? = null
    private var storedVerificationId: String? = null

    //MTC
    private var totalMillisUntilFinished = 90000L
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            countryCode = it.getString(ARG_COUNTRY_CODE)
            phoneNumber = it.getString(ARG_PHONE_NUMBER)
            storedVerificationId = it.getString(ARG_STORED_VERIFICATION_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOtpBinding.inflate(inflater, container, false);
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setUpPhoneAuthCallback()
        startCountdownTimer()
        startSmartUserConsent();
        setTextWatcher()
    }

    private fun startSmartUserConsent() {
        val client = SmsRetriever.getClient(requireContext())
        client.startSmsUserConsent(null)
    }

    private fun setupUI() {
        auth = FirebaseAuth.getInstance()

        tv_phone_number.text = String.format("+%s-%s", countryCode, phoneNumber)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        log("signInWithPhoneAuthCredential", "signInWithPhoneAuthCredential")
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                hideLoading()
                if (it.isSuccessful) {
                    activity?.let { activity ->
                        countDownTimer?.cancel()
//                        val intent = Intent(activity, HomeActivity::class.java)
//                        startActivity(intent)
//                        activity.finish()

                        checkWhetherNumberExists()
                    }
                } else {
                    // Sign in failed, display a message and update the UI
                    if (it.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        showSnack("Invalid OTP!")
                    }
                }
            }
    }

    private fun checkWhetherNumberExists() {
        phoneNumber?.let {
             UserDao().checkIfUserExists(phoneNumber!!){ isUserExist, user ->
                 if(isUserExist){
                     saveUserInPrefs(user!!)
                     (activity as OnboardingActivity)
                         .replaceFragment(
                             OnboardingFinishFragment.newInstance(),
                             R.id.containerFrame,
                             backStackTag = OtpFragment.javaClass.simpleName
                         )
                 }else{
                     (activity as OnboardingActivity)
                         .replaceFragment(
                             NameFragment.newInstance(),
                             R.id.containerFrame,
                             backStackTag = OtpFragment.javaClass.simpleName
                         )
                 }
            }
        }
    }

    private fun saveUserInPrefs(currentUser: User) {
        putString(Constants.KEY_USER_ID, Firebase.auth.currentUser!!.uid)
        putString(Constants.KEY_NAME, currentUser.name)
        putString(Constants.KEY_PHONE_NUMBER, currentUser.phoneNumber)
        putString(Constants.KEY_COUNTRY_CODE, currentUser.countryCode)
        putString(Constants.KEY_GENDER, currentUser.gender)
        putString(Constants.KEY_DOB, currentUser.dob)
        putString(Constants.KEY_PROFILE_PIC_URL, currentUser.imageUrl)
    }

    private fun setTextWatcher() {
        otp_pin_view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(s: CharSequence, i: Int, i1: Int, i2: Int) {
                if (s.length == 6) {
                    Log.d("patchsharma", "verifyPhone()")
                    verifyOtp(otp_pin_view.text.toString().trim())
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    private fun verifyOtp(otp: String) {
        if (otp.isNotEmpty()) {
            storedVerificationId?.let {
                val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(
                    storedVerificationId.toString(), otp
                )
                showLoading()
                signInWithPhoneAuthCredential(credential)
            }
        } else {
            showSnack("Enter OTP first!")
        }
    }

    private fun startCountdownTimer() {
        totalMillisUntilFinished = 90000L
        countDownTimer = object : CountDownTimer(totalMillisUntilFinished, 1000) {
            //Sets 10 second remaining
            override fun onTick(millisUntilFinished: Long) {
                try {
                    totalMillisUntilFinished = millisUntilFinished
                    countdown_resend.setText("0:" + millisUntilFinished / 1000)
                    if (totalMillisUntilFinished < 10000) {
                        countdown_resend.setText("0:0" + millisUntilFinished / 1000)
                    }
                }catch (e: Exception){

                }
            }

            override fun onFinish() {
                countdown_resend.setText("0:00")
                enableResendNowBtn(true)
                context?.let { AppUtil.hideKeyboard(it) }
            }
        }.start()
    }

    private fun resendOtp() {
        enableResendNowBtn(false)
        resendNewOtp("+$countryCode$phoneNumber")
    }

    private fun setUpPhoneAuthCallback() {
        // Callback function for Phone Auth
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            // This method is called when the verification is completed
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
//                startActivity(Intent(applicationContext, MainActivity::class.java))
//                finish()
                log(TAG, "onVerificationCompleted Success")
            }

            // Called when verification is failed add log statement to see the exception
            override fun onVerificationFailed(e: FirebaseException) {
                log(TAG, "onVerificationFailed  $e")
                hideLoading()
                e.message?.let {
                    showSnack(it)
                }
            }

            // On code is sent by the firebase this method is called
            // in here we start a new activity where user can enter the OTP
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                log(TAG, "onCodeSent: $verificationId")
                storedVerificationId = verificationId
                resendToken = token
                showSnack("Otp Sent Again, please wait!")
            }
        }
    }

    private fun resendNewOtp(phoneNumber: String) {
        val signInDao = SignInDao()
        showLoading()
        signInDao.sentOtp(phoneNumber, requireActivity(), callbacks)
    }

    fun enableResendNowBtn(enable: Boolean) {
        tv_resend_now.isEnabled = enable
        if (enable)
            tv_resend_now.setTextColor(resources.getColor(R.color.colorPrimary, null))
        else tv_resend_now.setTextColor(
            resources.getColor(R.color.textSecondary, null)
        )
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_resend_now -> resendOtp()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        @Nullable data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_USER_CONSENT) {
            if (resultCode == RESULT_OK && data != null) {
                val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                if (message != null) {
                    getOtpFromMessage(message)
                }
            }
        }
    }

    private fun getOtpFromMessage(message: String) {
        val otpPattern: Pattern = Pattern.compile("(|^)\\d{6}")
        val matcher: Matcher = otpPattern.matcher(message)
        if (matcher.find()) {
            otp_pin_view.setText(matcher.group(0))
            verifyOtp(otp_pin_view.text.toString().trim())
        }
    }

    private fun registerBroadcastReceiver() {
        smsBroadcastReceiver = SmsBroadcastReceiver()
        smsBroadcastReceiver!!.smsBroadcastReceiverListener = object : SmsBroadcastReceiverListener {
            override fun onSuccess(intent: Intent?) {
                startActivityForResult(
                    intent,
                   REQ_USER_CONSENT
                )
            }

            override fun onFailure() {
                showSnack("Failed to auto read OTP!")
            }
        }
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        context?.registerReceiver(smsBroadcastReceiver, intentFilter, SmsRetriever.SEND_PERMISSION,null)
    }


    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(2)
        //registerBroadcastReceiver()
    }

    override fun onStop() {
        super.onStop()
        //context?.unregisterReceiver(smsBroadcastReceiver)
    }
}