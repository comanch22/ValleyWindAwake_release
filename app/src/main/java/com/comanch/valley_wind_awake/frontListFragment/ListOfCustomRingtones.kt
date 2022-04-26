package com.comanch.valley_wind_awake.frontListFragment

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.comanch.valley_wind_awake.dataBase.RingtoneData

class ListOfCustomRingtones(context: Context) {

    private val list = mutableListOf<RingtoneData>()

    private val mediaContentUriE: Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    private val projection = arrayOf(
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.IS_MUSIC,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media._ID
    )

    private val selection = "is_music=?"
    private val selectionArgs = arrayOf("1")
    private val sortOrder = "artist ASC, album ASC"

    private val cursor =
        mediaContentUriE?.let {
            context.contentResolver?.query(
                it,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
        }

    private fun createListOfRingtones() {

        if (cursor != null && cursor.moveToFirst() && cursor.count > 0) {
            do {

                val ringtoneData = RingtoneData()
                ringtoneData.artist = cursor.getString(0)
                ringtoneData.album = cursor.getString(1)
                ringtoneData.title = cursor.getString(2)
                ringtoneData.duration = if (cursor.getLong(4) > 0) {
                    "${cursor.getLong(4) / 1000 / 60}:${cursor.getLong(4) / 1000 % 60}"
                } else {
                    "0"
                }
                ringtoneData.musicId = cursor.getLong(5)
                list.add(ringtoneData)
            } while (cursor.moveToNext())
        }
    }

    fun getListOfRingtones(): List<RingtoneData>{
        createListOfRingtones()
        return list
    }
}