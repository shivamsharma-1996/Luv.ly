package com.shivam.guftagoo.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.FragmentProfilePicBinding
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.ui.home.HomeActivity_new
import kotlinx.android.synthetic.main.fragment_profile_pic.*

class ProfilePicFragment private constructor(): Fragment() {

    private lateinit var binding: FragmentProfilePicBinding
    private val TAG = "Luv:ProfilePicFragment"

    private val CAMERA_REQUEST_CODE = 0

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
        }
        fl_user_pic.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (callCameraIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
            }
        }
        btn_continue.setOnClickListener{
            requireContext().startActivity(Intent(activity, HomeActivity_new::class.java))
            requireActivity().finish()
        }
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
                    iv_user_pic.setImageBitmap(data.extras?.get("data") as Bitmap)
                    iv_pic_anchor.setImageResource(R.drawable.ic_close)
                }
            }
            else -> {
                showSnack("Unrecognized request code")
            }
        }
    }
}