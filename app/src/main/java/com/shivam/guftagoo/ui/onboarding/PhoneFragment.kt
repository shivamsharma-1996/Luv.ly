package com.shivam.guftagoo.ui.onboarding

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseFragment
import com.shivam.guftagoo.daos.SignInDao
import com.shivam.guftagoo.databinding.FragmentPhoneBinding
import com.shivam.guftagoo.extensions.enable
import com.shivam.guftagoo.extensions.log
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.SignInUser
import com.shivam.guftagoo.util.AppUtil
import kotlinx.android.synthetic.main.fragment_phone.*

class PhoneFragment private constructor() : BaseFragment(){
    private lateinit var binding: FragmentPhoneBinding

    // we will use this to match the sent otp from firebase
    lateinit var storedVerificationId:String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    //vars
    private lateinit var countryCode: String
    private lateinit var phoneNumber: String

    companion object {
        private const val TAG = "Luv:PhoneFragment"
        private const val CREDENTIAL_PICKER_REQUEST = 1

        @JvmStatic
        fun newInstance() = PhoneFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPhonePicker()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhoneBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        setTextWatcher()
        setUpPhoneAuthCallback()
    }

    private fun initUI() {
        btn_continue.setOnClickListener{
            onContinueClicked()
        }
    }
    private fun setTextWatcher() {
        binding.etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

            }

            override fun onTextChanged(s: CharSequence, i: Int, i1: Int, i2: Int) {
                if (s.isNotEmpty()) {
                    binding.btnContinue.enable(requireActivity(), enable = true)
                }else{
                    binding.btnContinue.enable(requireActivity(), enable = false)
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }


    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(1)
    }

    private fun onContinueClicked() {
        countryCode = et_country_code.text.toString().trim()
        phoneNumber = et_phone_number.text.toString().trim()

        if(countryCode.isNotEmpty() && phoneNumber.isNotEmpty()){
            SignInUser.countryCode = countryCode
            SignInUser.phoneNumber = phoneNumber
                sentOtp("+$countryCode$phoneNumber")
        }else{
            showSnack("Phone number is required!")
        }
    }

    private fun showPhonePicker() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()

        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()

        val credentialsClient = Credentials.getClient(requireActivity().applicationContext, options)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)
        try {
            startIntentSenderForResult(
                intent.intentSender,
                CREDENTIAL_PICKER_REQUEST, null, 0, 0, 0, Bundle()
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == RESULT_OK) {

            // get data from the dialog which is of type Credential
            val credential: Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)

            credential?.apply {
                val phoneNumber = AppUtil.PhoneNumberWithoutCountryCode(credential.id)
                et_phone_number.setText(phoneNumber)
            }
        }
    }

    private fun setUpPhoneAuthCallback() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                log(TAG, "onVerificationCompleted Success")
            }
            override fun onVerificationFailed(e: FirebaseException) {
                log(TAG, "onVerificationFailed  $e")
                hideLoading()
                e.message?.let {
                    showSnack(it)
                }
            }
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                log(TAG, "onCodeSent: $verificationId")
                storedVerificationId = verificationId
                resendToken = token

                hideLoading()

                (activity as OnboardingActivity)
                    .replaceFragment(
                        OtpFragment.newInstance(
                            "91",
                            phoneNumber,
                            storedVerificationId
                        ),
                        R.id.containerFrame,
                        backStackTag = OtpFragment.javaClass.simpleName
                    )
            }
        }
    }

    private fun sentOtp(phoneNumber: String) {
        val signInDao = SignInDao()
        showLoading()
        signInDao.sentOtp(phoneNumber, requireActivity(), callbacks)
    }
}