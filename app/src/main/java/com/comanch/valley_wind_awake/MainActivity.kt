package com.comanch.valley_wind_awake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.comanch.valley_wind_awake.stringKeys.AppStyleKey
import com.comanch.valley_wind_awake.stringKeys.IntentKeys
import com.comanch.valley_wind_awake.frontListFragment.ListFragmentDirections
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val channelId = "myAlarm_Channel_Id"
    private val channelName = "myAlarm_Channel_Name"
    private val notificationId = 17131415

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.extras?.let {
            if (it.containsKey(IntentKeys.Alarm_R)) {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            }
        }

        val defaultPreference = PreferenceManager.getDefaultSharedPreferences(this)
        when (defaultPreference.getString(AppStyleKey.appStyle, AppStyleKey.blue)) {
            AppStyleKey.blue -> {
                setTheme(R.style.Theme_MyAlarmClock)
            }
            AppStyleKey.green -> {
                setTheme(R.style.Theme_MyAlarmClock2)
            }
            AppStyleKey.gray -> {
                setTheme(R.style.Theme_MyAlarmClock3)
            }
        }

        if (Build.VERSION.SDK_INT >= 27) {
           setShowWhenLocked(true)
           setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this)
        }

        setContentView(R.layout.activity_main)

        intent?.extras?.let {
            if (it.containsKey(IntentKeys.Alarm_R)) {

                intent.removeExtra(IntentKeys.Alarm_R)

                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

                val itemId = intent.extras?.getString(IntentKeys.timeId) ?: "-1"
                findNavController(R.id.nav_host_fragment).navigate(
                    ListFragmentDirections.actionListFragmentToDetailFragment(
                        itemId.toLong()
                    )
                )
            }else{
                if (Build.VERSION.SDK_INT >= 27) {
                    setShowWhenLocked(false)
                    setTurnScreenOn(false)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                }
            }
        }

        if (intent?.extras == null || intent?.extras?.isEmpty == true){
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        }
    }

    private fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "MainActivityNotificationChannel$notificationId"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}