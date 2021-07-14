package com.shivam.guftagoo.ui.call

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shivam.guftagoo.R

class UserMediaAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ConversationHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ConversationHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_video_thumbnail,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d("onBindViewHolder", "onBindViewHolder")
    }

    override fun getItemCount(): Int {
        return 16
    }
}