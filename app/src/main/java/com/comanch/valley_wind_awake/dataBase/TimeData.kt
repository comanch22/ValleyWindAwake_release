package com.comanch.valley_wind_awake.dataBase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_data_table")
data class TimeData(

    @PrimaryKey(autoGenerate = true)
    var timeId: Long = 0L,

    @ColumnInfo(name = "s1")
    var s1: String = "0",

    @ColumnInfo(name = "s2")
    var s2: String = "0",

    @ColumnInfo(name = "s3")
    var s3: String = "0",

    @ColumnInfo(name = "s4")
    var s4: String = "0",

    @ColumnInfo(name = "hhmm12")
    var hhmm12: String = "1200",

    @ColumnInfo(name = "hhmm24")
    var hhmm24: String = "0000",

    @ColumnInfo(name = "ampm")
    var ampm: String = "AM",

    @ColumnInfo(name = "mondayOn")
    var mondayOn: Boolean = false,

    @ColumnInfo(name = "tuesdayOn")
    var tuesdayOn: Boolean = false,

    @ColumnInfo(name = "wednesdayOn")
    var wednesdayOn: Boolean = false,

    @ColumnInfo(name = "thursdayOn")
    var thursdayOn: Boolean = false,

    @ColumnInfo(name = "fridayOn")
    var fridayOn: Boolean = false,

    @ColumnInfo(name = "saturdayOn")
    var saturdayOn: Boolean = false,

    @ColumnInfo(name = "sundayOn")
    var sundayOn: Boolean = false,

    @ColumnInfo(name = "specialDate")
    var specialDate: Long = 0L,

    @ColumnInfo(name = "specialDateStr")
    var specialDateStr: String = "",

    @ColumnInfo(name = "nearestDate")
    var nearestDate: Long = 0L,

    @ColumnInfo(name = "delayTime")
    var delayTime: Long = 0L,

    @ColumnInfo(name = "nearestDateStr")
    var nearestDateStr: String = "",

    @ColumnInfo(name = "nearestDateStr12")
    var nearestDateStr12: String = "",

    @ColumnInfo(name = "active")
    var active: Boolean = false,

    @ColumnInfo(name = "oneInstance")
    var oneInstance: Boolean = true,

    @ColumnInfo(name = "ringtoneUri")
    var ringtoneUri: String = "",

    @ColumnInfo(name = "contentDescriptionRu12")
    var contentDescriptionRu12: String = "",

    @ColumnInfo(name = "contentDescriptionEn12")
    var contentDescriptionEn12: String = "",

    @ColumnInfo(name = "contentDescriptionRu24")
    var contentDescriptionRu24: String = "",

    @ColumnInfo(name = "contentDescriptionEn24")
    var contentDescriptionEn24: String = "",

    @ColumnInfo(name = "ringtoneTitle")
    var ringtoneTitle: String = "",

    @ColumnInfo(name = "requestCode")
    var requestCode: Int = 0
){

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}
