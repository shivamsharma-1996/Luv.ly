package com.shivam.guftagoo.ui.home

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.shivam.guftagoo.R
import com.shivam.guftagoo.models.ItemModel
import kotlinx.android.synthetic.main.activity_home.*
import java.util.*
import kotlin.collections.ArrayList


class CardStackAdapter(items: ArrayList<ItemModel>, val swipeListener: ()->Unit): RecyclerView.Adapter<CardStackAdapter.CardStackHolder>() {
    private var items: ArrayList<ItemModel> = ArrayList()

    init {
        this.items = items
    }

    class CardStackHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById((R.id.item_image))
        val tv_user_name: TextView = itemView.findViewById((R.id.tv_user_name))
        val name: TextView = itemView.findViewById((R.id.item_name))
        val city: TextView = itemView.findViewById((R.id.item_city))
        val interests: ChipGroup = itemView.findViewById(R.id.chips_user_interest)

        val fab_reject = itemView.findViewById<View>((R.id.fab_reject))
        val fab_accept = itemView.findViewById<View>((R.id.fab_accept))

        fun setData(data: ItemModel) {
            Glide.with(itemView.context).
            load(data.image).
            into(imageView)

            tv_user_name.text = data.name + ", " + data.usia
//            name.text = data.name
//            city.text = data.kota

            val categoryChipList: List<String> = listOf(
                "printing",
                "code",
                "chess",
                "cook",
                "bloggin",
                "graphic",
                "Graphic",
                "stocks",
                "reading",
                "sleep",
                "art",
                "hacking",
                "riding",
                "travelling",
                "coffee"
            )
            Collections.shuffle(categoryChipList);


            for (record in categoryChipList.subList(0, 4)) {
                val mChip =
                    LayoutInflater.from(itemView.context).inflate(R.layout.item_list_category, null, false) as Chip
                mChip.text = record.toString()
                mChip.isChecked = true

                val paddingDp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 5f,
                    itemView.resources.displayMetrics
                ).toInt()
                mChip.setPadding(paddingDp, 0, paddingDp, 0)
                interests.addView(mChip)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardStackHolder {
        return CardStackHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_card,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CardStackHolder, position: Int) {
        holder.setData(items!![position])

        holder.fab_reject.setOnClickListener{
            swipeListener()
        }
        holder.fab_accept.setOnClickListener{
            swipeListener()
        }
    }

    override fun getItemCount(): Int {
        return items!!.count()
    }


    fun getItems(): ArrayList<ItemModel> {
        return items
    }

    fun setItems(items1: ArrayList<ItemModel>) {
        items = items1
    }
}