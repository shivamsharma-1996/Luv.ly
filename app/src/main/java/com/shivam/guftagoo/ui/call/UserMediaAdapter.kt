package com.shivam.guftagoo.ui.call

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.StorageReference
import com.shivam.guftagoo.R

class UserMediaAdapter(private val onClick: (String) -> Unit) :
    RecyclerView.Adapter<UserMediaAdapter.ConversationHolder>() {
    private var thumbnail:ArrayList<String> = ArrayList()

    class ConversationHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val image :ImageView = itemView.findViewById(R.id.iv_video_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationHolder {
        return ConversationHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_video_thumbnail,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ConversationHolder, position: Int) {
        Log.d("onBindViewHolder", "onBindViewHolder")
        val requestOptions = RequestOptions()
        requestOptions.isMemoryCacheable
        Glide.with(holder.itemView.context)
            .setDefaultRequestOptions(requestOptions)
            .load(thumbnail.get(position)).into(holder.image)

        holder.itemView.setOnClickListener{
            onClick(thumbnail[position])
        }
    }

    fun submitData(thumbnail1: List<String>){
        thumbnail.clear()
        thumbnail.addAll(thumbnail1)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return thumbnail.size
    }
}