package com.comanch.valley_wind_awake

import android.content.SharedPreferences
import com.comanch.valley_wind_awake.stringKeys.AppStyleKey
import com.comanch.valley_wind_awake.stringKeys.PreferenceKeys
import javax.inject.Inject

class DefaultPreference @Inject constructor(private val preference: SharedPreferences) {

    fun getString(key: String): String {

        val defaultValue =
            when(key){
                PreferenceKeys.signalDuration -> "2"
                PreferenceKeys.pauseDuration -> "5"
                PreferenceKeys.defaultRingtoneUri -> ""
                PreferenceKeys.defaultRingtoneTitle -> ""
                AppStyleKey.appStyle -> AppStyleKey.blue
                else -> {
                    null
                }
            }

        return preference.getString(key, defaultValue)!!
    }

    fun getBoolean(key: String): Boolean {

        val defaultValue =
            when(key){
            PreferenceKeys.isVibrate -> false
            else -> {
                null
            }
        }

        return preference.getBoolean(key, defaultValue!!)
    }

    fun putBoolean(key: String, value: Boolean) {

        with(preference.edit()) {
            putBoolean(key, value)
            apply()
        }
    }
/*    fun putInt(value: Int){

        with(preference.edit()){
            putInt(key, value)
            apply()
        }
    }*/

    fun putString(key: String, value: String){

        with(preference.edit()){
            putString(key, value)
            apply()
        }
    }
}
