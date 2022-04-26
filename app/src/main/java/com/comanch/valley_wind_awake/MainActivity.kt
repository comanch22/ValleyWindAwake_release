package com.comanch.valley_wind_awake

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.*
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

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 27) {
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).also {
                it.requestDismissKeyguard(this, null)
            }
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this)
        }

        setContentView(R.layout.activity_main)
    }


    override fun onStart() {

        super.onStart()
        intent?.extras?.let {
            if (it.containsKey(IntentKeys.Alarm_R)) {
                intent.removeExtra(IntentKeys.Alarm_R)
                val itemId = intent.extras?.getString(IntentKeys.timeId) ?: "-1"
                findNavController(R.id.nav_host_fragment).navigate(
                    ListFragmentDirections.actionListFragmentToDetailFragment(
                        itemId.toLong()
                    )
                )
            }
        }
    }

    override fun onPause() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
        super.onPause()
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