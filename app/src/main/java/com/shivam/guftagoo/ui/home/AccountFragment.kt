package com.shivam.guftagoo.ui.home

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.shivam.guftagoo.R
import com.shivam.guftagoo.base.BaseFragment
import com.shivam.guftagoo.daos.UserDao
import com.shivam.guftagoo.databinding.FragmentAccountBinding
import com.shivam.guftagoo.extensions.launchActivity
import com.shivam.guftagoo.extensions.log
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.UserVideos
import com.shivam.guftagoo.ui.call.UserMediaAdapter
import com.shivam.guftagoo.util.AppUtil
import com.shivam.guftagoo.util.Constants
import com.shivam.guftagoo.util.retrieveString
import kotlinx.android.synthetic.main.fragment_account.*
import kotlinx.android.synthetic.main.sheet_make_video_default.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class AccountFragment private constructor() : BaseFragment(), Player.EventListener,
    View.OnClickListener {
    private lateinit var binding: FragmentAccountBinding

    private lateinit var userMediaAdapter: UserMediaAdapter
    private var videoList: List<UserVideos> = emptyList()

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val VIDEO_RECORD_CODE = 200

        @JvmStatic
        fun newInstance() = AccountFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateUI()
        initUI()
        setupUI()
        fetchVideos()
    }

    private fun populateUI() {

        val items: List<String> = retrieveString(Constants.KEY_DOB).split("-")


        try {
            val day = items[0]
            val month = items[1]
            val year = items[2]
            val age = AppUtil.getAge(year.toInt(), month.toInt(), day.toInt())
            binding.tvUserName.text = retrieveString(Constants.KEY_NAME) + ", " + age
        } catch (e: java.lang.Exception) {

        }

        val phoneNumber = String.format(
            "%s-%s",
            retrieveString(Constants.KEY_COUNTRY_CODE),
            retrieveString(Constants.KEY_PHONE_NUMBER)
        )
        // binding.tvUserCountry.text = phoneNumber
        Glide.with(requireContext())
            .load(retrieveString(Constants.KEY_PROFILE_PIC_URL))
            .into(binding.ivUserPic)
    }

    private fun setupUI() {
        fab_settings.setOnClickListener {
            requireActivity().launchActivity<SettingsActivity>()
        }
        fab_edit_profile.setOnClickListener {
            requireActivity().launchActivity<EditProfileActivity>()
        }
        fab_add_media.setOnClickListener {
            //launch video capture intent
            if (isCameraPresentInPhone()) {
                getCameraPermission()
            }
        }
    }

    private fun initUI() {
        userMediaAdapter = UserMediaAdapter(this)
        rec_videos.layoutManager = GridLayoutManager(context, 3)
        rec_videos.adapter = userMediaAdapter
    }

    private fun isCameraPresentInPhone(): Boolean {
        return requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun getCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            recordVideo()
        }
    }

    private fun recordVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, VIDEO_RECORD_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VIDEO_RECORD_CODE) {
            if (resultCode == RESULT_OK) {
                val videoPath = data?.data
                log("Video path: $videoPath")

                if (videoPath != null)
                    upload(videoPath)
            } else if (resultCode == RESULT_CANCELED) {
                showSnack("Recording cancelled")
            } else {
                showSnack("Some error in recording")
            }
        }
    }

    private fun upload(uri: Uri) {
        val storage = Firebase.storage.reference

        val mReference =
            storage.child("videos/${Firebase.auth.currentUser!!.uid}/${uri.lastPathSegment!!}")
        try {
            showLoading()
            mReference.putFile(uri).addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->

                CoroutineScope(Dispatchers.IO).launch {
                    val downloadUri: Uri? = taskSnapshot.storage.downloadUrl.await()
                    if (downloadUri != null) {

                        //if defaultMediaUrl field does not exists for currentUserId, make the downloadUri as defaultMediaUrl
                        UserDao().handleDefaultMediaUrl(downloadUri.toString())
                        withContext(Dispatchers.Main) {
                            log("URI123:$downloadUri")
                            showSnack("Successfully Uploaded :)")
                            hideLoading()
                            UserDao().addVideosToUserModel(downloadUri.toString()) { error ->
                                if (error != null) {
                                    showSnack(error)
                                    return@addVideosToUserModel
                                }
                                fetchVideos()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchVideos() {
        try {
            val userDao = UserDao()
            progress_bar.visibility = VISIBLE
            userDao.fetchListOfVideoUris(
            ) { videoUriList, error ->
                try {
                    progress_bar?.visibility = GONE
                    if (error != null) {
                        showSnack(error)
                    } else {
                        videoUriList?.let {
                            try {
                                if (it.isEmpty()) {
                                    tv_empty_list_msg?.visibility = VISIBLE
                                } else {
                                    tv_empty_list_msg.visibility = GONE
                                    videoList = videoUriList

                                    userDao.fetchDefaultMedia { defaultMediaUrl ->
                                        videoUriList.map { userVideo ->
                                            if (userVideo.videoUrl == defaultMediaUrl)
                                                userVideo.isDefault = true
                                        }

                                        userMediaAdapter.submitData(videoUriList)
                                    }
                                }
                            } catch (e: Exception) {

                            }
                        }
                    }
                } catch (e: Exception) {

                }
            }
        } catch (e: Exception) {
            showSnack(e.message.toString())
        }
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.item_user_media -> {
                playVideo(v)
            }
            R.id.iv_more -> {
                showBottomSheet(v)

            }
        }
    }

    private fun playVideo(view: View) {
        val videoUrl: String = videoList[view.tag as Int].videoUrl
        (requireActivity() as HomeActivity_new).launchActivity<VideoActivity> {
            putExtra("videoUrl", videoUrl)
        }
    }

    private fun showBottomSheet(view: View) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.sheet_make_video_default, null)
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialog)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        dialogView.tv_make_as_default.setOnClickListener {
            progress_bar.visibility = VISIBLE

            val videoUrl: String = videoList[view.tag as Int].videoUrl
            UserDao().makeVideoAsDefault(videoUrl) { defaultMediaUrl ->
                progress_bar.visibility = GONE

                try {
                    videoList.map { userVideo ->
                        userVideo.isDefault = userVideo.videoUrl == defaultMediaUrl
                    }
                    userMediaAdapter.submitData(videoList)
                    dialog.dismiss()
                } catch (e: Exception) {
                    showSnack(e.message.toString())
                }
            }
        }
        dialog.show()
    }
}