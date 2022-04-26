package com.comanch.valley_wind_awake.dataBase

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TimeDataDao {

    @Insert
    suspend fun insert(time: TimeData)

    @Update
    suspend fun update(time: TimeData)

    @Delete
    suspend fun delete(time: TimeData)

    @Query("SELECT * from time_data_table WHERE timeId = :key")
    suspend fun get(key: Long): TimeData?

    @Query("DELETE FROM time_data_table")
    suspend fun clear()

    @Query("SELECT * FROM time_data_table ORDER BY timeId DESC LIMIT 1")
    suspend fun getItem(): TimeData?

    @Query("SELECT * FROM time_data_table ORDER BY timeId DESC")
    fun getAllItems(): LiveData<List<TimeData>>

    @Query("SELECT * FROM time_data_table ORDER BY timeId DESC")
    suspend fun getListItems(): List<TimeData>?
}