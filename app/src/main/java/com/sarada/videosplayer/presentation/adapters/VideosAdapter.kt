package com.sarada.videosplayer.presentation.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sarada.videosplayer.R
import com.sarada.videosplayer.databinding.VideoItemLayoutBinding
import com.sarada.videosplayer.models.Videos

class VideosAdapter(
    private val mContext: Context,
    private val onVideoItemClick: (position: Int) -> Unit
) :
    ListAdapter<Videos, VideosAdapter.VideoViewHolder>(VideosDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder.from(mContext, parent, onVideoItemClick)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(position, item)
    }

    class VideoViewHolder private constructor(
        private val mContext: Context,
        val binding: VideoItemLayoutBinding,
        private val onVideoItemClick: (position: Int) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        init {}

        fun bind(
            position: Int,
            item: Videos
        ) {
            Glide.with(mContext)
                .load(item.image)
                .apply(RequestOptions().placeholder(R.drawable.ic_launcher_foreground))
                .into(binding.ivVideoImage)
            binding.ivVideoImage.setOnClickListener { onVideoItemClick(position) }
        }

        companion object {
            fun from(mContext: Context, parent: ViewGroup, onVideoItemClick: (position: Int) -> Unit): VideoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = VideoItemLayoutBinding.inflate(layoutInflater, parent, false)
                return VideoViewHolder(mContext, binding, onVideoItemClick)
            }
        }
    }
}

class VideosDiffCallback: DiffUtil.ItemCallback<Videos>() {
    override fun areItemsTheSame(oldItem: Videos, newItem: Videos): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Videos, newItem: Videos): Boolean {
        return oldItem == newItem
    }

}