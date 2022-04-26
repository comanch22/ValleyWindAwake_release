package com.comanch.valley_wind_awake

open class DateDifference {

    private val secondsM: Long = 1000
    private val minutesM = secondsM * 60
    private val hoursM = minutesM * 60
    private val daysM = hoursM * 24

    fun getResultString(_diff: Long, daysStr: String, hoursStr: String, minStr: String): String {

        var diff = _diff
        val days: Long = diff / daysM
        diff %= daysM

        val hours: Long = diff / hoursM
        diff %= hoursM

        val minutes: Long = diff / minutesM
        diff %= minutesM

        return "$days $daysStr $hours $hoursStr $minutes $minStr"
    }
}