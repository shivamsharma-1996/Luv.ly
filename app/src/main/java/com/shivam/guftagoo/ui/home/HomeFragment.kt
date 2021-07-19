package com.shivam.guftagoo.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.patch.patchcalling.PatchResponseCodes
import com.patch.patchcalling.interfaces.OutgoingCallResponse
import com.patch.patchcalling.javaclasses.PatchSDK
import com.shivam.guftagoo.R
import com.shivam.guftagoo.daos.UserDao
import com.shivam.guftagoo.databinding.FragmentHomeBinding
import com.shivam.guftagoo.extensions.delayedHandler
import com.shivam.guftagoo.extensions.runOnMain
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.ItemModel
import com.shivam.guftagoo.models.User
import com.yuyakaido.android.cardstackview.*
import kotlinx.android.synthetic.main.fragment_home.*
import org.json.JSONObject

class HomeFragment private constructor(): Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private var manager : CardStackLayoutManager? =null
    private var adapter: CardStackAdapter? = null
    private val TAG = "HomeFragment"

    private val MICROPHONE_PERMISSION_CODE = 301

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.root    
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {

        manager = CardStackLayoutManager(context, object : CardStackListener {
            override fun onCardDragging(direction: Direction, ratio: Float) {
                Log.d(TAG, "onCardDragging: d=" + direction.name + " ratio=" + ratio)
            }

            override fun onCardSwiped(direction: Direction) {
                Log.d(TAG, "onCardSwiped: p=" + manager!!.topPosition + " d=" + direction)
                if (direction === Direction.Right) {
                    Toast.makeText(context, "Direction Right", Toast.LENGTH_SHORT).show()
                }
                if (direction === Direction.Top) {
                    Toast.makeText(context, "Direction Top", Toast.LENGTH_SHORT).show()
                }
                if (direction === Direction.Left) {
                    Toast.makeText(context, "Direction Left", Toast.LENGTH_SHORT).show()
                }
                if (direction === Direction.Bottom) {
                    Toast.makeText(context, "Direction Bottom", Toast.LENGTH_SHORT).show()
                }

                /*// Paginating
                if (manager!!.topPosition == adapter!!.itemCount - 5) {
                    paginate()
                }*/
            }

            override fun onCardRewound() {
                Log.d(TAG, "onCardRewound: " + manager!!.topPosition)
            }

            override fun onCardCanceled() {
                Log.d(TAG, "onCardRewound: " + manager!!.topPosition)
            }

            override fun onCardAppeared(view: View, position: Int) {
                //val tv: TextView = view.findViewById(R.id.item_name)
                //  Log.d(TAG, "onCardAppeared: " + position + ", nama: " + tv.text)
            }

            override fun onCardDisappeared(view: View, position: Int) {
                // val tv: TextView = view.findViewById(R.id.item_name)
                //  Log.d(TAG, "onCardAppeared: " + position + ", nama: " + tv.text)
            }
        })

        manager!!.setStackFrom(StackFrom.None)
        manager!!.setVisibleCount(3)
        manager!!.setTranslationInterval(8.0f)
        manager!!.setScaleInterval(0.95f)
        manager!!.setSwipeThreshold(0.3f)
        manager!!.setMaxDegree(20.0f)
        manager!!.setDirections(Direction.FREEDOM)
        manager!!.setCanScrollHorizontal(true)
        manager!!.setSwipeableMethod(SwipeableMethod.Manual)
        manager!!.setOverlayInterpolator(LinearInterpolator())
        card_stack_view.itemAnimator = DefaultItemAnimator()

        fetchUsers()
    }


    /*private fun paginate() {
        val old: MutableList<User> = adapter!!.getItems()
        val baru: MutableList<User> = MutableList(addList())
        val callback = CardStackCallback(old, baru)
        val hasil = DiffUtil.calculateDiff(callback)
        adapter!!.setItems(baru)
        hasil.dispatchUpdatesTo(adapter!!)
    }*/

    private fun fetchUsers(){
        val userDao = UserDao()

        userDao.fetchUsers{ userList, error ->
            if(userList!=null && userList.isNotEmpty()){
                adapter = CardStackAdapter(userList.filter {  it.uid != Firebase.auth.uid}
                    .toMutableList()){ user ->
                    if(user!=null && user.videos.isNotEmpty()){
                        getMicrophonePermission {
                            makeVoIPCall(user)
                        }
                    }else{
                        showSnack("Add atleast one video to your profile!")
                    }
                }
                card_stack_view.layoutManager = manager
                card_stack_view.adapter = adapter
            }else{
                showSnack(error!!)
            }
            binding.findPeopleLoader.visibility = View.GONE
            binding.cardStackView.visibility = View.VISIBLE
        }
    }

    private fun makeVoIPCall(user: User) {
        val options = JSONObject()
        if (PatchSDK.isGoodToGo()) {
            //options.put("cli", cli);
            PatchSDK.getInstance().call(
                context,
                user.uid,
                user.videos[0],
                options,
                object : OutgoingCallResponse {
                    override fun callStatus(reason: Int) {
                        Log.d("Patch", "reason is$reason")
                        Toast.makeText(context, "reason is$reason", Toast.LENGTH_LONG)
                            .show()
                    }

                    override fun onSuccess(response: Int) {
                        Log.d("Patch", response.toString())
                        Toast.makeText(context, "response is$response", Toast.LENGTH_LONG)
                            .show()
                    }

                    override fun onFailure(error: Int) {
                        Toast.makeText(context, "error is$error", Toast.LENGTH_LONG).show()
                        runOnMain {
                            if (error == PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_MICROPHONE_PERMISSION_NOT_GRANTED) {
                                Toast.makeText(
                                    context,
                                    "Micrphone permission needed. Enable permission from app settings",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (error == PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CONTACT_NOT_REACHABLE) {
                                Toast.makeText(
                                    context,
                                    "Cuid is not reachable",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (error == PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_BAD_NETWORK) {
                                Toast.makeText(context, "Bad network", Toast.LENGTH_LONG)
                                    .show()
                            } else if (error == PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_SOMETHING_WENT_WRONG) {
                                Toast.makeText(
                                    context,
                                    "something went wrong",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                })
        }
    }

    private fun addList(): ArrayList<ItemModel> {
        val items: ArrayList<ItemModel> = ArrayList()
        items.add(ItemModel(R.drawable.sample4, "Markobar", "19", "Bandung"))
        items.add(ItemModel(R.drawable.sample5, "Marmut", "25", "Hutan"))
        items.add(ItemModel(R.drawable.sample1, "Markonah", "24", "Jember"))
        items.add(ItemModel(R.drawable.sample2, "Marpuah", "20", "Malang"))
        items.add(ItemModel(R.drawable.sample3, "Sukijah", "27", "Jonggol"))
        items.add(ItemModel(R.drawable.sample4, "Markobar", "19", "Bandung"))
        items.add(ItemModel(R.drawable.sample5, "Marmut", "25", "Hutan"))
        items.add(ItemModel(R.drawable.sample7, "Markonah", "24", "Jember"))
        items.add(ItemModel(R.drawable.sample6, "Marpuah", "20", "Malang"))
        items.add(ItemModel(R.drawable.ic_dummy_user, "Sukijah", "27", "Jonggol"))
        return items
    }

    private fun getMicrophonePermission(onGrantedListener: () -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.CAMERA),
                MICROPHONE_PERMISSION_CODE
            )
        }else{
            onGrantedListener
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}