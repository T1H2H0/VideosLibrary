package com.t1h2h0.videos.views

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.t1h2h0.videos.databinding.FragmentMainBinding
import com.t1h2h0.videos.databinding.ItemSearchBinding
import com.t1h2h0.videos.utils.VideoSearchResult

class SearchResultsAdapter(
    private val onVideoClick: (VideoSearchResult) -> Unit
) : ListAdapter<VideoSearchResult, SearchResultsAdapter.VideoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
       )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video)
    }

    inner class VideoViewHolder(
        private val binding: ItemSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: VideoSearchResult) {
            binding.apply {
                titleTextView.text = video.title
                uploaderTextView.text = video.uploaderName

                // Format duration (convert seconds to mm:ss)
                val duration = formatDuration(video.duration)
                durationTextView.text = duration

                // Format view count
                val viewCount = formatViewCount(video.viewCount)
                viewCountTextView.text = viewCount

                // Load thumbnail
                video.thumbnailUrl?.let { thumbnailUrl ->
                    Glide.with(thumbnailImageView.context)
                        .load(thumbnailUrl)
                        .into(thumbnailImageView)
                }

                // Handle click
                root.setOnClickListener {
                    onVideoClick(video)
                }
            }
        }

        private fun formatDuration(seconds: Long): String {
            if (seconds <= 0) return "0:00"

            val minutes = seconds / 60
            val secs = seconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val mins = minutes % 60
                String.format("%d:%02d:%02d", hours, mins, secs)
            } else {
                String.format("%d:%02d", minutes, secs)
            }
        }

        private fun formatViewCount(count: Long): String {
            return when {
                count >= 1_000_000 -> {
                    val millions = count / 1_000_000.0
                    String.format("%.1fM views", millions)
                }
                count >= 1_000 -> {
                    val thousands = count / 1_000.0
                    String.format("%.1fK views", thousands)
                }
                else -> "$count views"
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<VideoSearchResult>() {
        override fun areItemsTheSame(oldItem: VideoSearchResult, newItem: VideoSearchResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VideoSearchResult, newItem: VideoSearchResult): Boolean {
            return oldItem == newItem
        }
    }
}