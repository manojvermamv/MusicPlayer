package com.anubhav.musicplayer.model.models

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import kotlinx.parcelize.Parcelize

@Parcelize
data class Music constructor(
    val id: Long,
    val name: String,
    val duration: String,
    val artist: String,
    val coverUri: Uri? = null
) : Parcelable {

    fun getMusicUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }

}

data class MusicAction(
    val index: Int = 0,
    val music: Music? = null,
    val type: MusicActionType? = null
)

enum class MusicActionType {
    PLAY,
    PAUSE,
    NEXT,
    PREVIOUS,
    SHUFFLE,
    EXIT,
    GO_TO_PLAYER
}