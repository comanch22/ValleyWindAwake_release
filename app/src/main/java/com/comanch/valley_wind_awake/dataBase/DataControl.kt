package com.comanch.valley_wind_awake.dataBase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TimeData::class, RingtoneData::class], version = 3, exportSchema = false)
abstract class DataControl : RoomDatabase() {

    abstract val timeDatabaseDao: TimeDataDao
    abstract val ringtoneDatabaseDao: RingtoneDataDao

    companion object {

        @Volatile
        private var INSTANCE: DataControl? = null

        fun getInstance(context: Context): DataControl {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            DataControl::class.java,
                            "time_data_base")
                            .fallbackToDestructiveMigration()
                            .build()
                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}