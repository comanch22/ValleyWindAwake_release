package com.comanch.valley_wind_awake.keyboardFragment

import androidx.lifecycle.*
import com.comanch.valley_wind_awake.LiveDataEvent
import com.comanch.valley_wind_awake.dataBase.TimeData
import com.comanch.valley_wind_awake.dataBase.TimeDataDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class KeyboardViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val database: TimeDataDao
) : ViewModel() {

    private var ringtoneUri: String = ""
    var localItemId = -1L

    private val _specialDateStr = MutableLiveData<String?>()
    val specialDateStr: LiveData<String?>
        get() = _specialDateStr

    private val _specialDate = MutableLiveData<Long?>()
    val specialDate: LiveData<Long?>
        get() = _specialDate

    private val _s1 = MutableLiveData<String?>()
    val s1: LiveData<String?>
        get() = _s1

    private val _s2 = MutableLiveData<String?>()
    val s2: LiveData<String?>
        get() = _s2

    private val _s3 = MutableLiveData<String?>()
    val s3: LiveData<String?>
        get() = _s3

    private val _s4 = MutableLiveData<String?>()
    val s4: LiveData<String?>
        get() = _s4

    private val _numbersIsUpdated = MutableLiveData<Int?>()
    val numbersIsUpdated: LiveData<Int?>
        get() = _numbersIsUpdated

    private var _hhmm12 = MutableLiveData<String?>()
    val hhmm12: LiveData<String?>
        get() = _hhmm12

    private var _hhmm24 = MutableLiveData<String?>()
    val hhmm24: LiveData<String?>
        get() = _hhmm24

    private val _is24HourFormat = MutableLiveData<Boolean?>()
    val is24HourFormat: LiveData<Boolean?>
        get() = _is24HourFormat

    private val _ampm = MutableLiveData<String?>()
    val ampm: LiveData<String?>
        get() = _ampm

    private val _hhmm = MutableLiveData<String>()
    val hhmm: LiveData<String>
        get() = _hhmm

    private val _hhmmSave = MutableLiveData<String>()
    val hhmmSave: LiveData<String>
        get() = _hhmmSave

    private val _monday = MutableLiveData<Boolean?>()
    val monday: LiveData<Boolean?>
        get() = _monday

    private val _tuesday = MutableLiveData<Boolean?>()
    val tuesday: LiveData<Boolean?>
        get() = _tuesday

    private val _wednesday = MutableLiveData<Boolean?>()
    val wednesday: LiveData<Boolean?>
        get() = _wednesday

    private val _thursday = MutableLiveData<Boolean?>()
    val thursday: LiveData<Boolean?>
        get() = _thursday

    private val _friday = MutableLiveData<Boolean?>()
    val friday: LiveData<Boolean?>
        get() = _friday

    private val _saturday = MutableLiveData<Boolean?>()
    val saturday: LiveData<Boolean?>
        get() = _saturday

    private val _sunday = MutableLiveData<Boolean?>()
    val sunday: LiveData<Boolean?>
        get() = _sunday

    private var _newAlarm = MutableLiveData<LiveDataEvent<TimeData?>>()
    val newAlarm: LiveData<LiveDataEvent<TimeData?>>
        get() = _newAlarm

    private var _deleteAlarm = MutableLiveData<LiveDataEvent<TimeData?>>()
    val deleteAlarm: LiveData<LiveDataEvent<TimeData?>>
        get() = _deleteAlarm

    private var _timeToast = MutableLiveData<LiveDataEvent<TimeData?>>()
    val timeToast: LiveData<LiveDataEvent<TimeData?>>
        get() = _timeToast

    private val _errorForUser = MutableLiveData<String?>()
    val errorForUser: LiveData<String?>
        get() = _errorForUser

    private val _save = MutableLiveData<LiveDataEvent<Int?>>()
    val save: LiveData<LiveDataEvent<Int?>>
        get() = _save

    private var _setTimeIsReady = MutableLiveData<LiveDataEvent<Int?>>()
    val setTimeIsReady: LiveData<LiveDataEvent<Int?>>
        get() = _setTimeIsReady

    private val _setRingtoneTitle = MutableLiveData<LiveDataEvent<String?>>()
    val setRingtoneTitle: LiveData<LiveDataEvent<String?>>
        get() = _setRingtoneTitle

    val backgroundTimerChanged
        get() = savedStateHandle.getLiveData("backgroundTimerChanged", 0)

    fun setTime(
        itemId: Long,
        correspondent: Correspondent,
        _ringtoneUri: String,
        _ringtoneTitle: String
    ) {
        when (correspondent) {
            Correspondent.ListFragment -> {
                viewModelScope.launch {
                    val item = database.get(itemId) ?: return@launch
                    setTimerNumbers(item)
                    _ampm.value = item.ampm
                    _monday.value = item.mondayOn
                    _tuesday.value = item.tuesdayOn
                    _wednesday.value = item.wednesdayOn
                    _thursday.value = item.thursdayOn
                    _friday.value = item.fridayOn
                    _saturday.value = item.saturdayOn
                    _sunday.value = item.sundayOn
                    localItemId = itemId
                    _setRingtoneTitle.value = LiveDataEvent(item.ringtoneTitle)
                    ringtoneUri = item.ringtoneUri
                    _specialDateStr.value = item.specialDateStr
                    _specialDate.value = item.specialDate
                    _setTimeIsReady.value = LiveDataEvent(1)
                }
            }
            Correspondent.RingtoneFragment -> {
                _setRingtoneTitle.value = LiveDataEvent(_ringtoneTitle)
                if (_ringtoneUri.isNotEmpty()) {
                    ringtoneUri = _ringtoneUri
                } else {
                    viewModelScope.launch {
                        val item = database.get(itemId) ?: return@launch
                        ringtoneUri = item.ringtoneUri
                    }
                }
                localItemId = itemId
            }
            Correspondent.FragmentRotation -> {
                if (_ringtoneTitle.isNotEmpty()) {
                    _setRingtoneTitle.value = LiveDataEvent(_ringtoneTitle)
                } else {
                    viewModelScope.launch {
                        val item = database.get(itemId) ?: return@launch
                        _setRingtoneTitle.value = LiveDataEvent(item.ringtoneTitle)
                    }
                }
                if (_ringtoneUri.isNotEmpty()) {
                    ringtoneUri = _ringtoneUri
                } else {
                    viewModelScope.launch {
                        val item = database.get(itemId) ?: return@launch
                        ringtoneUri = item.ringtoneUri
                    }
                }
                localItemId = itemId
            }
            else -> {
                localItemId = itemId
            }
        }
    }

    fun setTimerNumbers(item: TimeData) {

        if (is24HourFormat.value == true) {
                _s1.value = item.hhmm24[0].toString()
                _s2.value = item.hhmm24[1].toString()
                _s3.value = item.hhmm24[2].toString()
                _s4.value = item.hhmm24[3].toString()
            }
            else{
                _s1.value = item.hhmm12[0].toString()
                _s2.value = item.hhmm12[1].toString()
                _s3.value = item.hhmm12[2].toString()
                _s4.value = item.hhmm12[3].toString()
            }
    }

    fun prepareSave() {

        _hhmmSave.value = "${s1.value}${s2.value}${s3.value}${s4.value}"
    }

    fun saveTimer() {

        val newTime = setNewTime()
        viewModelScope.launch {
            val item = database.get(localItemId) ?: return@launch
            if (item != newTime) {
                item.s1 = newTime.s1
                item.s2 = newTime.s2
                item.s3 = newTime.s3
                item.s4 = newTime.s4
                item.ampm = newTime.ampm
                item.hhmm12 = newTime.hhmm12
                item.hhmm24 = newTime.hhmm24
                item.mondayOn = newTime.mondayOn
                item.tuesdayOn = newTime.tuesdayOn
                item.wednesdayOn = newTime.wednesdayOn
                item.thursdayOn = newTime.thursdayOn
                item.fridayOn = newTime.fridayOn
                item.saturdayOn = newTime.saturdayOn
                item.sundayOn = newTime.sundayOn
                if (specialDate.value ?: 0 > 0L){
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = specialDate.value!!
                    when (is24HourFormat.value) {
                        true -> {
                            calendar.set(Calendar.HOUR_OF_DAY, "${item.s1}${item.s2}".toInt())
                            calendar.set(Calendar.MINUTE, "${item.s3}${item.s4}".toInt())
                            calendar.set(Calendar.SECOND, 0)
                            calendar.clear(Calendar.MILLISECOND)
                        }
                        false -> {
                            calendar.set(Calendar.HOUR, "${item.s1}${item.s2}".toInt())
                            calendar.set(Calendar.MINUTE, "${item.s3}${item.s4}".toInt())
                            calendar.set(
                                Calendar.AM_PM,
                                if (ampm.value == "PM") Calendar.PM else Calendar.AM
                            )
                            calendar.set(Calendar.SECOND, 0)
                            calendar.clear(Calendar.MILLISECOND)
                        }
                        else -> {

                        }
                    }
                    item.specialDate = calendar.timeInMillis
                }
                item.specialDateStr = newTime.specialDateStr
                if (item.timeId < 999999999) {
                    item.requestCode = item.timeId.toInt()
                } else {
                    _errorForUser.value = "reinstall"
                    return@launch
                }
                item.ringtoneUri = ringtoneUri
                item.ringtoneTitle = setRingtoneTitle.value?.getContent() ?: ""
                item.delayTime = 0L
                database.update(item)

                val updatedItem = database.get(localItemId) ?: return@launch
                _newAlarm.value = LiveDataEvent(updatedItem)
            } else {
                _save.value = LiveDataEvent(2)
            }
        }

    }

    fun prepareSaveTimer() {

        viewModelScope.launch {
            val item = database.get(localItemId) ?: return@launch
            _deleteAlarm.value = LiveDataEvent(item)
        }
    }

    private fun setNewTime(): TimeData {

        val newTime = TimeData()

        newTime.s1 = _s1.value ?: "0"
        newTime.s2 = _s2.value ?: "0"
        newTime.s3 = _s3.value ?: "0"
        newTime.s4 = _s4.value ?: "0"
        if (hhmm12.value != null &&
            hhmm24.value != null
        ) {
            newTime.hhmm12 = hhmm12.value ?: "1200"
            newTime.hhmm24 = hhmm24.value ?: "0000"
        } else {
            _errorForUser.value = "restart"
        }
        newTime.active = true
        newTime.mondayOn = _monday.value == true
        newTime.tuesdayOn = _tuesday.value == true
        newTime.wednesdayOn = _wednesday.value == true
        newTime.thursdayOn = _thursday.value == true
        newTime.fridayOn = _friday.value == true
        newTime.saturdayOn = _saturday.value == true
        newTime.sundayOn = _sunday.value == true
        newTime.specialDateStr = specialDateStr.value ?: ""
        newTime.ampm = ampm.value ?: "AM"

        return newTime
    }

    fun fromKeyBoardLayout(num: Int) {

        when (num) {
            in 0..9 -> setNumberOnTimer(num)
            10 -> backSpaceNumber()
            12 -> prepareSave()
        }
    }

    fun selectDayOfWeek(num: Int) {

        when (num) {
            1 -> _monday.value = monday.value == false
            2 -> _tuesday.value = tuesday.value == false
            3 -> _wednesday.value = wednesday.value == false
            4 -> _thursday.value = thursday.value == false
            5 -> _friday.value = friday.value == false
            6 -> _saturday.value = saturday.value == false
            7 -> _sunday.value = sunday.value == false
        }
    }

    private fun setPositionOnTimer(num: Int) {

        when (num) {
            in 1..2 -> {
                savedStateHandle.set("positionOnTimer", 1)
                savedStateHandle.set("backgroundTimerChanged", 1)
            }
            in 3..4 -> {
                savedStateHandle.set("positionOnTimer", 3)
                savedStateHandle.set("backgroundTimerChanged", 3)
            }
        }
        savedStateHandle.set("stopDeleteNumbers", false)
        savedStateHandle.set("inputNumberFromTimer", true)
    }

    private fun setNumberOnTimer(num: Int) {

        savedStateHandle.set("inputNumberFromTimer", false)
        savedStateHandle.set("stopDeleteNumbers", false)
        val positionOnTimer = savedStateHandle.getLiveData("positionOnTimer", 1).value ?: 1
        var wrongNumber = false

        when (positionOnTimer) {
            1 -> {
                if (is24HourFormat.value == true && num in 0..2
                    || num in 0..1
                ) {
                    _s1.value = num.toString()
                } else {
                    wrongNumber = true
                }
            }
            2 -> {
                if (is24HourFormat.value == true && _s1.value == "2" && num !in 0..3) {
                    wrongNumber = true
                } else {
                    _s2.value = num.toString()
                }
            }
            3 -> {
                if (num in 0..5) {
                    _s3.value = num.toString()
                } else {
                    wrongNumber = true
                }
            }
            4 -> _s4.value = num.toString()
        }
        if (!wrongNumber) {
            savedStateHandle.set("backgroundTimerChanged", positionOnTimer)
            if (positionOnTimer < 4) {
                savedStateHandle.set("positionOnTimer", positionOnTimer + 1)
            } else {
                savedStateHandle.set("positionOnTimer", 1)
            }
        }
    }

    private fun backSpaceNumber() {

        var positionOnTimer = savedStateHandle.getLiveData("positionOnTimer", 1).value ?: 1
        val deleteNumber =
            savedStateHandle.getLiveData("inputNumberFromTimer", false).value ?: false
        val stopDeleteNumbers =
            savedStateHandle.getLiveData("stopDeleteNumbers", false).value ?: false
        if (!stopDeleteNumbers) {
            if (deleteNumber) {
                when (positionOnTimer) {
                    1 -> savedStateHandle.set("positionOnTimer", 3)
                    3 -> savedStateHandle.set("positionOnTimer", 1)
                }
            }
            positionOnTimer = savedStateHandle.getLiveData("positionOnTimer", 1).value ?: 1
            when (positionOnTimer) {
                1 -> _s4.value = "0"
                2 -> {
                    _s1.value = "0"
                    savedStateHandle.set("stopDeleteNumbers", true)
                }
                3 -> _s2.value = "0"
                4 -> _s3.value = "0"
            }
            if (positionOnTimer > 1) {
                savedStateHandle.set("positionOnTimer", positionOnTimer - 1)
                savedStateHandle.set("backgroundTimerChanged", positionOnTimer - 1)
            } else {
                savedStateHandle.set("positionOnTimer", 4)
                savedStateHandle.set("backgroundTimerChanged", 4)
            }
            savedStateHandle.set("inputNumberFromTimer", false)
        }
    }

    fun showDiffTimeToast(timeId: Long) {

        viewModelScope.launch {
            val item = database.get(timeId) ?: return@launch
            _timeToast.value = LiveDataEvent(item)
        }
    }

    fun fromTimerLayout(pos: Int) {
        setPositionOnTimer(pos)
    }

    fun setNumbersTimer(s: String) {

        _s1.value = s[0].toString()
        _s2.value = s[1].toString()
        _s3.value = s[2].toString()
        _s4.value = s[3].toString()
        _ampm.value = "${s[4]}${s[5]}"
        _numbersIsUpdated.value = 1
    }

    fun setOnMonday(b: Boolean) {
        _monday.value = b
    }

    fun setOnTuesday(b: Boolean) {
        _tuesday.value = b
    }

    fun setOnWednesday(b: Boolean) {
        _wednesday.value = b
    }

    fun setOnThursday(b: Boolean) {
        _thursday.value = b
    }

    fun setOnFriday(b: Boolean) {
        _friday.value = b
    }

    fun setOnSaturday(b: Boolean) {
        _saturday.value = b
    }

    fun setOnSunday(b: Boolean) {
        _sunday.value = b
    }

    fun setSpecialDateStr(s: String) {
        _specialDateStr.value = s
    }

    fun clearSpecialDate() {
        _specialDateStr.value = ""
        _specialDate.value = 0L
        viewModelScope.launch {
            val item = database.get(localItemId) ?: return@launch
            item.specialDate = 0L
            item.specialDateStr = ""
            database.update(item)
        }
    }

    fun setSpecialDate(specialDate: Long) {
        _specialDate.value = specialDate
    }

    fun upDateTimeView(s: String) {

        _s1.value = s[0].toString()
        _s2.value = s[1].toString()
        _s3.value = s[2].toString()
        _s4.value = s[3].toString()
    }

    fun refreshViewDateFormat() {

        if (s1.value != null &&
            s2.value != null &&
            s3.value != null &&
            s4.value != null
        ) {
            _hhmm.value = "${s1.value}${s2.value}${s3.value}${s4.value}${ampm.value}"
        }
    }

    fun setAMPM(s: String?) {
        _ampm.value = s
    }

    fun setHhmm12(s: String?) {
        _hhmm12.value = s
    }

    fun setHhmm24(s: String?) {
        _hhmm24.value = s
    }

    fun setIs24HourFormat(b: Boolean?) {
        _is24HourFormat.value = b
    }
}


