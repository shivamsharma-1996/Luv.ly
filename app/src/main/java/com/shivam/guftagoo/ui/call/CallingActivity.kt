package com.shivam.guftagoo.ui.call

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.shivam.guftagoo.R
import kotlinx.android.synthetic.main.activity_calling.*
import kotlinx.android.synthetic.main.fragment_account.*
import java.util.*


class CallingActivity : AppCompatActivity(), View.OnClickListener, View.OnTouchListener {
    var dX = 0f
    var dY = 0f
    var lastAction = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calling)


        playVideo()
        animateCallPickUpController()

        fab_pick_up.setOnTouchListener(this)
    }

     private fun animateCallPickUpController() {
         val animShake = AnimationUtils.loadAnimation(this, R.anim.vibrate)
         fab_pick_up.startAnimation(animShake)

         Timer().schedule(object : TimerTask() {
             override fun run() {
                 fab_pick_up.animate().scaleX(0.5f).scaleY(0.5f).setDuration(500).withEndAction {
                     fab_pick_up.animate().scaleX(1f).scaleY(1f).setDuration(500)
                 }
             }
         }, 1000, 3500)
     }

     private fun playVideo() {
         val path =
             "android.resource://" + getPackageName() + "/" + R.raw.reel
         videoView.setVideoURI(Uri.parse(path))
         videoView.setOnPreparedListener { mp ->
             if (videoView != null) videoView.start()
             mp.isLooping = true
         }

         videoView.setOnInfoListener(MediaPlayer.OnInfoListener { _, i, _ ->
             if (i == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                 //first frame was bufered - do your stuff here
                 if (videoView != null) videoView.setZOrderOnTop(false)
                 return@OnInfoListener true
             }
             false
         })
         videoView.setOnErrorListener(MediaPlayer.OnErrorListener { _, _, _ ->
             if (videoView != null) videoView.setVisibility(View.INVISIBLE)
             true
         })
     }

     override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
         when (event!!.actionMasked) {
             MotionEvent.ACTION_DOWN -> {
                 dX = fab_pick_up!!.x - event!!.rawX
                 dY = fab_pick_up.y - event!!.rawY
                 lastAction = MotionEvent.ACTION_DOWN
             }
             MotionEvent.ACTION_MOVE -> {
                 fab_pick_up!!.y = event!!.rawY + dY
                 fab_pick_up.x = event!!.rawX + dX
                 lastAction = MotionEvent.ACTION_MOVE
             }
             MotionEvent.ACTION_UP -> if (lastAction === MotionEvent.ACTION_DOWN)
                 Toast.makeText(
                     this,
                     "Clicked!",
                     Toast.LENGTH_SHORT
                 ).show()
             else -> return false
         }
         return super.dispatchTouchEvent(event)

     }

     override fun onClick(v: View?) {
     }

     override fun onTouch(v: View?, event: MotionEvent?): Boolean {
         return false
     }
}