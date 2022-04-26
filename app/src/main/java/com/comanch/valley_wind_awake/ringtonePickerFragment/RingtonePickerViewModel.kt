package com.comanch.valley_wind_awake.ringtonePickerFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comanch.valley_wind_awake.LiveDataEvent
import com.comanch.valley_wind_awake.dataBase.RingtoneDataDao
import com.comanch.valley_wind_awake.dataBase.RingtoneData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RingtonePickerViewModel @Inject constructor(val database: RingtoneDataDao) : ViewModel() {

    var items: LiveData<List<RingtoneData>> = database.getAllItems()

    private var currentRingToneId: Long? = null
    private var currentRingToneIsCustom: Int? = null
    private var setRestorePlayerFlag: Boolean = false

    private val _currentRingTone = MutableLiveData<RingtoneData?>()
    val currentRingTone: LiveData<RingtoneData?>
        get() = _currentRingTone

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?>
        get() = _toast

    private val _delete = MutableLiveData<Int?>()
    val delete: LiveData<Int?>
        get() = _delete

    private val _setRingtoneUri = MutableLiveData<LiveDataEvent<String?>>()
    val setRingtoneUri: LiveData<LiveDataEvent<String?>>
        get() = _setRingtoneUri

    private var _setTouchSoundAndVolume = MutableLiveData<LiveDataEvent<Int?>>()
    val setTouchSoundAndVolume: LiveData<LiveDataEvent<Int?>>
        get() = _setTouchSoundAndVolume

    private val _setRingtoneTitle = MutableLiveData<LiveDataEvent<String?>>()
    val setRingtoneTitle: LiveData<LiveDataEvent<String?>>
        get() = _setRingtoneTitle

    private val _itemActiveState = MutableLiveData<LiveDataEvent<Boolean>>()
    val itemActiveState: LiveData<LiveDataEvent<Boolean>>
        get() = _itemActiveState

    private val _chooseRingtone = MutableLiveData<LiveDataEvent<Int?>>()
    val chooseRingtone: LiveData<LiveDataEvent<Int?>>
        get() = _chooseRingtone

    private val _restorePlayerFlag = MutableLiveData<LiveDataEvent<Boolean>>()
    val restorePlayerFlag: LiveData<LiveDataEvent<Boolean>>
        get() = _restorePlayerFlag

    fun onItemClicked(ringtoneData: RingtoneData) {

        currentRingToneId = ringtoneData.melodyId
        currentRingToneIsCustom = ringtoneData.isCustom
        _currentRingTone.value = ringtoneData
    }

    fun setMelody() {

        if (currentRingTone.value?.active == 1) {
            _setRingtoneUri.value = LiveDataEvent(currentRingTone.value?.uriAsString)
            _setRingtoneTitle.value = LiveDataEvent(currentRingTone.value?.title)
        } else {
            _setRingtoneUri.value = LiveDataEvent("")
            _setRingtoneTitle.value = LiveDataEvent("")
        }
    }

    fun addMelody() {
        _chooseRingtone.value = LiveDataEvent(1)
    }

    fun deleteMelody() {
        delete(currentRingToneId, currentRingToneIsCustom)
    }

    fun delete(currentRingToneId_ : Long?, currentRingToneIsCustom_: Int?) {
        if (currentRingToneId_ != null && currentRingToneIsCustom_ != null) {
            if (currentRingToneIsCustom_ == 1) {
                viewModelScope.launch {
                    val item = currentRingToneId_.let { database.get(it) }
                    deleteItem(item)
                }
                _toast.value = null
            } else {
                _toast.value = "cannot be deleted"
            }
        } else {
            _toast.value = "choose a ringtone"
        }
    }

    suspend fun deleteItem(item: RingtoneData?) {
        if (item != null) {
            database.delete(item)
            _delete.value = 1
        } else {
            _toast.value = "choose a ringtone"
        }
    }

    fun setItemActiveState() {
        _itemActiveState.value = LiveDataEvent(true)
    }

    fun restoreStateForRingtoneFragment() {
        _restorePlayerFlag.value = LiveDataEvent(setRestorePlayerFlag)
    }

    fun setRestorePlayerFlag(b: Boolean) {
        setRestorePlayerFlag = b
    }

    fun resetCurrentRingTone() {
        _currentRingTone.value = null
    }

    fun resetCurrentRingtoneValue() {
        currentRingToneId = null
        currentRingToneIsCustom = null
    }

    fun resetDelete(){
        _delete.value = null
    }

    fun resetToast(){
        _toast.value = null
    }
    fun setTouchSoundAndVolume() {
        _setTouchSoundAndVolume.value = LiveDataEvent(1)
    }
}