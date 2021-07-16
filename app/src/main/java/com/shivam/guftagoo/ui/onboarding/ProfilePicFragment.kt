package com.shivam.guftagoo.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseFragment
import com.shivam.guftagoo.daos.UserDao
import com.shivam.guftagoo.databinding.FragmentProfilePicBinding
import com.shivam.guftagoo.extensions.enable
import com.shivam.guftagoo.extensions.log
import com.shivam.guftagoo.extensions.replaceFragment
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.SignInUser
import com.shivam.guftagoo.models.User
import com.shivam.guftagoo.util.Constants.KEY_COUNTRY_CODE
import com.shivam.guftagoo.util.Constants.KEY_DOB
import com.shivam.guftagoo.util.Constants.KEY_GENDER
import com.shivam.guftagoo.util.Constants.KEY_NAME
import com.shivam.guftagoo.util.Constants.KEY_PHONE_NUMBER
import com.shivam.guftagoo.util.Constants.KEY_PROFILE_PIC_URL
import com.shivam.guftagoo.util.Constants.KEY_USER_ID
import com.shivam.guftagoo.util.putString
import kotlinx.android.synthetic.main.fragment_profile_pic.*


class ProfilePicFragment private constructor(): BaseFragment() {

    private lateinit var binding: FragmentProfilePicBinding
    private val TAG = "Luv:ProfilePicFragment"

    private val CAMERA_PERMISSION_CODE = 201

    private val CAMERA_REQUEST_CODE = 202

    companion object {
        @JvmStatic
        fun newInstance() = ProfilePicFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfilePicBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        iv_pic_anchor.setOnClickListener{
            iv_user_pic.setImageBitmap(null)
            iv_pic_anchor.setImageResource(R.drawable.ic_add)
            SignInUser.profilePicUrl = null
            binding.btnContinue.enable(requireActivity(), enable = false)
        }
        fl_user_pic.setOnClickListener {
            getCameraPermission()
        }
        btn_continue.setOnClickListener{
            if(SignInUser.profilePicUrl!=null){
                log(
                    "SingletonUser", SignInUser.countryCode +
                            " " + SignInUser.phoneNumber + " " + SignInUser.name + " " + SignInUser.gender
                            + " " + SignInUser.dob + " " + SignInUser.profilePicUrl + " " + SignInUser.interestList
                )
                updateUI(Firebase.auth.currentUser)
            }else{
                showSnack("Please add your image first!")
            }

            /*updateUI(Firebase.auth.currentUser)
            */
        }
    }

    private fun launchImageCaptureIntent() {
        val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (callCameraIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
        }
    }

    private fun updateUI(firebaseUser: FirebaseUser?) {
        if(firebaseUser !=null){
            val user = User()
            user.countryCode = SignInUser.countryCode
            user.name = SignInUser.name
            user.phoneNumber = SignInUser.phoneNumber
            user.uid = firebaseUser.uid
            user.dob = SignInUser.dob
            user.gender = SignInUser.gender
            user.imageUrl = SignInUser.profilePicUrl!!
            user.interestList = SignInUser.interestList

            val usersDao = UserDao()
            usersDao.addUser(user){
                saveUserInPrefs(firebaseUser.uid)
                (activity as OnboardingActivity)
                    .replaceFragment(
                        OnboardingFinishFragment.newInstance(),
                        R.id.containerFrame,
                        backStackTag = OtpFragment.javaClass.simpleName
                    )
            }
        }else{
            showSnack("Something went Wrong!")
        }
    }

    private fun saveUserInPrefs(uid: String) {
        putString(KEY_USER_ID, uid)
        putString(KEY_NAME, SignInUser.name)
        putString(KEY_PHONE_NUMBER, SignInUser.phoneNumber)
        putString(KEY_COUNTRY_CODE, SignInUser.countryCode)
        putString(KEY_GENDER, SignInUser.gender)
        putString(KEY_DOB, SignInUser.dob)
        //putString(KEY_INTERESTS, SignInUser.interestList)
        putString(KEY_PROFILE_PIC_URL, SignInUser.profilePicUrl)
    }

    override fun onStart() {
        super.onStart()
        (activity as OnboardingActivity).updateStepProgress(7)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val bitmap = data.extras?.get("data") as Bitmap
                    iv_user_pic.setImageBitmap(bitmap)
                    iv_pic_anchor.setImageResource(R.drawable.ic_close)

                    uploadProfilePicInStorage(bitmap)
                }
            }
            else -> {
                showSnack("Unrecognized request code")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    launchImageCaptureIntent()
                }
            }
        }
    }

    private fun uploadProfilePicInStorage(bitmap: Bitmap){
        Firebase.auth.currentUser?.let {
            showLoading()
            UserDao().uploadUserProfilePic(requireActivity(), bitmap, it.uid){ fileUrl ->
                hideLoading()
                SignInUser.profilePicUrl = fileUrl
                binding.btnContinue.enable(requireActivity(), enable = true)
            }
        }
    }

    private fun getCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }else{
            launchImageCaptureIntent()
        }
    }
}