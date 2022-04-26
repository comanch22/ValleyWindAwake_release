package com.comanch.valley_wind_awake.frontListFragment

import android.content.res.Resources
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.comanch.valley_wind_awake.*
import com.comanch.valley_wind_awake.stringKeys.FragmentResultKey
import com.comanch.valley_wind_awake.stringKeys.OperationKey
import com.comanch.valley_wind_awake.stringKeys.PreferenceKeys
import com.comanch.valley_wind_awake.alarmManagement.AlarmControl
import com.comanch.valley_wind_awake.alarmManagement.AlarmTypeOperation
import com.comanch.valley_wind_awake.databinding.FrontListFragmentBinding
import com.comanch.valley_wind_awake.dialogFragments.DialogDeleteAllAlarms
import com.comanch.valley_wind_awake.keyboardFragment.Correspondent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ListFragment : Fragment() {

    val listViewModel: ListViewModel by viewModels()

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    @Inject
    lateinit var alarmControl: AlarmControl

    private val language: String? by lazy { setLanguage() }
    private var adapter: ListItemAdapter? = null
    private var deleteModeOn = false
    private var isCreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            activity?.finish()
        }
        callback.isEnabled = true

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: FrontListFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.front_list_fragment, container, false
        )

        binding.list.setHasFixedSize(true)
        listViewModel.defaultRingtoneUri = getDefaultRingtoneUri()
        listViewModel.defaultRingtoneTitle = getDefaultRingtoneTitle()
        binding.listViewModel = listViewModel

        setFragmentResultListener(FragmentResultKey.deleteAllItemsKey) { _, bundle ->
            when (bundle.get(FragmentResultKey.deleteAllItemsExtraKey)) {
                FragmentResultKey.ok -> {
                    listViewModel.clear()
                }
            }
        }

        binding.toolbar.inflateMenu(R.menu.app_menu)
        binding.toolbar.setOnMenuItemClickListener {
            menuNavigation(it)
        }

        adapter = ListItemAdapter(
            ItemListener { itemId ->
                listViewModel.onItemClicked(itemId)
            },
            SwitchListener { item ->
                listViewModel.updateSwitchCheck(item)
            },
            DeleteListener { itemId ->
                listViewModel.offAlarmDeleteItem(itemId)
            },
            resolveColor(R.attr.colorSecondary).data,
            resolveColor(R.attr.colorAccent).data,
            resolveColor(R.attr.colorPrimaryVariant).data,
            is24HourFormat(),
            Calendar.getInstance().timeInMillis,
            language,
            R.drawable.ic_baseline_alarm_on_64,
            R.drawable.ic_baseline_alarm_on_48
        )

        binding.list.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner

        deleteModeOn = savedInstanceState?.getBoolean("deleteModeOn", false) == true
        setButtonsVisible(binding)
        adapter?.setDeleteMode(deleteModeOn)
        listViewModel.setDeleteItemsMode(deleteModeOn)

        listViewModel.items.observe(viewLifecycleOwner) { list ->
            list?.let {
                adapter?.setData(it)
            }
            lifecycleScope.launch {
                delay(200)
                binding.list.smoothScrollToPosition(0)
            }
            listViewModel.setNearestDate(is24HourFormat())
        }

        listViewModel.nearestDate.observe(viewLifecycleOwner) { str ->
            str?.let {
                if (it.isEmpty()) {
                    binding.toolbarTitle.visibility = View.INVISIBLE
                    binding.toolbarTitle.text = ""
                } else {
                    binding.toolbarTitle.text = resources.getString(R.string.the_nearest_signal)
                    binding.toolbarTitle.visibility = View.VISIBLE
                }
                binding.toolbarNearestDate.text = it
                setToolbarContentDescription(binding)
            }
        }

        listViewModel.navigateToKeyboardFragment.observe(viewLifecycleOwner) { itemId ->
            itemId?.getContentIfNotHandled()?.let {

                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundStart)
                navigation.navigateToDestination(
                    this,
                    ListFragmentDirections.actionListFragmentToKeyboardFragment(
                        it,
                        Correspondent.ListFragment,
                        "",
                        ""
                    )
                )
            }
        }

        listViewModel.deleteAllItems.observe(viewLifecycleOwner) { listTimeData ->
            listTimeData?.let {
                lifecycleScope.launch {
                    context?.applicationContext.let { appContext ->
                        if (appContext != null) {
                            it.forEach {
                                alarmControl.timeData = it
                                alarmControl.schedulerAlarm(AlarmTypeOperation.DELETE).isNotEmpty()
                            }
                            listViewModel.deleteAll()
                            listViewModel.resetDeleteAllItems()
                        }
                    }
                }
            }
        }

        listViewModel.offAlarm.observe(viewLifecycleOwner) {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundUiTap)
            it?.let {
                lifecycleScope.launch {
                    context?.applicationContext.let { appContext ->
                        if (appContext != null) {
                            alarmControl.timeData = it
                            alarmControl.schedulerAlarm(AlarmTypeOperation.DELETE).isNotEmpty()
                            listViewModel.deleteItem(it)
                        }
                    }
                }
            }
        }

        listViewModel.itemActive.observe(viewLifecycleOwner) { timeData ->
            timeData?.let {
                Log.e("grtrtrtyrty", "code = ${timeData.requestCode}")
                if (timeData.active) {
                    soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundStateDown)
                } else {
                    soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundStateUp)
                }

                lifecycleScope.launch {
                    context?.applicationContext.let { appContext ->
                        if (appContext != null) {
                            alarmControl.timeData = timeData
                            when (alarmControl.schedulerAlarm(AlarmTypeOperation.SWITCH)
                            ) {
                                OperationKey.successOff -> {
                                    listViewModel.resetItemActive()
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.alarm_is_off),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                OperationKey.successOn -> {
                                    listViewModel.resetItemActive()
                                    listViewModel.showDiffTimeToast(it.timeId)
                                }
                                OperationKey.incorrectDate -> {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.time_is_over),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        listViewModel.timeToast.observe(viewLifecycleOwner) { timeData ->

            timeData.getContentIfNotHandled()?.let {
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
        }

        binding.ButtonDelete.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            setVisibleFromButtonDelete(binding)
            adapter?.setDeleteMode(deleteModeOn)
            listViewModel.setDeleteItemsMode(deleteModeOn)
        }

        binding.ButtonDone.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)

            setVisibleFromButtonDone(binding)
            adapter?.setDeleteMode(deleteModeOn)
            listViewModel.setDeleteItemsMode(deleteModeOn)
        }

        binding.ButtonPlus.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            listViewModel.insertItem()
        }

        binding.arrowBack.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            activity?.finish()
        }
        return binding.root
    }

    private fun setVisibleFromButtonDone(binding: FrontListFragmentBinding) {
        binding.ButtonDone.visibility = View.INVISIBLE
        binding.toolbar.visibility = View.VISIBLE
        binding.ButtonDelete.visibility = View.VISIBLE
        binding.ButtonPlus.visibility = View.VISIBLE
        deleteModeOn = false
    }

    private fun setVisibleFromButtonDelete(binding: FrontListFragmentBinding) {
        if (deleteModeOn) {
            binding.ButtonDone.visibility = View.INVISIBLE
            binding.toolbar.visibility = View.VISIBLE
            binding.ButtonDelete.visibility = View.VISIBLE
            binding.ButtonPlus.visibility = View.VISIBLE
            deleteModeOn = false
        } else {
            binding.ButtonDone.visibility = View.VISIBLE
            binding.toolbar.visibility = View.INVISIBLE
            binding.ButtonDelete.visibility = View.INVISIBLE
            binding.ButtonPlus.visibility = View.INVISIBLE
            deleteModeOn = true
        }
    }

    private fun setToolbarContentDescription(binding: FrontListFragmentBinding) {
        if (language == "ru_RU") {
            binding.toolbar.contentDescription = "Заголовок. Слева кнопка назад. "
        } else {
            binding.toolbar.contentDescription = "Heading. Back button on the left. "
        }
    }

    private fun setButtonsVisible(binding: FrontListFragmentBinding) {
        if (!deleteModeOn) {
            binding.ButtonDone.visibility = View.INVISIBLE
            binding.toolbar.visibility = View.VISIBLE
            binding.ButtonDelete.visibility = View.VISIBLE
            binding.ButtonPlus.visibility = View.VISIBLE
        } else {
            binding.ButtonDone.visibility = View.VISIBLE
            binding.toolbar.visibility = View.INVISIBLE
            binding.ButtonDelete.visibility = View.INVISIBLE
            binding.ButtonPlus.visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        if (isCreated) {
            adapter?.setIs24HourFormat(is24HourFormat())
            adapter?.refreshList()
            listViewModel.setNearestDate(is24HourFormat())
        } else {
            isCreated = true
        }
    }

    override fun onResume() {
        super.onResume()
        soundPoolContainer.setTouchSound()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("deleteModeOn", deleteModeOn)
        super.onSaveInstanceState(outState)
    }

    private fun menuNavigation(it: MenuItem) = when (it.itemId) {
        R.id.action_done -> {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            val dialogPicker = DialogDeleteAllAlarms()
            parentFragmentManager.let { fragmentM ->
                dialogPicker.show(fragmentM, "dialogPicker")
            }
            true
        }
        R.id.action_settings -> {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToSettingsFragment()
            )
            true
        }
        R.id.about_app -> {
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                ListFragmentDirections.actionListFragmentToAboutAppFragment()
            )
            true
        }
        else -> false
    }

    private fun is24HourFormat() = DateFormat.is24HourFormat(requireContext())

    private fun setLanguage(): String? {

        val localeList = Resources.getSystem().configuration.locales
        return if (localeList.size() > 0) {
            localeList[0].toString()
        } else {
            null
        }
    }

    private fun resolveColor(attr: Int): TypedValue {

        val color = TypedValue()
        when (attr) {
            R.attr.colorAccent -> {
                requireContext().theme.resolveAttribute(attr, color, true)
            }
            R.attr.colorSecondary -> {
                requireContext().theme.resolveAttribute(attr, color, true)
            }
            R.attr.colorPrimaryVariant -> {
                requireContext().theme.resolveAttribute(attr, color, true)
            }
        }

        return color
    }

    private fun getDefaultRingtoneUri(): String {

        return preferences.getString(PreferenceKeys.defaultRingtoneUri)
    }

    private fun getDefaultRingtoneTitle(): String {

        return preferences.getString(PreferenceKeys.defaultRingtoneTitle)
    }
}



