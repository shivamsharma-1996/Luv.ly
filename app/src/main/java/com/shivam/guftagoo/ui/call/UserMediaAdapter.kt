package com.shivam.guftagoo.ui.call

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.shivam.guftagoo.R
import com.shivam.guftagoo.models.UserVideos

class UserMediaAdapter(private val onclickLister: View.OnClickListener) :
    RecyclerView.Adapter<UserMediaAdapter.ConversationHolder>() {
    private var videosList:ArrayList<UserVideos> = ArrayList()

    class ConversationHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val ivThumbnail :ImageView = itemView.findViewById(R.id.iv_video_thumbnail)
        val ivMore :ImageView = itemView.findViewById(R.id.iv_more)
        val tvDefault :TextView = itemView.findViewById(R.id.tv_default_video)
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
        holder.itemView.tag = position
        holder.itemView.setOnClickListener(onclickLister)

        holder.ivMore.tag = position
        holder.ivMore.setOnClickListener(onclickLister)

        val requestOptions = RequestOptions()
        requestOptions.isMemoryCacheable
        Glide.with(holder.itemView.context)
            .setDefaultRequestOptions(requestOptions)
            .load(videosList[position].videoUrl).into(holder.ivThumbnail)

        if(videosList[position].isDefault){
            holder.tvDefault.visibility = VISIBLE
            holder.ivMore.visibility = GONE
        }else{
            holder.tvDefault.visibility = GONE
            holder.ivMore.visibility = VISIBLE
        }
    }

    fun submitData(thumbnail1: List<UserVideos>){
        videosList.clear()
        videosList.addAll(thumbnail1)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int {
        return videosList.size
    }
}