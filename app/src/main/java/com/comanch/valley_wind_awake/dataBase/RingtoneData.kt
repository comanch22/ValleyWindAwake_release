package com.comanch.valley_wind_awake.dataBase

import android.content.ContentUris
import android.provider.MediaStore
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_melody_data_table")
data class RingtoneData(

    @PrimaryKey(autoGenerate = true)
    var melodyId: Long = 0L,

    @ColumnInfo(name = "title")
    var title: String = "",

    @ColumnInfo(name = "album")
    var album: String = "",

    @ColumnInfo(name = "artist")
    var artist: String = "",

    @ColumnInfo(name = "duration")
    var duration: String = "",

    @ColumnInfo(name = "musicId")
    var musicId: Long = 0,

    @ColumnInfo(name = "uri")
    var uriAsString: String = "",

    @ColumnInfo(name = "active")
    var active: Int = 0,

    @ColumnInfo(name = "isCustom")
    var isCustom: Int = 0,

    @ColumnInfo(name = "position")
    var position: Int = -1

){

    fun setUriFromMusicId(musicId: Long){

        this.uriAsString = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            musicId
        ).toString()
    }
}
