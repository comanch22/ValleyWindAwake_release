package com.comanch.valley_wind_awake.ringtonePickerCustomFragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.dataBase.RingtoneData
import com.comanch.valley_wind_awake.databinding.RingtoneCustomPickerItemBinding

class ItemListener(val clickListener: (ringtoneData: RingtoneData) -> Unit) {

    fun onClick(ringtone: RingtoneData) {
        return clickListener(ringtone)
    }
}

class RingtoneCustomAdapter(
    private val clickListener: ItemListener,
    private val language: String?) :
    ListAdapter<RingtoneData, RecyclerView.ViewHolder>(RingtoneItemDiffCallback()) {

    fun setData(list: List<RingtoneData>?) {

        list?.let {
            submitList(list)
        }
    }

    fun getIt(position: Int): RingtoneData? {

        return if (position >= 0 && currentList.size.minus(1) >= position) super.getItem(position) else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            is ViewHolder -> {
                val item = getItem(position)
                holder.bind(item, clickListener, language)
            }
        }
    }

    class ViewHolder private constructor(val binding: RingtoneCustomPickerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater =
                    LayoutInflater.from(parent.context)
                val binding =
                    RingtoneCustomPickerItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(ringtone: RingtoneData, clickListener: ItemListener, language: String?) {

            binding.ringtone = ringtone
            binding.title.text = ringtone.title
            binding.album.text = ringtone.album
            binding.artist.text = ringtone.artist
            binding.duration.text = ringtone.duration
            binding.clickListener = clickListener
            if (ringtone.active == 1) {
                binding.ringtoneCustomList.setBackgroundResource(R.drawable.rectangle_for_list_white)
                if (language == "ru_RU") {
                    binding.ringtoneCustomList.contentDescription = " эта мелодия выбрана и играет "
                } else {
                    binding.ringtoneCustomList.contentDescription = " this melody is selected and is playing "
                }
            } else {
                binding.ringtoneCustomList.setBackgroundResource(R.drawable.rectangle_for_list)
                if (language == "ru_RU") {
                    binding.ringtoneCustomList.contentDescription = " эта мелодия не выбрана "
                } else {
                    binding.ringtoneCustomList.contentDescription = " this melody is not selected "
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
