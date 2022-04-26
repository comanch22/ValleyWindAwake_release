package com.comanch.valley_wind_awake.frontListFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.comanch.valley_wind_awake.LiveDataEvent
import com.comanch.valley_wind_awake.dataBase.TimeData
import com.comanch.valley_wind_awake.dataBase.TimeDataDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ListViewModel @Inject constructor(val database: TimeDataDao) : ViewModel() {

    lateinit var defaultRingtoneUri: String
    lateinit var defaultRingtoneTitle: String

    val items = database.getAllItems()
    private var isDeleteMode = false

    private val _navigateToKeyboardFragment = MutableLiveData<LiveDataEvent<Long?>>()
    val navigateToKeyboardFragment: LiveData<LiveDataEvent<Long?>>
        get() = _navigateToKeyboardFragment

    private var _timeToast = MutableLiveData<LiveDataEvent<TimeData?>>()
    val timeToast: LiveData<LiveDataEvent<TimeData?>>
        get() = _timeToast

    private val _nearestDate = MutableLiveData<String?>()
    val nearestDate: LiveData<String?>
        get() = _nearestDate

    private var _itemActive = MutableLiveData<TimeData?>()
    val itemActive: LiveData<TimeData?>
        get() = _itemActive

    private var _deleteAllItems = MutableLiveData<List<TimeData>?>()
    val deleteAllItems: LiveData<List<TimeData>?>
        get() = _deleteAllItems

    private val _offAlarm = MutableLiveData<TimeData?>()
    val offAlarm: LiveData<TimeData?>
        get() = _offAlarm

    fun setNearestDate(is24HourFormat: Boolean) {

        var result = ""
        viewModelScope.launch {
            val listItems = database.getListItems()
            listItems?.sortedBy {
                it.nearestDate
            }
            listItems?.filter { it.active }?.sortedBy {
                it.nearestDate
            }?.let {
                result = if (it.isNotEmpty()) {
                    if (is24HourFormat) {
                        it[0].nearestDateStr
                    } else {
                        it[0].nearestDateStr12
                    }
                } else {
                    ""
                }
            }
            _nearestDate.value = result
        }
    }

    fun setDeleteItemsMode(b: Boolean) {
        isDeleteMode = b
    }

    fun insertItem() {

        viewModelScope.launch {
            val item = TimeData()
            item.ringtoneUri = defaultRingtoneUri
            item.ringtoneTitle = defaultRingtoneTitle
            database.insert(item)
        }
    }

    fun clear() {

        viewModelScope.launch {
            _deleteAllItems.value = database.getListItems()
        }
    }

    fun deleteAll() {

        viewModelScope.launch {
            database.clear()
        }

    }

    fun onItemClicked(itemId: Long) {

        if (!isDeleteMode) {
            _navigateToKeyboardFragment.value = LiveDataEvent(itemId)
        }
    }

    fun updateSwitchCheck(item: TimeData) {
        _itemActive.value = item
    }

    fun offAlarmDeleteItem(itemId: Long) {

        viewModelScope.launch {
            val item = database.get(itemId) ?: return@launch
            _offAlarm.value = item
        }
    }

    fun deleteItem(item: TimeData) {

        viewModelScope.launch {
            database.delete(item)
        }
    }

    fun resetItemActive() {
        _itemActive.value = null
    }

    fun resetDeleteAllItems() {
        _deleteAllItems.value = null
    }

    fun showDiffTimeToast(timeId: Long) {

        viewModelScope.launch {
            val item = database.get(timeId) ?: return@launch
            _timeToast.value = LiveDataEvent(item)
        }
    }
}
