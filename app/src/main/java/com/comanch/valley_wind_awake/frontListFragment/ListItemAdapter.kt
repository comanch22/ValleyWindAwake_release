package com.comanch.valley_wind_awake.frontListFragment

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.dataBase.TimeData
import com.comanch.valley_wind_awake.databinding.FrontListItemBinding


class ItemListener(val clickListener: (itemId: Long) -> Unit) {
    fun onClick(time: TimeData) = clickListener(time.timeId)
}

class SwitchListener(val switchListener: (item: TimeData) -> Unit) {
    fun onSwitch(time: TimeData) = switchListener(time)
}

class DeleteListener(val deleteListener: (itemId: Long) -> Unit) {
    fun deleteItem(time: TimeData) = deleteListener(time.timeId)
}

class ListItemAdapter(
    private val clickListener: ItemListener,
    private val switchListener: SwitchListener,
    private val deleteListener: DeleteListener,
    private val colorOn: Int,
    private val colorOff: Int,
    private val backgroundColor: Int,
    private var is24HourFormat: Boolean,
    private val timeInstance: Long,
    private val language: String?,
    private val drawableAlarm64: Int,
    private val drawableAlarm48: Int

) : ListAdapter<DataItem, RecyclerView.ViewHolder>(
    SleepNightDiffCallback()
) {

    private var isDeleteMode = false
    private var mRecyclerView: RecyclerView? = null

    fun setData(list: List<TimeData>?) {

        val items = list?.map { DataItem.AlarmItem(it) }
        submitList(items)
    }

    fun setIs24HourFormat(b: Boolean) {

        is24HourFormat = b
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {

        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    fun setDeleteMode(b: Boolean) {

        isDeleteMode = b
        notifyItemRangeChanged(0, itemCount)
    }

    fun refreshList() {

        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is ViewHolder -> {
                val item = getItem(position) as DataItem.AlarmItem
                holder.bind(
                    item.timeData,
                    clickListener,
                    switchListener,
                    deleteListener,
                    isDeleteMode,
                    colorOn,
                    colorOff,
                    backgroundColor,
                    is24HourFormat,
                    timeInstance,
                    language,
                    drawableAlarm64,
                    drawableAlarm48
                )
            }
        }
    }

    class ViewHolder private constructor(val binding: FrontListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater =
                    LayoutInflater.from(parent.context)
                val binding =
                    FrontListItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(
            item: TimeData,
            clickListener: ItemListener,
            switchListener: SwitchListener,
            deleteListener: DeleteListener,
            isDeleteMode: Boolean,
            colorOn: Int,
            colorOff: Int,
            backgroundColor: Int,
            is24HourFormat: Boolean,
            timeInstance: Long,
            language: String?,
            drawableAlarm64: Int,
            drawableAlarm48: Int
        ) {

            binding.item = item

            val mapView = mapOf(
                binding.textViewMonday to item.mondayOn,
                binding.textViewTuesday to item.tuesdayOn,
                binding.textViewWednesday to item.wednesdayOn,
                binding.textViewThursday to item.thursdayOn,
                binding.textViewFriday to item.fridayOn,
                binding.textViewSaturday to item.saturdayOn,
                binding.textViewSunday to item.sundayOn
            )

            setItemLayoutContentDescription(
                item,
                language,
                is24HourFormat,
                (item.delayTime > 0L && item.delayTime > timeInstance),
                isDeleteMode)

            if (is24HourFormat) {
                setTimeOnView(item.hhmm24)
            } else {
                setTimeOnView(item.hhmm12)
                binding.ampm.text = item.ampm
            }

            binding.selectedDate.text = item.specialDateStr

            SetBackgroundDays(
                colorOn,
                colorOff,
                backgroundColor,
                mapView
            ).setBackgroundDays()

            setElementsVisible(
                isDeleteMode,
                item,
                drawableAlarm64,
                drawableAlarm48,
                is24HourFormat,
                timeInstance
            )

            binding.clickListener = clickListener
            binding.switchListener = switchListener
            binding.deleteListener = deleteListener
        }

        private fun setElementsVisible(
            isDeleteMode: Boolean,
            item: TimeData,
            drawableAlarm64: Int,
            drawableAlarm48: Int,
            is24HourFormat: Boolean,
            timeInstance: Long
        ) {
            if (isDeleteMode) {
                binding.switchActive.visibility = View.GONE
                binding.deleteItem.visibility = View.VISIBLE
            } else {
                binding.deleteItem.visibility = View.GONE
                binding.switchActive.visibility = View.VISIBLE
                if (item.active) {
                    binding.switchActive.setBackgroundResource(drawableAlarm64)
                } else {
                    binding.switchActive.setBackgroundResource(drawableAlarm48)
                }
            }
            if (is24HourFormat) {
                binding.ampm.visibility = View.INVISIBLE
            } else {
                binding.ampm.visibility = View.VISIBLE
                binding.ampm.text = item.ampm
            }

            if (item.delayTime > 0L && item.delayTime > timeInstance) {
                binding.isDelayed.visibility = View.VISIBLE
            } else {
                binding.isDelayed.visibility = View.INVISIBLE
            }
        }

        private fun setTimeOnView(hhmm: String) {

            binding.textViewNumberOne.text = hhmm[0].toString()
            binding.textViewNumberTwo.text = hhmm[1].toString()
            binding.textViewNumberThree.text = hhmm[2].toString()
            binding.textViewNumberFour.text = hhmm[3].toString()
        }

        private fun setItemLayoutContentDescription(
            item: TimeData,
            language: String?,
            is24HourFormat: Boolean,
            isDelayed: Boolean,
            isDeleteMode: Boolean
        ) {

            binding.itemLayout.contentDescription =
                if (language == "ru_RU") {
                    when {
                        is24HourFormat && isDelayed && item.active -> {
                            item.contentDescriptionRu24 + " сигнал отложен. будильник включен. "
                        }
                        is24HourFormat && isDelayed && !item.active -> {
                            item.contentDescriptionRu24 + " сигнал отложен. будильник выключен. "
                        }
                        is24HourFormat && !isDelayed && item.active -> {
                            item.contentDescriptionRu24 + " будильник включен. "
                        }
                        is24HourFormat && !isDelayed && !item.active -> {
                            item.contentDescriptionRu24 + " будильник выключен. "
                        }
                        !is24HourFormat && isDelayed && item.active -> {
                            item.contentDescriptionRu12 + " сигнал отложен. будильник включен. "
                        }
                        !is24HourFormat && isDelayed && !item.active -> {
                            item.contentDescriptionRu12 + " сигнал отложен. будильник выключен. "
                        }
                        !is24HourFormat && !isDelayed && item.active -> {
                            item.contentDescriptionRu12 + " будильник включен. "
                        }
                        !is24HourFormat && !isDelayed && !item.active -> {
                            item.contentDescriptionRu12 + " будильник выключен. "
                        }
                        else -> {
                            " ошибка, состояние будильника неизвестно. "
                        }
                    }
                } else {
                    when {
                        is24HourFormat && isDelayed && item.active -> {
                            item.contentDescriptionEn24 + " the signal is delayed. the alarm clock is on. "
                        }
                        is24HourFormat && isDelayed && !item.active -> {
                            item.contentDescriptionEn24 + " the signal is delayed. the alarm clock is off. "
                        }
                        is24HourFormat && !isDelayed && item.active -> {
                            item.contentDescriptionEn24 + " the alarm clock is on. "
                        }
                        is24HourFormat && !isDelayed && !item.active -> {
                            item.contentDescriptionEn24 + " the alarm clock is off. "
                        }
                        !is24HourFormat && isDelayed && item.active -> {
                            item.contentDescriptionEn12 + " the signal is delayed. the alarm clock is on. "
                        }
                        !is24HourFormat && isDelayed && !item.active -> {
                            item.contentDescriptionEn12 + " the signal is delayed. the alarm clock is off. "
                        }
                        !is24HourFormat && !isDelayed && item.active -> {
                            item.contentDescriptionEn12 + " the alarm clock is on. "
                        }
                        !is24HourFormat && !isDelayed && !item.active -> {
                            item.contentDescriptionEn12 + " the alarm clock is off. "
                        }
                        else -> {
                            " error, the alarm clock status is unknown. "
                        }
                    }
                }

            if (isDeleteMode) {
                if (language == "ru_RU") {
                    binding.itemLayout.contentDescription =
                        " Включен режим удаления будильников. Будильник из списка в режиме выбора для удаления. " +
                                " Время будильника. " +
                                "${binding.textViewNumberOne.text}${binding.textViewNumberTwo.text} часов" +
                                "${binding.textViewNumberThree.text}${binding.textViewNumberFour.text} минут"
                } else {
                    binding.itemLayout.contentDescription =
                        " Alarm removal mode is enabled. Alarm clock from the list in the selection mode for deletion." +
                                " Alarm clock time. " +
                                "${binding.textViewNumberOne.text}${binding.textViewNumberTwo.text} hours" +
                                "${binding.textViewNumberThree.text}${binding.textViewNumberFour.text} minutes"
                }

            } else {
                if (item.active) {
                    if (language == "ru_RU") {
                        binding.switchActive.contentDescription = " будильник включен. "
                    } else {
                        binding.switchActive.contentDescription = " the alarm is on. "
                    }
                } else {
                    if (language == "ru_RU") {
                        binding.switchActive.contentDescription = " будильник выключен. "
                    } else {
                        binding.switchActive.contentDescription = " the alarm is off. "
                    }
                }
            }
            if (language == "ru_RU") {
                binding.deleteItem.contentDescription = " удалить будильник из списка. "
            }else{
                binding.deleteItem.contentDescription = " remove the alarm from the list. "
            }
        }

    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}

class SleepNightDiffCallback : DiffUtil.ItemCallback<DataItem>() {

    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}

sealed class DataItem {

    abstract val id: Long

    data class AlarmItem(val timeData: TimeData) : DataItem() {
        override val id = timeData.timeId
    }
}

class SetBackgroundDays(
    private val colorOn: Int,
    private val colorOff: Int,
    private val backgroundColor: Int,
    private val mapView: Map<TextView, Boolean>
) {

    fun setBackgroundDays() {

        mapView.forEach { (key, value) ->
            if (value) {
                setBackgroundDaysOn(key)
            } else {
                setBackgroundDaysOff(key)
            }
        }
    }

    private fun setBackgroundDaysOn(view: TextView) {

        view.setBackgroundResource(R.drawable.rectangle_list_background_day)
        view.setTypeface(null, Typeface.BOLD)
        view.setTextColor(colorOn)
    }

    private fun setBackgroundDaysOff(view: TextView) {

        view.setBackgroundColor(backgroundColor)
        view.setTypeface(null, Typeface.NORMAL)
        view.setTextColor(colorOff)
    }
}

