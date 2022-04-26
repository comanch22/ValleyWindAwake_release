package com.comanch.valley_wind_awake.keyboardFragment

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.RingtoneManager
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.comanch.valley_wind_awake.*
import com.comanch.valley_wind_awake.stringKeys.FragmentResultKey
import com.comanch.valley_wind_awake.stringKeys.OperationKey
import com.comanch.valley_wind_awake.alarmManagement.AlarmControl
import com.comanch.valley_wind_awake.alarmManagement.AlarmTypeOperation
import com.comanch.valley_wind_awake.databinding.KeyboardFragmentBinding
import com.comanch.valley_wind_awake.dialogFragments.DialogDatePicker
import com.comanch.valley_wind_awake.stateViewModel.StateViewModel
import com.comanch.valley_wind_awake.viewTags.ViewTags
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class KeyboardFragment : Fragment() {

    private val stateViewModel: StateViewModel by activityViewModels()
    val keyboardViewModel: KeyboardViewModel by viewModels()

    @Inject
    lateinit var alarmControl: AlarmControl

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private val args: KeyboardFragmentArgs by navArgs()

    private var viewOne: TextView? = null
    private var viewTwo: TextView? = null
    private var viewThree: TextView? = null
    private var viewFour: TextView? = null
    private var selectedRingtoneTitle: String = ""
    private var isRotation = false
    private var colorAccent: Int? = null
    private var is24HourFormat: Boolean? = null
    private var previousIs24HourFormat: Boolean? = null
    private var ampm: String? = null
    private var isStarted = false
    private var isPaused = false
    private var s1 = "0"
    private var s2 = "0"
    private var s3 = "0"
    private var s4 = "0"
    private var specialDateStr = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val action = KeyboardFragmentDirections.actionKeyboardFragmentToListFragment()
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@KeyboardFragment,
                action
            )
        }
        callback.isEnabled = true

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }

        is24HourFormat = DateFormat.is24HourFormat(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: KeyboardFragmentBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.keyboard_fragment, container, false
            )

        if (args.correspondent != Correspondent.ListFragment) {
            stateViewModel.restoreStateForKeyboardFragment()
        } else {
            stateViewModel.restoreTimer()
        }

        isRotation = savedInstanceState?.getBoolean("isRotation", false) == true

        binding.keyboardViewModel = keyboardViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        keyboardViewModel.setIs24HourFormat(is24HourFormat)

        val colorAccentValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorPrimaryVariant, colorAccentValue, true)
        colorAccent = colorAccentValue.data

        keyboardViewModel.setTime(
            args.itemId,
            if (isRotation) Correspondent.FragmentRotation else args.correspondent,
            args.ringtoneUri,
            args.ringtoneTitle
        )

        savedInstanceState?.putBoolean("isRotation", false)

        keyboardViewModel.setTimeIsReady.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.refreshViewDateFormat()
                isStarted = true
            }
        }

        keyboardViewModel.numbersIsUpdated.observe(viewLifecycleOwner) { content ->
            content?.let {
                previousIs24HourFormat = stateViewModel.getIs24HourFormat()
                keyboardViewModel.refreshViewDateFormat()
            }
        }

        keyboardViewModel.is24HourFormat.observe(viewLifecycleOwner) {
            it?.let {
                if (it) {
                    binding.ampmKey.visibility = View.INVISIBLE
                    binding.ampm.visibility = View.INVISIBLE
                } else {
                    binding.ampmKey.visibility = View.VISIBLE
                    binding.ampm.visibility = View.VISIBLE
                }
                stateViewModel.setIs24HourFormat(it)
                is24HourFormat = it
                if (isPaused) {
                    keyboardViewModel.refreshViewDateFormat()
                }
            }
        }

        keyboardViewModel.setRingtoneTitle.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                stateViewModel.setRingtoneTitle(it)
                selectedRingtoneTitle = it
                binding.trackTitle.text = it
            }
        }

        keyboardViewModel.specialDateStr.observe(viewLifecycleOwner) { specialDate ->
            specialDate?.let {
                stateViewModel.setSpecialDate(it)
                binding.selectedDate.text = it
            }
        }

        keyboardViewModel.deleteAlarm.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                alarmControl.timeData = it
                lifecycleScope.launch {
                    if (alarmControl.schedulerAlarm(AlarmTypeOperation.DELETE)
                        ==  OperationKey.success){
                        keyboardViewModel.saveTimer()
                    }else{
                        Toast.makeText(
                            context,
                            resources.getString(R.string.restart),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        keyboardViewModel.newAlarm.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                if (it.ringtoneUri.isEmpty()) {
                    it.ringtoneUri =
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
                }
                lifecycleScope.launch {
                    context?.applicationContext.let { appContext ->
                        if (appContext != null) {
                            alarmControl.timeData = it
                            when (alarmControl.schedulerAlarm(AlarmTypeOperation.SAVE)
                            ) {
                                OperationKey.success -> {
                                    stateViewModel.resetStateStoreForKeyboardFragment()
                                    keyboardViewModel.showDiffTimeToast(it.timeId)
                                }
                                OperationKey.incorrectDate -> {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.time_is_over),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                OperationKey.incorrectSpecialDate -> {
                                    keyboardViewModel.setTimerNumbers(it)
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.incorrect_special_date),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        keyboardViewModel.timeToast.observe(viewLifecycleOwner) { timeData ->

            timeData.getContentIfNotHandled()?.let {
                Log.e("fdgdfgbgdg", "222 id = ${it.timeId}")
                Toast.makeText(
                    context,
                    DateDifference().getResultString(
                        it.nearestDate - Calendar.getInstance().timeInMillis,
                        resources.getString(R.string.days),
                        resources.getString(R.string.hours),
                        resources.getString(R.string.min)
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            navigation.navigateToDestination(
                this,
                KeyboardFragmentDirections.actionKeyboardFragmentToListFragment()
            )
        }

        keyboardViewModel.errorForUser.observe(viewLifecycleOwner) {
            it?.let {
                var message = ""
                when (it) {
                    "reinstall" -> {
                        message = resources.getString(R.string.reinstall)
                    }
                    "restart" -> {
                        message = resources.getString(R.string.restart)
                    }
                }
                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        keyboardViewModel.save.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let {
                navigation.navigateToDestination(
                    this,
                    KeyboardFragmentDirections.actionKeyboardFragmentToListFragment()
                )
                stateViewModel.resetStateStoreForKeyboardFragment()
            }
        }

        keyboardViewModel.backgroundTimerChanged.observe(viewLifecycleOwner) { item ->
            item?.let {

                viewOne = binding.textViewNumberOne
                viewTwo = binding.textViewNumberTwo
                viewThree = binding.textViewNumberThree
                viewFour = binding.textViewNumberFour
                changeSelection(it)
            }
        }

        keyboardViewModel.s1.observe(viewLifecycleOwner) {
            it?.let {

                stateViewModel.sets1(it)
                s1 = it
                stateViewModel.getNumbersTimer()?.let { s ->
                    stateViewModel.setNumbersTimer("$it${s.substring(1..5)}")
                    binding.textViewNumberOne.contentDescription =
                        resources.getString(R.string.first_hour) +
                                it +
                                resources.getString(R.string.time_displayed) +
                                s.substring(1..2) +
                                resources.getString(R.string.hours_keyboard) +
                                s.substring(3..4) +
                                resources.getString(R.string.minutes)
                }
            }
        }

        keyboardViewModel.s2.observe(viewLifecycleOwner) {
            it?.let {

                stateViewModel.sets2(it)
                s2 = it
                stateViewModel.getNumbersTimer()?.let { s ->
                    stateViewModel.setNumbersTimer("${s[0]}$it${s.substring(2..5)}}")
                    binding.textViewNumberTwo.contentDescription =
                        resources.getString(R.string.second_hour) +
                                it +
                                resources.getString(R.string.time_displayed) +
                                s.substring(1..2) +
                                resources.getString(R.string.hours_keyboard) +
                                s.substring(3..4) +
                                resources.getString(R.string.minutes) +
                                resources.getString(R.string.activated) +
                                resources.getString(R.string.first_hour)
                }
            }
        }

        keyboardViewModel.s3.observe(viewLifecycleOwner) {
            it?.let {

                stateViewModel.sets3(it)
                s3 = it
                stateViewModel.getNumbersTimer()?.let { s ->
                    stateViewModel.setNumbersTimer("${s.substring(0..1)}$it${s.substring(3..5)}")
                    binding.textViewNumberThree.contentDescription =
                        resources.getString(R.string.first_minute) +
                                it +
                                resources.getString(R.string.time_displayed) +
                                s.substring(1..2) +
                                resources.getString(R.string.hours_keyboard) +
                                s.substring(3..4) +
                                resources.getString(R.string.minutes)
                }
            }
        }

        keyboardViewModel.s4.observe(viewLifecycleOwner) {
            it?.let {

                stateViewModel.sets4(it)
                s4 = it
                stateViewModel.getNumbersTimer()?.let { s ->
                    stateViewModel.setNumbersTimer("${s.substring(0..2)}$it${s.substring(4..5)}")
                    binding.textViewNumberFour.contentDescription =
                        resources.getString(R.string.second_minute) +
                                it +
                                resources.getString(R.string.time_displayed) +
                                s.substring(1..2) +
                                resources.getString(R.string.hours_keyboard) +
                                s.substring(3..4) +
                                resources.getString(R.string.minutes) +
                                resources.getString(R.string.activated) +
                                resources.getString(R.string.first_minute)
                }
            }
        }

        keyboardViewModel.monday.observe(viewLifecycleOwner) { onMonday ->
            onMonday?.let {

                setTextColorDayPanel(onMonday, binding.textViewMonday)
                stateViewModel.setMonday(onMonday)
            }
        }

        keyboardViewModel.tuesday.observe(viewLifecycleOwner) { onTuesday ->
            onTuesday?.let {

                setTextColorDayPanel(onTuesday, binding.textViewTuesday)
                stateViewModel.setTuesday(onTuesday)
            }
        }

        keyboardViewModel.wednesday.observe(viewLifecycleOwner) { onWednesday ->
            onWednesday?.let {

                setTextColorDayPanel(onWednesday, binding.textViewWednesday)
                stateViewModel.setWednesday(onWednesday)
            }
        }

        keyboardViewModel.thursday.observe(viewLifecycleOwner) { onThursday ->
            onThursday?.let {

                setTextColorDayPanel(onThursday, binding.textViewThursday)
                stateViewModel.setThursday(onThursday)
            }
        }

        keyboardViewModel.friday.observe(viewLifecycleOwner) { onFriday ->
            onFriday?.let {

                setTextColorDayPanel(onFriday, binding.textViewFriday)
                stateViewModel.setFriday(onFriday)
            }
        }

        keyboardViewModel.saturday.observe(viewLifecycleOwner) { onSaturday ->
            onSaturday?.let {

                setTextColorDayPanel(onSaturday, binding.textViewSaturday)
                stateViewModel.setSaturday(onSaturday)
            }
        }

        keyboardViewModel.sunday.observe(viewLifecycleOwner) { onSunday ->
            onSunday?.let {

                setTextColorDayPanel(onSunday, binding.textViewSunday)
                stateViewModel.setSunday(onSunday)
            }
        }

        keyboardViewModel.ampm.observe(viewLifecycleOwner) {
            it?.let {

                ampm = it
                binding.ampm.text = it
                stateViewModel.getNumbersTimer()?.let { s ->
                    stateViewModel.setNumbersTimer("${s[0]}${s[1]}${s[2]}${s[3]}$it")
                }
            }
        }

        keyboardViewModel.hhmm.observe(viewLifecycleOwner) {
            it?.let {

                val dateFormatter = DateFormatter(it, ampm)
                stateViewModel.setNumbersTimer(it)
                if (!isPaused) {
                    if (is24HourFormat != true) {

                        if (previousIs24HourFormat != null && previousIs24HourFormat != is24HourFormat) {
                            setUpDateTimeView(
                                ampm,
                                dateFormatter.format12from24(),
                                it.substring(0, 4),
                                dateFormatter.format12from24()
                            )
                        } else {
                            setUpDateTimeView(
                                ampm,
                                it.substring(0, 4),
                                dateFormatter.format24from12(),
                                it.substring(0, 4)
                            )
                        }
                    } else {
                        if (previousIs24HourFormat != null && previousIs24HourFormat != is24HourFormat) {
                            setUpDateTimeView(
                                ampm,
                                it.substring(0, 4),
                                dateFormatter.format24from12(),
                                dateFormatter.format24from12()
                            )
                        } else {
                            setUpDateTimeView(
                                ampm,
                                dateFormatter.format12from24(),
                                it.substring(0, 4),
                                it.substring(0, 4)
                            )
                        }
                    }
                } else {
                    if (is24HourFormat != true) {
                        setUpDateTimeView(
                            dateFormatter.getAmPm24(),
                            dateFormatter.format12from24(),
                            it.substring(0, 4),
                            dateFormatter.format12from24()
                        )
                        isPaused = false
                    } else {
                        setUpDateTimeView(
                            ampm,
                            dateFormatter.format24from12(),
                            it.substring(0, 4),
                            dateFormatter.format24from12()
                        )
                        isPaused = false
                    }
                }
            }
        }

        keyboardViewModel.hhmmSave.observe(viewLifecycleOwner) {
            it?.let {

                val dateFormatter = DateFormatter(it, ampm)
                if (is24HourFormat == true) {
                    keyboardViewModel.setAMPM(dateFormatter.getAmPm24())
                    keyboardViewModel.setHhmm12(dateFormatter.format12from24())
                    keyboardViewModel.setHhmm24(it)
                } else {
                    keyboardViewModel.setAMPM(dateFormatter.getAmPm12())
                    keyboardViewModel.setHhmm12(it)
                    keyboardViewModel.setHhmm24(dateFormatter.format24from12())
                }
                keyboardViewModel.prepareSaveTimer()
            }
        }

        stateViewModel.numbersTimer.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setNumbersTimer(it)
            }
        }

        stateViewModel.monday.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setOnMonday(it)
            }
        }

        stateViewModel.tuesday.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setOnTuesday(it)
            }
        }

        stateViewModel.wednesday.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setOnWednesday(it)
            }
        }

        stateViewModel.thursday.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setOnThursday(it)
            }
        }

        stateViewModel.friday.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setOnFriday(it)
            }
        }

        stateViewModel.saturday.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setOnSaturday(it)
            }
        }

        stateViewModel.sunday.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setOnSunday(it)
            }
        }

        stateViewModel.specialDate.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                keyboardViewModel.setSpecialDateStr(it)
            }
        }

        //endregion

        //region setSound
        keyboardViewModel.monday.observe(viewLifecycleOwner) {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
        }

        keyboardViewModel.tuesday.observe(viewLifecycleOwner) {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
        }

        keyboardViewModel.wednesday.observe(viewLifecycleOwner) {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
        }

        keyboardViewModel.thursday.observe(viewLifecycleOwner) {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
        }

        keyboardViewModel.friday.observe(viewLifecycleOwner) {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
        }

        keyboardViewModel.saturday.observe(viewLifecycleOwner) {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
        }

        keyboardViewModel.sunday.observe(viewLifecycleOwner) {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
        }

        binding.textViewKeyOne.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(1)
        }

        binding.textViewKeyTwo.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(2)
        }

        binding.textViewKeyThree.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(3)
        }

        binding.textViewKeyFour.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(4)
        }

        binding.textViewKeyFive.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(5)
        }

        binding.textViewKeySix.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(6)
        }

        binding.textViewKeySeven.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(7)
        }

        binding.textViewKeyEight.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(8)
        }

        binding.textViewKeyNine.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(9)
        }

        binding.textViewKeyZero.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(0)
        }

        binding.imageViewKeyDelete.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            keyboardViewModel.fromKeyBoardLayout(10)
        }
        //endregion

        binding.textViewKeySave.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundCancel)
            keyboardViewModel.prepareSave()
        }

        binding.textViewKeyCancel.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            stateViewModel.resetStateStoreForKeyboardFragment()
            navigation.navigateToDestination(
                this,
                KeyboardFragmentDirections.actionKeyboardFragmentToListFragment()
            )
        }

        binding.textViewSelectTrack.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                KeyboardFragmentDirections.actionKeyboardFragmentToRingtonePickerFragment(
                    args.itemId,
                    selectedRingtoneTitle,
                    Correspondent.KeyboardFragment
                )
            )
        }

        setFragmentResultListener(FragmentResultKey.selectedDateKey) { _, bundle ->

            when (val result = bundle.get(FragmentResultKey.selectedDateExtraKey)) {
                FragmentResultKey.clear -> {
                    stateViewModel.setSpecialDate("")
                    keyboardViewModel.clearSpecialDate()
                    keyboardViewModel.setSpecialDateStr("")
                }
                else -> {
                    if (result is Bundle) {
                        val calendar = Calendar.getInstance()
                        val dateBundle = result.getBundle(FragmentResultKey.dateBundle)
                        val year = dateBundle?.getInt(FragmentResultKey.year) ?: 0
                        val month = dateBundle?.getInt(FragmentResultKey.month) ?: 0
                        val day = dateBundle?.getInt(FragmentResultKey.day) ?: 0
                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, day)
                        if (is24HourFormat == true) {
                            calendar.set(Calendar.HOUR_OF_DAY, "$s1$s2".toInt())
                            calendar.set(Calendar.MINUTE, "$s3$s4".toInt())
                            calendar.set(Calendar.SECOND, 0)
                            calendar.clear(Calendar.MILLISECOND)
                        } else {
                            calendar.set(Calendar.HOUR, "$s1$s2".toInt())
                            calendar.set(Calendar.MINUTE, "$s3$s4".toInt())
                            calendar.set(
                                Calendar.AM_PM,
                                if (ampm == "PM") Calendar.PM else Calendar.AM
                            )
                            calendar.set(Calendar.SECOND, 0)
                            calendar.clear(Calendar.MILLISECOND)
                        }
                        calendar.clear(Calendar.MILLISECOND)
                        if (calendar.timeInMillis <= Calendar.getInstance().timeInMillis) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.time_is_over),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val date = SimpleDateFormat(
                                "dd.MM.yyyy",
                                Locale.US
                            ).format(calendar.time)
                            stateViewModel.setSpecialDate(date)
                            keyboardViewModel.setSpecialDateStr(date)
                            keyboardViewModel.setSpecialDate(calendar.timeInMillis)
                        }
                    }
                }
            }
        }

        binding.calendarMonth.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            val dateDialogPicker = DialogDatePicker()
            parentFragmentManager.let { fragmentM ->
                dateDialogPicker.show(fragmentM, "datePicker")
            }
        }

        binding.ampmKey.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            when (ampm) {
                "AM" -> {
                    resources.getString(R.string.pm).let {
                        binding.ampm.text = it
                        ampm = it
                        keyboardViewModel.setAMPM(it)
                    }
                }
                "PM" -> {
                    resources.getString(R.string.am).let {
                        binding.ampm.text = it
                        ampm = it
                        keyboardViewModel.setAMPM(it)
                    }
                }
            }
        }

        binding.arrowBack.setOnClickListener {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                KeyboardFragmentDirections.actionKeyboardFragmentToListFragment()
            )
        }

        return binding.root
    }

    private fun setUpDateTimeView(
        ampm: String?,
        hhmm12: String?,
        hhmm24: String?,
        timeView: String?
    ) {

        keyboardViewModel.setAMPM(ampm)
        keyboardViewModel.setHhmm12(hhmm12)
        keyboardViewModel.setHhmm24(hhmm24)
        keyboardViewModel.upDateTimeView(timeView!!)
    }

    override fun onStart() {

        super.onStart()
        val currentHourFormat = DateFormat.is24HourFormat(requireContext())
        if (currentHourFormat != is24HourFormat) {
            keyboardViewModel.setIs24HourFormat(currentHourFormat)
        }
        if (!isStarted) {
            isStarted = true
        }
    }

    override fun onStop() {

        isPaused = true
        super.onStop()
    }

    override fun onResume() {

        super.onResume()
        soundPoolContainer.setTouchSound()
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putBoolean("isRotation", true)
        outState.putString("specialDate", specialDateStr)
        super.onSaveInstanceState(outState)
    }

    private fun setTextColorDayPanel(itemOn: Boolean, view: TextView) {

        if (itemOn) {
            view.setBackgroundResource(R.drawable.rectangle)
            view.setTypeface(null, Typeface.BOLD)
            view.tag = ViewTags.dayOn
        } else {
            view.setTypeface(null, Typeface.NORMAL)
            view.setBackgroundColor(colorAccent ?: Color.WHITE)
            view.tag = ViewTags.dayOff
        }
    }

    private fun changeSelection(i: Int) {

        when (i) {
            1 -> {
                if ((viewThree!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewThree!!.paintFlags =
                        viewThree!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewOne!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) != Paint.UNDERLINE_TEXT_FLAG) {
                    viewOne!!.paintFlags =
                        viewOne!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewTwo!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewTwo!!.paintFlags =
                        viewTwo!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
            }
            2 -> {
                if ((viewThree!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewThree!!.paintFlags =
                        viewThree!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewOne!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewOne!!.paintFlags =
                        viewOne!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewTwo!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) != Paint.UNDERLINE_TEXT_FLAG) {
                    viewTwo!!.paintFlags =
                        viewTwo!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
            }
            3 -> {
                if ((viewThree!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) != Paint.UNDERLINE_TEXT_FLAG) {
                    viewThree!!.paintFlags =
                        viewThree!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewOne!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewOne!!.paintFlags =
                        viewOne!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewTwo!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewTwo!!.paintFlags =
                        viewTwo!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
            }
            4 -> {
                if ((viewThree!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewThree!!.paintFlags =
                        viewThree!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewOne!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewOne!!.paintFlags =
                        viewOne!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
                if ((viewTwo!!.paintFlags and Paint.UNDERLINE_TEXT_FLAG) == Paint.UNDERLINE_TEXT_FLAG) {
                    viewTwo!!.paintFlags =
                        viewTwo!!.paintFlags xor Paint.UNDERLINE_TEXT_FLAG
                }
            }
        }
    }
}