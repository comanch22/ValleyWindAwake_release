package com.comanch.valley_wind_awake.ringtonePickerFragment

import androidx.navigation.fragment.navArgs
import android.Manifest
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.comanch.valley_wind_awake.DefaultPreference
import com.comanch.valley_wind_awake.NavigationBetweenFragments
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.SoundPoolForFragments
import com.comanch.valley_wind_awake.alarmManagement.RingtoneService
import com.comanch.valley_wind_awake.dataBase.RingtoneData
import com.comanch.valley_wind_awake.databinding.RingtonePickerFragmentBinding
import com.comanch.valley_wind_awake.keyboardFragment.Correspondent
import com.comanch.valley_wind_awake.stringKeys.PreferenceKeys
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RingtonePickerFragment : Fragment() {

    val ringtonePickerViewModel: RingtonePickerViewModel by viewModels()

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    val adapter: RingtoneAdapter by lazy { setAdapter() }
    private val language: String? by lazy { setLanguage() }
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val args: RingtonePickerFragmentArgs by navArgs()
    private var defaultListOfRingtones: MutableList<RingtoneData> = mutableListOf()
    private var previousRingTone: RingtoneData? = null
    var mService: RingtoneService? = null
    private var audioManager: AudioManager? = null
    private var currentVolume: Int? = null
    private var maxVolume: Int? = null
    private var minVolume: Int = 1

    private var isPlaying: Boolean = false
    private var isSaveState: Boolean = false
    private var mBound: Boolean = false
    private var isCustomChooseVolume: Boolean = false

    private var ringtoneSeekPosition: Int = 0
    private val itemNotActive: Int = 0
    private val zeroPosition: Int = 0
    private var currentRingTonePositionInList: Int = -1
    private val impossiblePositionInList: Int = -1
    private val impossibleMelodyId: Long = -2
    private val itemActive: Int = 1

    private val emptyString: String = ""
    private var ringtoneUri: String = ""

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            val binder = service as RingtoneService.LocalBinder
            mService = binder.getService()
            mBound = true
            startServiceAfterRotation()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    navigation.navigateToDestination(
                        this,
                        RingtonePickerFragmentDirections.actionRingtonePickerFragmentToRingtoneCustomPickerFragment(
                            args.itemId,
                            args.ringtoneTitle
                        )
                    )
                } else {
                    Toast.makeText(context, R.string.noAccessRights, Toast.LENGTH_LONG).show()
                }
            }

        super.onCreate(savedInstanceState)

        val action =
            if (args.correspondent == Correspondent.SettingsFragment) {
                RingtonePickerFragmentDirections.actionRingtonePickerFragmentToSettingsFragment()
            } else {
                RingtonePickerFragmentDirections.actionRingtonePickerFragmentToKeyboardFragment(
                    args.itemId,
                    Correspondent.RingtoneFragment,
                    emptyString,
                    args.ringtoneTitle
                )
            }
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@RingtonePickerFragment,
                action
            )
        }
        callback.isEnabled = true
        initValuesFromSavedState(savedInstanceState)
        setAudioManager()

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: RingtonePickerFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.ringtone_picker_fragment, container, false
        )

        binding.ringtonePickerViewModel = ringtonePickerViewModel
        binding.RingtoneList.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner

        if (args.correspondent == Correspondent.SettingsFragment) {
            setButtonsVisible(binding)
        }

        ringtonePickerViewModel.setRestorePlayerFlag(isSaveState)
        ringtonePickerViewModel.restorePlayerFlag.observe(viewLifecycleOwner) {

            it.getContentIfNotHandled()?.let {
                if (isSaveState && isPlaying) {
                    mService?.startPlayAfterRotation(ringtoneUri, ringtoneSeekPosition)
                    previousRingTone = setActiveForItem(adapter, itemActive)
                    previousRingTone?.let { it1 -> ringtonePickerViewModel.onItemClicked(it1) }
                }
            }
        }

        ringtonePickerViewModel.setTouchSoundAndVolume()
        ringtonePickerViewModel.setTouchSoundAndVolume.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {

                if (currentVolume != null && maxVolume != null) {
                    binding.seekbar.progress =
                        currentVolume ?: (maxVolume!!.plus(minVolume)).div(2)
                    binding.seekbar.max = maxVolume!!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        binding.seekbar.min = minVolume
                    }
                    binding.seekbarValue.text = currentVolume.toString()
                }
            }
        }

        ringtonePickerViewModel.itemActiveState.observe(viewLifecycleOwner) {

            it.getContentIfNotHandled()?.let {
                if (!isSaveState) {
                    setActiveForItem(adapter, itemNotActive)
                }
            }
        }

        Intent(context, RingtoneService::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        ringtonePickerViewModel.currentRingTone.observe(viewLifecycleOwner) {
            it?.let {
                if (!isSaveState) {
                    if (isCustomChooseVolume && mService?.isPlaying() == true) {
                        currentRingTonePositionInList = it.position
                    } else {
                        mService?.stopPlay()
                        if (it.active == itemActive) {
                            it.active = itemNotActive
                            currentRingTonePositionInList = impossiblePositionInList
                            ringtonePickerViewModel.resetCurrentRingtoneValue()
                        } else {
                            mService?.setUri(it.uriAsString)
                            mService?.startPlay()
                            it.active = itemActive
                            currentRingTonePositionInList = it.position
                        }
                    }
                    previousRingTone?.position?.let { pos -> adapter.notifyItemChanged(pos) }
                    it.position.let { pos -> adapter.notifyItemChanged(pos) }

                    if (previousRingTone != it) {
                        previousRingTone?.active = itemNotActive
                        previousRingTone = it
                    }
                    isCustomChooseVolume = false
                } else {
                    isSaveState = false
                }
            }
        }

        ringtonePickerViewModel.items.observe(viewLifecycleOwner) {
            it?.let {
                adapter.setData(it)
            }
        }

        ringtonePickerViewModel.delete.observe(viewLifecycleOwner) {
            it?.let {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                mService?.stopPlay()
                ringtonePickerViewModel.resetDelete()
            }
        }

        ringtonePickerViewModel.chooseRingtone.observe(viewLifecycleOwner) {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            it.getContentIfNotHandled()?.let {
                when {
                    context?.let { it_ ->
                        ContextCompat.checkSelfPermission(
                            it_,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    } == PackageManager.PERMISSION_GRANTED -> {
                        navigation.navigateToDestination(
                            this,
                            RingtonePickerFragmentDirections.actionRingtonePickerFragmentToRingtoneCustomPickerFragment(
                                args.itemId,
                                args.ringtoneTitle
                            )
                        )

                    }
                    shouldShowRequestPermissionRationale("READ_EXTERNAL_STORAGE") -> {
                    }
                    else -> {
                        requestPermissionLauncher.launch(
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    }
                }
            }
        }

        ringtonePickerViewModel.toast.observe(viewLifecycleOwner) {
            it?.let {
                val text = when (it) {
                    "choose a ringtone" -> resources.getString(R.string.choose_a_ringtone)
                    "cannot be deleted" -> resources.getString(R.string.cannot_deleted)
                    else -> ""
                }
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
                ringtonePickerViewModel.resetToast()
            }
        }

        ringtonePickerViewModel.setRingtoneTitle.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let { ringtoneTitle ->
                if (ringtoneTitle != "" && args.correspondent == Correspondent.SettingsFragment) {
                    preferences.putString(PreferenceKeys.defaultRingtoneTitle, ringtoneTitle)
                }
            }
        }

        ringtonePickerViewModel.setRingtoneUri.observe(viewLifecycleOwner) { content ->

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundCancel)
            content.getContentIfNotHandled()?.let { ringtoneUri ->
                if (ringtoneUri == "") {
                    Toast.makeText(context, R.string.choose_a_ringtone, Toast.LENGTH_LONG)
                        .show()
                } else {
                    when (args.correspondent) {
                        Correspondent.SettingsFragment -> {
                            preferences.putString(PreferenceKeys.defaultRingtoneUri, ringtoneUri)
                            navigation.navigateToDestination(
                                this,
                                RingtonePickerFragmentDirections.actionRingtonePickerFragmentToSettingsFragment()
                            )
                        }
                        else -> {
                            navigation.navigateToDestination(
                                this,
                                RingtonePickerFragmentDirections.actionRingtonePickerFragmentToKeyboardFragment(
                                    args.itemId,
                                    Correspondent.RingtoneFragment,
                                    ringtoneUri,
                                    previousRingTone?.title ?: ""
                                )
                            )
                        }
                    }
                }
            }
        }

        binding.fabDelete.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            mService?.stopPlay()
            ringtonePickerViewModel.deleteMelody()
        }

        binding.Cancel.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            if (args.correspondent == Correspondent.SettingsFragment) {
                navigation.navigateToDestination(
                    this,
                    RingtonePickerFragmentDirections.actionRingtonePickerFragmentToSettingsFragment()
                )
            } else {
                navigation.navigateToDestination(
                    this,
                    RingtonePickerFragmentDirections.actionRingtonePickerFragmentToKeyboardFragment(
                        args.itemId,
                        Correspondent.RingtoneFragment,
                        emptyString,
                        args.ringtoneTitle
                    )
                )
            }
        }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    binding.seekbarValue.text = progress.toString()
                    audioManager?.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        progress,
                        0
                    )
                } else {
                    val mProgress = if (progress == 0) 1 else progress
                    binding.seekbarValue.text = mProgress.toString()
                    audioManager?.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        mProgress,
                        0
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

                val ringtone =
                    adapter.getRingtone(if (currentRingTonePositionInList >= 0) currentRingTonePositionInList else 0)
                ringtone?.let {
                    isCustomChooseVolume = true
                    ringtonePickerViewModel.onItemClicked(ringtone)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                context?.let {

                }
            }

        })

        binding.arrowBack.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            if (args.correspondent == Correspondent.SettingsFragment) {
                navigation.navigateToDestination(
                    this,
                    RingtonePickerFragmentDirections.actionRingtonePickerFragmentToSettingsFragment()
                )
            } else {
                navigation.navigateToDestination(
                    this,
                    RingtonePickerFragmentDirections.actionRingtonePickerFragmentToKeyboardFragment(
                        args.itemId,
                        Correspondent.RingtoneFragment,
                        emptyString,
                        args.ringtoneTitle
                    )
                )
            }
        }

        return binding.root
    }

    private fun setAdapter() = RingtoneAdapter(
        ItemListener { ringtone ->
            ringtonePickerViewModel.onItemClicked(ringtone)
        },
        defaultListOfRingtones,
        setColorAccent(),
        language
    )

    override fun onResume() {

        super.onResume()
        ringtonePickerViewModel.setItemActiveState()
        soundPoolContainer.setTouchSound()
    }

    override fun onPause() {

        isPlaying = mService?.isPlaying() ?: false
        ringtoneUri = mService?.getStringUri() ?: emptyString
        ringtoneSeekPosition = mService?.getPausePosition() ?: zeroPosition
        isSaveState = false

        ringtonePickerViewModel.resetCurrentRingTone()
        mService?.offMediaPlayer()

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putBoolean("isPlaying", isPlaying)
        outState.putBoolean("isPause", false)
        outState.putBoolean("isSaveState", true)
        outState.putInt("currentRingTonePositionInList", currentRingTonePositionInList)
        outState.putString("ringtoneUri", ringtoneUri)
        outState.putInt("ringtoneSeekPosition", ringtoneSeekPosition)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {

        offMediaUnbindService()
        super.onDestroy()
    }

    private fun offMediaUnbindService() {

        if (mBound) {
            mService?.offMediaPlayer()
            context?.unbindService(connection)
            mBound = false
        }
    }

    private fun getDefaultRingtoneList(defaultListOfRingtones: MutableList<RingtoneData>): MutableList<RingtoneData> {

        val ringtoneManager = RingtoneManager(context)
        val cursor = ringtoneManager.cursor
        for (i in 0 until cursor.count) {
            cursor.moveToPosition(i)
            defaultListOfRingtones.add(
                RingtoneData(
                    title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX),
                    uriAsString = ringtoneManager.getRingtoneUri(i).toString(),
                    melodyId = impossibleMelodyId
                )
            )
        }
        return defaultListOfRingtones
    }

    private fun setActiveForItem(adapter: RingtoneAdapter, active: Int): RingtoneData? {

        return adapter.getRingtone(currentRingTonePositionInList)?.let { ringtoneData ->
            ringtoneData.active = active
            adapter.notifyItemChanged(ringtoneData.position)
            ringtoneData
        }
    }

    private fun startServiceAfterRotation() {
        ringtonePickerViewModel.restoreStateForRingtoneFragment()
    }

    private fun setColorAccent(): Int {
        val colorAccent = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorPrimaryVariant, colorAccent, true)
        return colorAccent.data
    }

    private fun setButtonsVisible(binding: RingtonePickerFragmentBinding) {
        binding.fabAdd.isInvisible = true
        binding.fabDelete.isInvisible = true
    }

    private fun setAudioManager() {

        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.let {
            currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM)
            maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                maxVolume = maxVolume?.minus(1)
            }
        }
    }

    private fun initValuesFromSavedState(savedInstanceState: Bundle?) {

        ringtoneUri = savedInstanceState?.getString("ringtoneUri") ?: emptyString
        ringtoneSeekPosition = savedInstanceState?.getInt("ringtoneSeekPosition") ?: zeroPosition
        isPlaying = savedInstanceState?.getBoolean("isPlaying") ?: false
        isSaveState = savedInstanceState?.getBoolean("isSaveState") ?: false
        defaultListOfRingtones = getDefaultRingtoneList(defaultListOfRingtones)
        currentRingTonePositionInList =
            savedInstanceState?.getInt("currentRingTonePositionInList") ?: -1
    }

    private fun setLanguage(): String? {

        val localeList = Resources.getSystem().configuration.locales
        return if (localeList.size() > 0) {
            localeList[0].toString()
        } else {
            null
        }
    }
}