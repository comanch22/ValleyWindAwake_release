package com.comanch.valley_wind_awake.dialogFragments

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.DatePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.comanch.valley_wind_awake.stringKeys.FragmentResultKey
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.SoundPoolForFragments
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DialogDatePicker : DialogFragment(), OnDateSetListener {

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(requireContext(), this, year, month, day)
        datePicker.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            getString(R.string.clear_calendar)
        ) { _, _ ->
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            setFragmentResult(
                FragmentResultKey.selectedDateKey,
                bundleOf(FragmentResultKey.selectedDateExtraKey to FragmentResultKey.clear)
            )
        }
        datePicker.apply {
            setOnShowListener {
                this.getButton(DialogInterface.BUTTON_POSITIVE)
                    .isSoundEffectsEnabled = false
                this.getButton(DialogInterface.BUTTON_NEGATIVE)
                    .isSoundEffectsEnabled = false
                this.getButton(DialogInterface.BUTTON_NEUTRAL)
                    .isSoundEffectsEnabled = false
                this.getButton(DialogInterface.BUTTON_POSITIVE)
                    .textSize = 16F
                this.getButton(DialogInterface.BUTTON_NEGATIVE)
                    .textSize = 16F
                this.getButton(DialogInterface.BUTTON_NEUTRAL)
                    .textSize = 16F
            }
        }
        return datePicker
    }

    override fun onResume() {
        super.onResume()
        soundPoolContainer.setTouchSound()
    }

    override fun onCancel(dialog: DialogInterface) {

        soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
        super.onCancel(dialog)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {

        soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
        val dateBundle = Bundle()
        dateBundle.putInt(FragmentResultKey.year, year)
        dateBundle.putInt(FragmentResultKey.month, month)
        dateBundle.putInt(FragmentResultKey.day, day)
        setFragmentResult(
            FragmentResultKey.selectedDateKey,
            bundleOf(FragmentResultKey.selectedDateExtraKey to bundleOf(FragmentResultKey.dateBundle to dateBundle))
        )
    }
}