package com.comanch.valley_wind_awake.alarmManagement

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.comanch.valley_wind_awake.DefaultPreference
import com.comanch.valley_wind_awake.stringKeys.IntentKeys
import com.comanch.valley_wind_awake.MainActivity
import com.comanch.valley_wind_awake.broadcastreceiver.AlarmReceiver
import com.comanch.valley_wind_awake.dataBase.TimeData
import com.comanch.valley_wind_awake.dataBase.TimeDataDao
import com.comanch.valley_wind_awake.stringKeys.OperationKey
import com.comanch.valley_wind_awake.stringKeys.PreferenceKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AlarmControl @Inject constructor(
    val database: TimeDataDao,
    @ApplicationContext val context: Context
) {

    var timeData: TimeData? = null

    @Inject
    lateinit var preferences: DefaultPreference

    suspend fun restartAlarm() {

        val alarmList = database.getListItems()
        alarmList?.forEach {
            if (it.active) {
                onAlarm(it, true)
            }
        }
    }

    private fun createCalendarList(item: TimeData): MutableList<Calendar> {

        val calendarList = mutableListOf<Calendar>()
        setContentDescriptionPart1(item)
        createSeveralCalendars(item, calendarList)
        setNearestDate(item, calendarList)
        return calendarList
    }

    private fun setNearestDate(item: TimeData, calendarList: MutableList<Calendar>) {

        calendarList.sortBy {
            it.timeInMillis
        }
        if (calendarList.size > 0) {
            item.nearestDate = calendarList[0].timeInMillis
            item.nearestDateStr = SimpleDateFormat(
                "dd-MM-yyyy HH:mm", Locale.US
            ).format(calendarList[0].time)
            item.nearestDateStr12 = SimpleDateFormat(
                "dd-MM-yyyy hh:mm a", Locale.US
            ).format(calendarList[0].time)

            item.contentDescriptionRu12 +=
                ". ближайщий сигнал этого будильника. " +
                        SimpleDateFormat(
                            "dd-MM-yyyy hh:mm a", Locale("ru", "RU")
                        ).format(calendarList[0].time)
            item.contentDescriptionRu24 +=
                ". ближайщий сигнал этого будильника. " +
                        SimpleDateFormat(
                            "dd-MM-yyyy HH:mm", Locale("ru", "RU")
                        ).format(calendarList[0].time)
            item.contentDescriptionEn12 +=
                ". the nearest of this alarm clock. " +
                        SimpleDateFormat(
                            "dd-MM-yyyy hh:mm a", Locale.US
                        ).format(calendarList[0].time)
            item.contentDescriptionEn24 +=
                ". the nearest of this alarm clock. " +
                        SimpleDateFormat(
                            "dd-MM-yyyy HH:mm", Locale.US
                        ).format(calendarList[0].time)
        }
    }

    private fun createSeveralCalendars(
        item: TimeData,
        calendarList: MutableList<Calendar>
    ) {

        setContentDescriptionPart2(
            item,
            " отмечены дни недели. ",
            " the days of the week are marked. "
        )

        var simpleCondition = true
        var isRepeat = false
        if (item.specialDate > 0L) {
            simpleCondition = false
            if (item.specialDate > Calendar.getInstance().timeInMillis) {
                val calendar = Calendar.getInstance()

                item.contentDescriptionRu12 +=
                    "дополнительно указанная дата срабатывания сигнала. " +
                            SimpleDateFormat(
                                "yyyy-MM-dd", Locale("ru", "RU")
                            ).format(item.specialDate) + ". "
                item.contentDescriptionRu24 +=
                    "дополнительно указанная дата срабатывания сигнала. " +
                            SimpleDateFormat(
                                "yyyy-MM-dd", Locale("ru", "RU")
                            ).format(item.specialDate) + ". "
                item.contentDescriptionEn12 +=
                    "additionally, the specified date of the alarm. " +
                            SimpleDateFormat(
                                "yyyy-MM-dd", Locale.US
                            ).format(item.specialDate) + ". "
                item.contentDescriptionEn24 +=
                    "additionally, the specified date of the alarm. " +
                            SimpleDateFormat(
                                "yyyy-MM-dd", Locale.US
                            ).format(item.specialDate) + ". "

                calendar.timeInMillis = item.specialDate
                calendar.clear(Calendar.MILLISECOND)
                calendarList.add(calendar)
            }else{
                return calendarList.clear()
            }
        }

            if (item.mondayOn) {
                setDayOfWeek(Calendar.MONDAY, item, calendarList)
                setContentDescriptionPart2(
                    item,
                    " понедельник. ",
                    " monday. "
                )
                simpleCondition = false
                isRepeat = true
    }

            if (item.tuesdayOn) {
                setDayOfWeek(Calendar.TUESDAY, item, calendarList)
                setContentDescriptionPart2(
                    item,
                    " вторник. ",
                    " tuesday. "
                )
                simpleCondition = false
                isRepeat = true
            }

            if (item.wednesdayOn) {
                setDayOfWeek(Calendar.WEDNESDAY, item, calendarList)
                setContentDescriptionPart2(
                    item,
                    " среда. ",
                    " wednesday. "
                )
                simpleCondition = false
                isRepeat = true
            }

            if (item.thursdayOn) {
                setDayOfWeek(Calendar.THURSDAY, item, calendarList)
                setContentDescriptionPart2(
                    item,
                    " четверг. ",
                    " thursday. "
                )
                simpleCondition = false
                isRepeat = true
            }

            if (item.fridayOn) {
                setDayOfWeek(Calendar.FRIDAY, item, calendarList)
                setContentDescriptionPart2(
                    item,
                    " пятница. ",
                    " friday. "
                )
                simpleCondition = false
                isRepeat = true
            }

            if (item.saturdayOn) {
                setDayOfWeek(Calendar.SATURDAY, item, calendarList)
                setContentDescriptionPart2(
                    item,
                    " суббота. ",
                    " saturday. "
                )
                simpleCondition = false
                isRepeat = true
            }

            if (item.sundayOn) {
                setDayOfWeek(Calendar.SUNDAY, item, calendarList)
                setContentDescriptionPart2(
                    item,
                    " воскресенье. ",
                    " sunday. "
                )
                simpleCondition = false
                isRepeat = true
            }

            if (item.delayTime != 0L) {
                val calendar = Calendar.getInstance()
                if (calendar.timeInMillis <= item.delayTime) {
                    calendar.timeInMillis = item.delayTime
                    calendar.clear(Calendar.MILLISECOND)
                    calendarList.add(calendar)
                    simpleCondition = false
                }

        }

        if (simpleCondition) {
            val calendar = Calendar.getInstance()
            setTimeCalendar(calendar, item)
            if (calendar.timeInMillis < Calendar.getInstance().timeInMillis) {
                calendar.add(Calendar.DATE, 1)
            }
            calendarList.add(calendar)
        }

        item.oneInstance = calendarList.size <= 1 && !isRepeat
    }

    private fun createOffListCalendars(
        item: TimeData
    ): MutableList<Int> {
        val offList: MutableList<Int> = mutableListOf()

        var simpleCondition = true
        if (item.specialDate > 0L) {
            simpleCondition = false
            offList.add(1)
        }

        if (item.mondayOn) {
            offList.add(1)
            simpleCondition = false
        }

        if (item.tuesdayOn) {
            offList.add(1)
            simpleCondition = false
        }

        if (item.wednesdayOn) {
            offList.add(1)
            simpleCondition = false
        }

        if (item.thursdayOn) {
            offList.add(1)
            simpleCondition = false
        }

        if (item.fridayOn) {
            offList.add(1)
            simpleCondition = false
        }

        if (item.saturdayOn) {
            offList.add(1)
            simpleCondition = false
        }

        if (item.sundayOn) {
            offList.add(1)
            simpleCondition = false
        }

        if (item.delayTime != 0L) {
            offList.add(1)
            simpleCondition = false
        }

        if (simpleCondition) {
            offList.add(1)
        }
        return offList
    }

    private fun setDayOfWeek(day: Int, item: TimeData, calendarList: MutableList<Calendar>) {

        val calendar = Calendar.getInstance()
        setTimeCalendar(calendar, item)
        calendar.set(Calendar.DAY_OF_WEEK, day)
        if (calendar.timeInMillis < Calendar.getInstance().timeInMillis) {
            calendar.add(Calendar.DATE, 7)
        }
        calendarList.add(calendar)
    }

    private fun setTimeCalendar(calendar: Calendar, item: TimeData): Calendar {

        val hhmm = item.hhmm24
        calendar.set(Calendar.HOUR_OF_DAY, "${hhmm[0]}${hhmm[1]}".toInt())
        calendar.set(Calendar.MINUTE, "${hhmm[2]}${hhmm[3]}".toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.clear(Calendar.MILLISECOND)
        return calendar
    }

    suspend fun schedulerAlarm(typeOperation: AlarmTypeOperation): String {

        val item = timeData?.timeId?.let { database.get(it) } ?: return "error"
        when (typeOperation) {
            AlarmTypeOperation.SAVE -> {
                if (onAlarm(item) == OperationKey.incorrectSpecialDate){
                    return OperationKey.incorrectSpecialDate
                }
                if (item.nearestDate == 0L) {
                    return OperationKey.incorrectDate
                }
                item.active = true
                database.update(item)
            }
            AlarmTypeOperation.OFF -> {
                if (item.oneInstance) {
                    item.active = false
                } else {
                    offAlarm(item)
                    clearSpecialDate(item)
                    onAlarm(item)
                }
                database.update(item)
            }

            AlarmTypeOperation.DELETE -> {
                offAlarm(item)
                clearSpecialDate(item)
            }
            AlarmTypeOperation.SWITCH -> {
                if (item.active) {
                    offAlarm(item)
                    clearSpecialDate(item)
                    item.active = false
                    database.update(item)
                    return OperationKey.successOff
                } else {
                    onAlarm(item)
                    if (item.nearestDate == 0L) {
                        return OperationKey.incorrectDate
                    }
                    item.active = true
                    database.update(item)
                    return OperationKey.successOn
                }
            }
            AlarmTypeOperation.PAUSE -> {
                offAlarm(item)
                clearSpecialDate(item)
                item.active = false
                delaySignal(item)
                onAlarm(item)
                item.active = true
                database.update(item)
            }
        }
        return "success"
    }

    private fun onAlarm(item: TimeData, isRestart: Boolean = false): String {

        val calendarList = createCalendarList(item)
        if (calendarList.isNullOrEmpty()){
            return OperationKey.incorrectSpecialDate
        }

        if (calendarList.size > 0) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            var count = 1

            calendarList.forEach {
                val requestCode = "${item.requestCode}$count".toInt()
                val requestCodeInfo = "${item.requestCode}$count$count".toInt()
                val pendingIntent = if (isRestart) {
                    createPendingIntent(requestCode, item)
                } else {
                    createPendingIntent(requestCode)
                }
                val pendingIntentInfo = createPendingIntentInfo(requestCodeInfo)
                val alarmInfo = AlarmManager.AlarmClockInfo(it.timeInMillis, pendingIntentInfo)
                am.setAlarmClock(alarmInfo, pendingIntent)
                count++
            }
        }
        return ""
    }

    private fun offAlarm(item: TimeData) {

        item.delayTime = 0L
        val offList = createOffListCalendars(item)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var count = 1
        offList.forEach { _ ->
            val requestCode = "${item.requestCode}$count".toInt()
            val requestCodeInfo = "${item.requestCode}$count$count".toInt()
            val pendingIntent = createPendingIntent(requestCode)
            val pendingIntentInfo = createPendingIntentInfo(requestCodeInfo)
            am.cancel(pendingIntent)
            am.cancel(pendingIntentInfo)
            count++
        }
    }

    fun delaySignal(item: TimeData) {

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.clear(Calendar.MILLISECOND)
        item.delayTime =
            calendar.timeInMillis + (preferences.getString(PreferenceKeys.pauseDuration)).toInt() * 60000
    }

    private fun clearSpecialDate(item: TimeData) {

        if (item.specialDate > 0L) {
            if (Calendar.getInstance().timeInMillis > item.specialDate) {
                item.specialDate = 0L
                item.specialDateStr = ""
            }
        }
    }

    private fun clearDelayTime(item: TimeData) {

        if (item.delayTime > 0L) {
                item.delayTime = 0L
        }
    }

    private fun createPendingIntent(
        requestCode: Int,
        item: TimeData? = null
    ): PendingIntent {

        val timeDataLocal = item ?: timeData
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.action = IntentKeys.SetAlarm
        intent.putExtra(IntentKeys.timeId, timeDataLocal?.timeId)
        intent.putExtra(IntentKeys.ringtoneUri, timeDataLocal?.ringtoneUri)
        intent.putExtra(
            IntentKeys.timeStr,
            "${timeDataLocal?.s1}${timeDataLocal?.s2}${timeDataLocal?.s3}${timeDataLocal?.s4}"
        )
        intent.putExtra(IntentKeys.Alarm_R, true)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createPendingIntentInfo(requestCode: Int): PendingIntent {

        val intentInfo = Intent(context, MainActivity::class.java)
        intentInfo.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getActivity(
            context,
            requestCode,
            intentInfo,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun setContentDescriptionPart1(item: TimeData) {
        item.contentDescriptionRu12 =
            "будильник из списка. " +
                    "время будильника ${item.hhmm12[0]}${item.hhmm12[1]} часов " +
                    "${item.hhmm12[2]}${item.hhmm12[3]} минут " +
                    "двенадцатичасовой формат " +
                    "${item.ampm[0]}. ${item.ampm[1]}. " + ". "
        item.contentDescriptionRu24 =
            "будильник из списка. " +
                    "время будильника ${item.hhmm24[0]}${item.hhmm24[1]} часов " +
                    "${item.hhmm24[2]}${item.hhmm24[3]} минут " + ". "
        item.contentDescriptionEn12 =
            "alarm clock from the list. " +
                    "alarm clock time ${item.hhmm12[0]}${item.hhmm12[1]} hours " +
                    "${item.hhmm12[2]}${item.hhmm12[3]} minutes " +
                    "twelve - hour format " +
                    "${item.ampm[0]}. ${item.ampm[1]}. " + ". "
        item.contentDescriptionEn24 =
            "alarm clock from the list. " +
                    "alarm clock time ${item.hhmm24[0]}${item.hhmm24[1]} hours " +
                    "${item.hhmm24[2]}${item.hhmm24[3]} minutes " + ". "
    }

    private fun setContentDescriptionPart2(item: TimeData, strRu: String, strEn: String) {

        item.contentDescriptionRu12 += strRu
        item.contentDescriptionRu24 += strRu
        item.contentDescriptionEn12 += strEn
        item.contentDescriptionEn24 += strEn
    }
}