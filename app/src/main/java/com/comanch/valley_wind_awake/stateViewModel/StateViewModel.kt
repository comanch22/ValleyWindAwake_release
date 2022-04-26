package com.comanch.valley_wind_awake.stateViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.comanch.valley_wind_awake.LiveDataEvent

class StateViewModel : ViewModel() {

    private var setS1: String? = null
    private var setS2: String? = null
    private var setS3: String? = null
    private var setS4: String? = null
    private var setNumbersTimer: String? = null
    private var setIs24HourFormat: Boolean? = null

    private var setOnMonday: Boolean? = null
    private var setOnTuesday: Boolean? = null
    private var setOnWednesday: Boolean? = null
    private var setOnThursday: Boolean? = null
    private var setOnFriday: Boolean? = null
    private var setOnSaturday: Boolean? = null
    private var setOnSunday: Boolean? = null

    private var setSpecialDate: String? = null
    private var setRingtoneTitle: String? = null

    private val _is24HourFormat = MutableLiveData<LiveDataEvent<Boolean?>>()
    val is24HourFormat: LiveData<LiveDataEvent<Boolean?>>
        get() = _is24HourFormat

    private val _ringtoneTitle = MutableLiveData<LiveDataEvent<String?>>()
    val ringtoneTitle: LiveData<LiveDataEvent<String?>>
        get() = _ringtoneTitle

    private val _s1 = MutableLiveData<LiveDataEvent<String?>>()
    val s1: LiveData<LiveDataEvent<String?>>
        get() = _s1

    private val _s2 = MutableLiveData<LiveDataEvent<String?>>()
    val s2: LiveData<LiveDataEvent<String?>>
        get() = _s2

    private val _s3 = MutableLiveData<LiveDataEvent<String?>>()
    val s3: LiveData<LiveDataEvent<String?>>
        get() = _s3

    private val _s4 = MutableLiveData<LiveDataEvent<String?>>()
    val s4: LiveData<LiveDataEvent<String?>>
        get() = _s4

    private val _numbersTimer = MutableLiveData<LiveDataEvent<String?>>()
    val numbersTimer: LiveData<LiveDataEvent<String?>>
        get() = _numbersTimer

    private val _ampm = MutableLiveData<LiveDataEvent<String?>>()
    val ampm: LiveData<LiveDataEvent<String?>>
        get() = _ampm

    private val _monday = MutableLiveData<LiveDataEvent<Boolean?>>()
    val monday: LiveData<LiveDataEvent<Boolean?>>
        get() = _monday

    private val _tuesday = MutableLiveData<LiveDataEvent<Boolean?>>()
    val tuesday: LiveData<LiveDataEvent<Boolean?>>
        get() = _tuesday

    private val _wednesday = MutableLiveData<LiveDataEvent<Boolean?>>()
    val wednesday: LiveData<LiveDataEvent<Boolean?>>
        get() = _wednesday

    private val _thursday = MutableLiveData<LiveDataEvent<Boolean?>>()
    val thursday: LiveData<LiveDataEvent<Boolean?>>
        get() = _thursday

    private val _friday = MutableLiveData<LiveDataEvent<Boolean?>>()
    val friday: LiveData<LiveDataEvent<Boolean?>>
        get() = _friday

    private val _saturday = MutableLiveData<LiveDataEvent<Boolean?>>()
    val saturday: LiveData<LiveDataEvent<Boolean?>>
        get() = _saturday

    private val _sunday = MutableLiveData<LiveDataEvent<Boolean?>>()
    val sunday: LiveData<LiveDataEvent<Boolean?>>
        get() = _sunday

    private val _specialDate = MutableLiveData<LiveDataEvent<String?>>()
    val specialDate: LiveData<LiveDataEvent<String?>>
        get() = _specialDate

    fun sets1(s: String) {
        setS1 = s
    }

    fun sets2(s: String) {
        setS2 = s
    }

    fun sets3(s: String) {
        setS3 = s
    }

    fun sets4(s: String) {
        setS4 = s
    }

    fun setNumbersTimer(s: String) {
        setNumbersTimer = s
    }

    fun setIs24HourFormat(b: Boolean) {
        setIs24HourFormat = b
    }

    fun setMonday(b: Boolean) {
        setOnMonday = b
    }

    fun setTuesday(b: Boolean) {
        setOnTuesday = b
    }

    fun setWednesday(b: Boolean) {
        setOnWednesday = b
    }

    fun setThursday(b: Boolean) {
        setOnThursday = b
    }

    fun setFriday(b: Boolean) {
        setOnFriday = b
    }

    fun setSaturday(b: Boolean) {
        setOnSaturday = b

    }

    fun setSunday(b: Boolean) {
        setOnSunday = b
    }

    fun setSpecialDate(s: String) {
        setSpecialDate = s
    }

    fun setRingtoneTitle(s: String) {
        setRingtoneTitle = s
    }

    fun getNumbersTimer(): String? {
        return setNumbersTimer
    }

    fun getIs24HourFormat(): Boolean? {
        return setIs24HourFormat
    }

    fun restoreTimer() {
        _numbersTimer.value = LiveDataEvent(setNumbersTimer)
    }

    fun restoreStateForKeyboardFragment() {

        _is24HourFormat.value = LiveDataEvent(setIs24HourFormat)
        _numbersTimer.value = LiveDataEvent(setNumbersTimer)
        _monday.value = LiveDataEvent(setOnMonday)
        _tuesday.value = LiveDataEvent(setOnTuesday)
        _wednesday.value = LiveDataEvent(setOnWednesday)
        _thursday.value = LiveDataEvent(setOnThursday)
        _friday.value = LiveDataEvent(setOnFriday)
        _saturday.value = LiveDataEvent(setOnSaturday)
        _sunday.value = LiveDataEvent(setOnSunday)

        _specialDate.value = LiveDataEvent(setSpecialDate)
        _ringtoneTitle.value = LiveDataEvent(setRingtoneTitle)
    }

    fun resetStateStoreForKeyboardFragment() {

        setS1 = null
        setS2 = null
        setS3 = null
        setS4 = null
        setNumbersTimer = null
        setIs24HourFormat = null

        setOnMonday = null
        setOnTuesday = null
        setOnWednesday = null
        setOnThursday = null
        setOnFriday = null
        setOnSaturday = null
        setOnSunday = null

        setRingtoneTitle = null
        setSpecialDate = null
    }
}
