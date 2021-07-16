package com.shivam.guftagoo.daos

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.shivam.guftagoo.extensions.log
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.User
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.lang.Exception
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import kotlinx.coroutines.*

class UserDao {
    private val db = FirebaseFirestore.getInstance()
    private val storageRef = Firebase.storage.reference
    private val userCollection = db.collection("users")

    fun addUser(user: User, onCompletionHandler: () -> Unit){
        user?.let {
            CoroutineScope(Dispatchers.IO).launch {
                userCollection.document(user.uid).set(it).await()
                onCompletionHandler()
            }
        }
    }

    fun getUserById(uid: String): Task<DocumentSnapshot> {
        return userCollection.document(uid).get()
    }

    fun uploadUserProfilePic(
        activity: Activity?,
        bitmap: Bitmap,
        uid: String,
        onCompletionHandler: (String) -> Unit
    ){
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
                if (downloadUri!=null) {
                    log("## Stored path is $downloadUri")
                    onCompletionHandler(downloadUri.toString())
                }
            }
            activity?.showSnack("Uploaded!")
        }
    }


    fun fetchListOfVideoUris(
        activity: Activity?,
        uid: String,
        onCompletionHandler: (List<String>?, error: String?) -> Unit
    ) {
        val mReference = storageRef.child("videos/${Firebase.auth.currentUser!!.uid}")
        CoroutineScope(Dispatchers.IO).launch {
            mReference.listAll()
                .addOnSuccessListener { (items, prefixes) ->
                    var videoUriList = ArrayList<String>()

                    CoroutineScope(Dispatchers.IO).launch {
                        items.map {
                            async {
                                val url = it.downloadUrl.await()
                                videoUriList.add(url.toString())
                            }
                        }.forEach{
                            it.await()
                        }
                        onCompletionHandler(videoUriList, null)
                    }
                }
                .addOnFailureListener {
                    onCompletionHandler(null, it.message)
                }

            /*try {
                val result = mReference.listAll().await()
                result?.let {
                    videoUriList = it.items
                    onCompletionHandler(videoUriList, null)
                }
            }catch (e:Exception){
                onCompletionHandler(null, e.message)
            }*/
        }
    }

}