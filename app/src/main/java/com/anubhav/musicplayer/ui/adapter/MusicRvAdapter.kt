package com.anubhav.musicplayer.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.anubhav.musicplayer.R
import com.anubhav.musicplayer.databinding.MusicItemBinding
import com.anubhav.musicplayer.model.models.Music
import com.anubhav.musicplayer.model.models.MusicAction
import com.anubhav.musicplayer.model.models.MusicActionType
import com.squareup.picasso.Picasso

class MusicRvAdapter(private val itemList: ArrayList<Music>, private val callback: (action: MusicAction) -> Unit) : RecyclerView.Adapter<MusicRvAdapter.MusicViewHolder>() {

    inner class MusicViewHolder(private val itemBinding: MusicItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(item: Music, callback: (action: MusicAction) -> Unit) {
            val durationMinutesSeconds = "${(item.duration.toInt()/(60*1000)).toString().padStart(2, '0')}:${(item.duration.toInt()%60).toString().padStart(2, '0')}";
            itemBinding.tvSongName.text = item.name
            itemBinding.tvSongArtist.text = item.artist
            itemBinding.tvSongLength.text = durationMinutesSeconds
            Picasso.get().load(item.coverUri).error(R.drawable.img_music).into(itemBinding.ivProfile)

            itemBinding.ivPause.isVisible = false
            itemBinding.main.setOnClickListener {
                val currentIndex = it.tag.toString().toInt()
                callback(MusicAction(currentIndex, item, MusicActionType.GO_TO_PLAYER))
            }
            itemBinding.main.setOnLongClickListener {
                val currentIndex = it.tag.toString().toInt()
                callback(MusicAction(currentIndex, item, MusicActionType.PLAY))
                return@setOnLongClickListener true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val itemBinding = MusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(itemList[position], callback)
        holder.itemView.tag = position
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}