package com.comanch.valley_wind_awake.ringtonePickerFragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.dataBase.RingtoneData
import com.comanch.valley_wind_awake.databinding.RingtonePickerItemBinding

class ItemListener(val clickListener: (ringtoneData: RingtoneData) -> Unit) {

    fun onClick(ringtone: RingtoneData) {
        return clickListener(ringtone)
    }

}

class RingtoneAdapter(
    private val clickListener: ItemListener,
    var _ringtoneList: MutableList<RingtoneData>,
    private val colorAccent: Int?,
    private val language: String?
) :
    ListAdapter<RingtoneData, RecyclerView.ViewHolder>(RingtoneItemDiffCallback()) {

    fun setData(list: List<RingtoneData>) {
        submitList(list + _ringtoneList)
    }


    override fun getItemCount(): Int {
        return currentList.size
    }

    fun getRingtone(position: Int): RingtoneData? {
        return if (position >= 0 && currentList.size.minus(1) >= position) super.getItem(position) else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is ViewHolder -> {
                val item = getItem(position)
                holder.bind(item, clickListener, colorAccent, language)
            }
        }

    }

    class ViewHolder private constructor(val binding: RingtonePickerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater =
                    LayoutInflater.from(parent.context)
                val binding =
                    RingtonePickerItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(ringtone: RingtoneData, clickListener: ItemListener, colorAccent: Int?, language: String?) {

            binding.ringtone = ringtone
            binding.TITLE.text = ringtone.title
            binding.clickListener = clickListener
            if (ringtone.active == 1) {
                binding.TITLE.setBackgroundResource(R.drawable.rectangle_ringtone_adapter)
                if (language == "ru_RU") {
                    binding.ringtonePickerLayout.contentDescription = " эта мелодия выбрана и играет "
                } else {
                    binding.ringtonePickerLayout.contentDescription = " this melody is selected and is playing "
                }
            } else {
                binding.TITLE.setBackgroundColor(colorAccent ?: Color.WHITE)
                if (language == "ru_RU") {
                    binding.ringtonePickerLayout.contentDescription = " эта мелодия не выбрана "
                } else {
                    binding.ringtonePickerLayout.contentDescription = " this melody is not selected "
                }
            }
        }

    }

    override fun onCurrentListChanged(
        previousList: MutableList<RingtoneData>,
        currentList: MutableList<RingtoneData>
    ) {
        var i = 0
        currentList.forEach {
            it.position = i
            i++
        }
        super.onCurrentListChanged(previousList, currentList)
    }

}

class RingtoneItemDiffCallback : DiffUtil.ItemCallback<RingtoneData>() {
    override fun areItemsTheSame(oldItem: RingtoneData, newItem: RingtoneData): Boolean {
        return oldItem.uriAsString == newItem.uriAsString && oldItem.active == newItem.active
                && oldItem.title == newItem.title
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: RingtoneData, newItem: RingtoneData): Boolean {
        return oldItem == newItem
    }
}
