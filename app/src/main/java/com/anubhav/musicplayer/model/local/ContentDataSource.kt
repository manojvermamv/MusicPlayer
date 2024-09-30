package com.anubhav.musicplayer.model.local

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import com.anubhav.musicplayer.model.models.Contact
import com.anubhav.musicplayer.model.models.Music

class ContentDataSource(private val contentResolver: ContentResolver) {

    fun fetchContacts(): List<Contact> {
        val result = mutableListOf<Contact>()
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        cursor?.let {
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                result.add(Contact(cursor.getString(0), cursor.getString(1).toContactImageUri()))
                cursor.moveToNext()
            }
            cursor.close()
        }
        result.sortWith { a, b -> a.name.compareTo(b.name) }
        return result.toList()
    }

    fun fetchMusic(): List<Music> {
        val result = mutableListOf<Music>()
        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.Audio.Media.DATA} LIKE ? AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("%/Music/%", "15000")
        val cursor = contentResolver.query(musicUri, null, selection, selectionArgs, null)
        if ((cursor != null) && cursor.moveToFirst()) {
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            do {
                val thisId = cursor.getLong(idColumn)
                val thisTitle = cursor.getString(titleColumn)
                val thisDuration = cursor.getString(durationColumn)
                val thisArtist = cursor.getString(artistColumn)
                val thisAlbumId = cursor.getString(albumIdColumn)
                val albumArtUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtContentUri = ContentUris.withAppendedId(albumArtUri, thisAlbumId.toLong())
                result.add(Music(thisId, thisTitle, thisDuration, thisArtist, albumArtContentUri))

            } while (cursor.moveToNext())
            cursor.close()
        }
        result.sortWith { a, b -> a.name.compareTo(b.name) }
        return result.toList()
    }

}

fun String.toContactImageUri() = Uri.withAppendedPath(
    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, this.toLong()),
    ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
).toString()