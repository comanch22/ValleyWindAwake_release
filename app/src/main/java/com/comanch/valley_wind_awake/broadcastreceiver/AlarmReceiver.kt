package com.comanch.valley_wind_awake.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.comanch.valley_wind_awake.stringKeys.IntentKeys
import com.comanch.valley_wind_awake.alarmManagement.AlarmControl
import com.comanch.valley_wind_awake.alarmManagement.AlarmTypeOperation
import com.comanch.valley_wind_awake.alarmManagement.RingtoneService
import com.comanch.valley_wind_awake.dataBase.TimeDataDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmControl: AlarmControl

    @Inject
    lateinit var database: TimeDataDao

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent != null) {
            when (intent.action) {

                "android.intent.action.BOOT_COMPLETED" -> {
                    restartAlarms(context)
                }
                IntentKeys.SetAlarm -> {

                    startRingtoneService(
                        context,
                        intent.getLongExtra(IntentKeys.timeId, -1),
                        intent.getStringExtra(IntentKeys.timeStr) ?: "0000",
                        intent.getStringExtra(IntentKeys.ringtoneUri) ?: ""
                    )
                }
                IntentKeys.offAlarm -> {

                    intent.getStringExtra(IntentKeys.timeId)?.let { offAlarm(context, it) }
                    if (intent.getBooleanExtra("notification", false)) {
                        if (context != null) {
                            NotificationManagerCompat.from(context).cancel(17131415)
                        }
                    }
                }
                IntentKeys.offDuplicateSignal -> {

                    intent.getStringExtra(IntentKeys.timeId)
                        ?.let { offDuplicateSignal(context, it) }
                }
                IntentKeys.offAlarmFromTimer -> {

                    intent.getStringExtra(IntentKeys.timeId)?.let { offAlarm(context, it) }
                }
                IntentKeys.pauseAlarm -> {

                    pauseAlarmFromNotification(
                        context,
                        intent.getStringExtra(IntentKeys.timeId) ?: ""
                    )
                }
            }
        }
    }

    private fun restartAlarms(context: Context?) {

        context.let {
            val mCoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
            mCoroutineScope.launch {
                alarmControl.restartAlarm()
            }
        }
    }

    private fun pauseAlarmFromNotification(context: Context?, timeId: String) {

        if (timeId.isNotEmpty() && timeId.toLongOrNull() != null) {
            context.let {
                val mCoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
                mCoroutineScope.launch {
                    val item = database.get(timeId.toLong()) ?: return@launch
                    alarmControl.timeData = item
                    alarmControl.schedulerAlarm(AlarmTypeOperation.PAUSE)
                    stopRingtoneService(context)
                }
            }
            if (context != null) {
                NotificationManagerCompat.from(context).cancel(17131415)
            }
        }
    }

    private fun offAlarm(context: Context?, timeId: String) {

        if (timeId.isNotEmpty() && timeId.toLongOrNull() != null) {
            stopRingtoneService(context)
            context.let {
                val mCoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
                mCoroutineScope.launch {
                    val item = database.get(timeId.toLong()) ?: return@launch
                    alarmControl.timeData = item
                    alarmControl.schedulerAlarm(AlarmTypeOperation.OFF)
                }
            }
            if (context != null) {
                NotificationManagerCompat.from(context).cancel(17131415)
            }
        }
    }

    private fun offDuplicateSignal(context: Context?, timeId: String) {
        if (timeId.isNotEmpty() && timeId.toLongOrNull() != null) {
            context.let {
                val mCoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
                mCoroutineScope.launch {
                    val item = database.get(timeId.toLong()) ?: return@launch
                    alarmControl.timeData = item
                    alarmControl.schedulerAlarm(AlarmTypeOperation.OFF)
                }
            }
        }
    }

    private fun startRingtoneService(
        context: Context?,
        timeId: Long,
        timeStr: String,
        ringtoneUri: String
    ) {

        val intent = Intent(context, RingtoneService::class.java).apply {
            action = IntentKeys.playAction
            putExtra(IntentKeys.ringtoneUri, ringtoneUri)
            putExtra(IntentKeys.timeId, timeId)
            putExtra(IntentKeys.timeStr, timeStr)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(intent)
        } else {
            context?.startService(intent)
        }
    }

    private fun stopRingtoneService(context: Context?) {

        val intent = Intent(context, RingtoneService::class.java).apply {
            action = IntentKeys.stopAction
        }
        context?.startService(intent)
    }
}

