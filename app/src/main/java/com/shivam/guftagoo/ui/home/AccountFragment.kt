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
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.shivam.guftagoo.base.BaseFragment
import com.shivam.guftagoo.daos.UserDao
import com.shivam.guftagoo.databinding.FragmentAccountBinding
import com.shivam.guftagoo.extensions.launchActivity
import com.shivam.guftagoo.extensions.log
import com.shivam.guftagoo.extensions.runOnMain
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.ui.call.UserMediaAdapter
import com.shivam.guftagoo.util.AppUtil
import com.shivam.guftagoo.util.Constants
import com.shivam.guftagoo.util.retrieveString
import kotlinx.android.synthetic.main.fragment_account.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AccountFragment private constructor() : BaseFragment(), Player.EventListener {
    private lateinit var binding: FragmentAccountBinding

    private lateinit var userMediaAdapter: UserMediaAdapter

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
        setupUI()
        dummyCode()
        fetchVideos()
    }

    private fun populateUI() {

        val items: List<String> = retrieveString(Constants.KEY_DOB).split("-")

        val day = items[0]
        val month = items[1]
        val year = items[2]
        val age = AppUtil.getAge(year.toInt(), month.toInt(), day.toInt())
        binding.tvUserName.text = retrieveString(Constants.KEY_NAME) + ", " + age

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

    private fun dummyCode() {
        userMediaAdapter = UserMediaAdapter{ videoUrl ->
            //playInExoPlayer()
            (requireActivity() as HomeActivity_new).launchActivity<VideoActivity> {
                putExtra("videoUrl", videoUrl)
            }
        }
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

                        runOnMain {
                            log("URI123:$downloadUri")
                            showSnack("Successfully Uploaded :)")
                            hideLoading()
                            fetchVideos()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchVideos() {
        val userDao = UserDao()
        progress_bar.visibility = VISIBLE
        userDao.fetchListOfVideoUris(
            requireActivity(),
            Firebase.auth.currentUser!!.uid
        ) { videoUriList, error ->
            runOnMain {
                progress_bar.visibility = GONE
                if (error != null) {
                    showSnack(error)
                } else {
                    videoUriList?.let {
                        if(it.isEmpty()){
                            tv_empty_list_msg.visibility = VISIBLE
                        }else{
                            tv_empty_list_msg.visibility = GONE
                            userMediaAdapter.submitData(videoUriList)
                        }
                    }
                }
            }
        }
    }
}