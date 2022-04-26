package com.comanch.valley_wind_awake.dataBase

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RingtoneDataDao {

    @Insert
    suspend fun insert(melody: RingtoneData)

    @Update
    suspend fun update(melody: RingtoneData)

    @Delete
    suspend fun delete(melody: RingtoneData)

    @Query("SELECT * from custom_melody_data_table WHERE melodyId = :key")
    suspend fun get(key: Long): RingtoneData?

    @Query("DELETE FROM custom_melody_data_table")
    suspend fun clear()

    @Query("SELECT * FROM custom_melody_data_table ORDER BY melodyId DESC LIMIT 1")
    suspend fun getItem(): RingtoneData?

    @Query("SELECT * FROM custom_melody_data_table ORDER BY melodyId DESC")
    fun getAllItems(): LiveData<List<RingtoneData>>

    @Query("SELECT * FROM custom_melody_data_table ORDER BY melodyId DESC")
    fun getListItems(): List<RingtoneData>?

    @Query("SELECT * FROM custom_melody_data_table WHERE active = 1 LIMIT 1")
    suspend fun getActiveItem(): RingtoneData?
}