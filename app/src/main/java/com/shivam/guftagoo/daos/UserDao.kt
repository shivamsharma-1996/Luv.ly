package com.shivam.guftagoo.daos

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import com.shivam.guftagoo.extensions.log
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.User
import com.shivam.guftagoo.models.UserVideos
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream


class UserDao {
    private val db = FirebaseFirestore.getInstance()
    private val storageRef = Firebase.storage.reference
    private val userCollection = db.collection("users")

    fun checkIfUserExists(
        number: String,
        onCompletionHandler: (Boolean, User?) -> Unit
    ) {
        GlobalScope.launch {
            userCollection.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null) {
                        val usersList = task.result!!.toObjects(User::class.java)
                        for (user in usersList) {
                            if (user.phoneNumber == number) {
                                onCompletionHandler(true, user)
                                break
                            }
                        }
                    }
                } else {
                    onCompletionHandler(false, null)
                }
            }
        }
    }

    fun addUser(user: User, onCompletionHandler: () -> Unit) {
        user?.let {
            CoroutineScope(Dispatchers.IO).launch {
                userCollection.document(user.uid).set(it).await()
                onCompletionHandler()
            }
        }
    }

    fun addVideosToUserModel(
        videoUrl: String,
        onCompletionHandler: (error: String?) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            userCollection.document(Firebase.auth.currentUser!!.uid).update("videos", FieldValue.arrayUnion(videoUrl))
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onCompletionHandler(null)
                        log("addVideosToUserModel", "success!")
                    } else {
                        onCompletionHandler(task.exception.toString())
                        log("addVideosToUserModel", "failure!")
                    }
                }
        }
    }


    fun fetchUsers(
        onCompletionHandler: (MutableList<User>?, error: String?) -> Unit
    ) {
        GlobalScope.launch {
            userCollection.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null) {
                        val userList = task.result!!.toObjects(User::class.java)!!
                        onCompletionHandler(userList, null)
                    }
                } else {
                    onCompletionHandler(null, task.exception!!.message)
                }
            }
        }
    }

    fun uploadUserProfilePic(
        activity: Activity?,
        bitmap: Bitmap,
        uid: String,
        onCompletionHandler: (String) -> Unit
    ) {
        val imagesRef = storageRef.child("images/$uid.jpg")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data: ByteArray = baos.toByteArray()

        val uploadTask = imagesRef.putBytes(data)
        uploadTask.addOnFailureListener { failure ->
            activity?.let {
                failure.message?.let { message -> activity.showSnack(message) }
            }
            // Handle unsuccessful uploads
        }.addOnSuccessListener { taskSnapshot ->
            CoroutineScope(Dispatchers.IO).launch {
                val downloadUri: Uri? = taskSnapshot.storage.downloadUrl.await()
                if (downloadUri != null) {
                    log("## Stored path is $downloadUri")
                    onCompletionHandler(downloadUri.toString())
                }
            }
            activity?.showSnack("Uploaded!")
        }
    }


    fun fetchListOfVideoUris(
        onCompletionHandler: (List<UserVideos>?, error: String?) -> Unit
    ) {
        val mReference = storageRef.child("videos/${Firebase.auth.currentUser!!.uid}")
        CoroutineScope(Dispatchers.IO).launch {
            mReference.listAll()
                .addOnSuccessListener { (items, prefixes) ->
                    var videoUriList = ArrayList<UserVideos>()

                    CoroutineScope(Dispatchers.IO).launch {
                        items.map {
                            async {
                                val url = it.downloadUrl.await()
                                videoUriList.add(UserVideos(url.toString(), false))
                            }
                        }.forEach {
                            it.await()
                        }
                        withContext(Dispatchers.Main){
                            onCompletionHandler(videoUriList, null)
                        }
                    }
                }
                .addOnFailureListener {
                    onCompletionHandler(null, it.message)
                }
        }
    }


    fun fetchDefaultMedia(onCompletionHandler: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            //Getting default selected media for callScreens
            val defaultMediaRef = userCollection.
            document("${Firebase.auth.currentUser!!.uid}").get()
                .await()

            withContext(Dispatchers.Main){
                val defaultVideoUrl = defaultMediaRef.getString("defaultMediaUrl")
                if(defaultVideoUrl!=null){
                    onCompletionHandler(defaultVideoUrl)
                }else{
                    onCompletionHandler(null)
                }
            }
        }
    }

    fun makeVideoAsDefault(videoUrl: String, onCompletionHandler: (String) -> Unit){
        CoroutineScope(Dispatchers.IO).launch {
            //Setting videoUrl as defaultMediaUrl for callScreens
            userCollection
                .document(Firebase.auth.currentUser!!.uid).update("defaultMediaUrl", videoUrl)
                .await()

            withContext(Dispatchers.Main){
                onCompletionHandler(videoUrl)
            }
        }
    }

    fun handleDefaultMediaUrl(defaultVideoUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUserReference = userCollection.document(Firebase.auth.currentUser!!.uid)
            val documentSnapShot = currentUserReference.get().await()
            if(documentSnapShot.getString("defaultMediaUrl")==null){
                currentUserReference.update("defaultMediaUrl", defaultVideoUrl).await()
            }
        }
    }
}