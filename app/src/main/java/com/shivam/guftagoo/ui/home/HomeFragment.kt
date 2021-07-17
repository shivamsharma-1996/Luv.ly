package com.shivam.guftagoo.ui.home

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.FragmentHomeBinding
import com.shivam.guftagoo.extensions.delayedHandler
import com.shivam.guftagoo.extensions.showSnack
import com.shivam.guftagoo.models.ItemModel
import com.yuyakaido.android.cardstackview.*
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment private constructor(): Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private var manager : CardStackLayoutManager? =null
    private var adapter: CardStackAdapter? = null
    private val TAG = "HomeFragment"
    
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


        Handler().delayedHandler(4000){
            binding.findPeopleLoader.visibility = View.GONE
            binding.cardStackView.visibility = View.VISIBLE
        }
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

                // Paginating
                if (manager!!.topPosition == adapter!!.itemCount - 5) {
                    paginate()
                }
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
        adapter = CardStackAdapter(addList()){
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            manager!!.setSwipeAnimationSetting(setting)
            card_stack_view.swipe()
            showSnack("Coming Soon, till then use swipe")
        }
        card_stack_view.layoutManager = manager
        card_stack_view.adapter = adapter
        card_stack_view.itemAnimator = DefaultItemAnimator()
    }


    private fun paginate() {
        val old: ArrayList<ItemModel> = adapter!!.getItems()
        val baru: ArrayList<ItemModel> = ArrayList(addList())
        val callback = CardStackCallback(old, baru)
        val hasil = DiffUtil.calculateDiff(callback)
        adapter!!.setItems(baru)
        hasil.dispatchUpdatesTo(adapter!!)
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


}