package com.comanch.valley_wind_awake

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class DateFormatter(_hhmm: String, _ampm: String?) {

    private val hhmm = _hhmm.substring(0,4)
    private val ampm = _ampm

    fun format12from24(): String? {

        return try {
            SimpleDateFormat("hhmm", Locale.US)
                .format(SimpleDateFormat("HHmm", Locale.US).parse(hhmm)!!)
        } catch (e: ParseException) {
            null
        }
    }

    fun format24from12(): String? {

        return try {
            SimpleDateFormat("HHmm", Locale.US)
                .format(SimpleDateFormat("hhmm a", Locale.US).parse("${hhmm} $ampm")!!)
        } catch (e: ParseException) {
            null
        }
    }

    fun getAmPm24(): String? {

        return try {
            SimpleDateFormat("a", Locale.US)
                .format(SimpleDateFormat("HHmm", Locale.US).parse(hhmm)!!)
        } catch (e: ParseException) {
            null
        }
    }

    fun getAmPm12(): String? {

        return try {
            SimpleDateFormat("a", Locale.US)
                .format(SimpleDateFormat("hhmm a", Locale.US).parse("$hhmm $ampm")!!)
        } catch (e: ParseException) {
            null
        }
    }
}