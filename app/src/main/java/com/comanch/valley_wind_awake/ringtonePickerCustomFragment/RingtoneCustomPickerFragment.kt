package com.comanch.valley_wind_awake.ringtonePickerCustomFragment

import android.content.ServiceConnection
import android.content.ComponentName
import android.content.Intent
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.comanch.valley_wind_awake.NavigationBetweenFragments
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.SoundPoolForFragments
import com.comanch.valley_wind_awake.alarmManagement.RingtoneService
import com.comanch.valley_wind_awake.dataBase.RingtoneData
import com.comanch.valley_wind_awake.databinding.RingtoneCustomPickerFragmentBinding
import com.comanch.valley_wind_awake.keyboardFragment.Correspondent
import com.comanch.valley_wind_awake.frontListFragment.ListOfCustomRingtones
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RingtoneCustomPickerFragment : Fragment() {

    val ringtoneCustomPickerViewModel: RingtoneCustomPickerViewModel by viewModels()

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private val args: RingtoneCustomPickerFragmentArgs by navArgs()
    private var previousRingTone: RingtoneData? = null
    var mService: RingtoneService? = null
    private var listOfRingtones: List<RingtoneData>? = null
    private var mBound: Boolean = false
    private var isPlaying: Boolean = false
    private var isSaveState: Boolean = false

    private var ringtoneSeekPosition: Int = 0
    private var currentRingTonePositionInList: Int = -1
    private val itemActive: Int = 1
    private val itemNotActive: Int = 0
    private val zeroPosition: Int = 0
    private val impossiblePositionInList: Int = -1

    private val emptyString: String = ""
    private var ringtoneUri: String = ""

    private val language: String? by lazy { setLanguage() }

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

        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@RingtoneCustomPickerFragment,
                RingtoneCustomPickerFragmentDirections.actionRingtoneCustomPickerFragmentToRingtonePickerFragment(
                    args.itemId,
                    args.ringtoneTitle,
                    Correspondent.RingtoneCustomFragment
                )
            )
        }
        callback.isEnabled = true

        initSavedInstanceState(savedInstanceState)
        initListOfRingtones()

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: RingtoneCustomPickerFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.ringtone_custom_picker_fragment, container, false
        )

        binding.ringtoneCustomPickerViewModel = ringtoneCustomPickerViewModel

        val adapter =
            RingtoneCustomAdapter(ItemListener { ringtone ->
                ringtoneCustomPickerViewModel.onItemClicked(ringtone)
            }, language)

        binding.RingtoneList.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner

        listOfRingtones?.let {
            adapter.setData(listOfRingtones)
        }

        ringtoneCustomPickerViewModel.setRestorePlayerFlag(isSaveState)
        ringtoneCustomPickerViewModel.restorePlayerFlag.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let {
                if (isPlaying && isSaveState) {
                    mService?.startPlayAfterRotation(ringtoneUri, ringtoneSeekPosition)
                    previousRingTone = setActiveForItem(adapter, itemActive)
                }
            }
        }

        Intent(context, RingtoneService::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        ringtoneCustomPickerViewModel.itemActiveState.observe(viewLifecycleOwner) {

            it.getContentIfNotHandled()?.let {
                if (!isSaveState) {
                    setActiveForItem(adapter, itemNotActive)
                }
            }
        }

        ringtoneCustomPickerViewModel.currentRingTone.observe(viewLifecycleOwner) {

            it?.let {
                mService?.stopPlay()
                if (it.active == itemActive) {
                    it.active = itemNotActive
                    currentRingTonePositionInList = impossiblePositionInList
                } else {
                    mService?.setUri(it.musicId)
                    mService?.startPlay()
                    it.active = itemActive
                    currentRingTonePositionInList = it.position
                }

                previousRingTone?.position?.let { pos -> adapter.notifyItemChanged(pos) }
                it.position.let { pos -> adapter.notifyItemChanged(pos) }

                if (previousRingTone != it) {
                    previousRingTone?.active = itemNotActive
                    previousRingTone = it
                }
            }
        }

        binding.Cancel.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                RingtoneCustomPickerFragmentDirections.actionRingtoneCustomPickerFragmentToRingtonePickerFragment(
                    args.itemId,
                    args.ringtoneTitle,
                    Correspondent.RingtoneCustomFragment
                )
            )
        }

        binding.Ok.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundCancel)

            ringtoneCustomPickerViewModel.setMelody()

            navigation.navigateToDestination(
                this,
                RingtoneCustomPickerFragmentDirections.actionRingtoneCustomPickerFragmentToRingtonePickerFragment(
                    args.itemId,
                    args.ringtoneTitle,
                    Correspondent.RingtoneCustomFragment
                )
            )
        }

        binding.arrowBack.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this,
                RingtoneCustomPickerFragmentDirections.actionRingtoneCustomPickerFragmentToRingtonePickerFragment(
                    args.itemId,
                    args.ringtoneTitle,
                    Correspondent.RingtoneCustomFragment
                )
            )
        }

        return binding.root
    }

    override fun onResume() {

        super.onResume()
        ringtoneCustomPickerViewModel.setItemActiveState()
        soundPoolContainer.setTouchSound()
    }

    override fun onPause() {

        isPlaying = mService?.isPlaying() ?: false
        ringtoneUri = mService?.getStringUri() ?: emptyString
        ringtoneSeekPosition = mService?.getPausePosition() ?: zeroPosition
        isSaveState = false

        ringtoneCustomPickerViewModel.resetCurrentRingTone()
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

    private fun setActiveForItem(adapter: RingtoneCustomAdapter, active: Int): RingtoneData? {

        return adapter.getIt(currentRingTonePositionInList)?.let { ringtoneData ->
            ringtoneData.active = active
            adapter.notifyItemChanged(ringtoneData.position)
            ringtoneData
        }
    }

    private fun startServiceAfterRotation() {
        ringtoneCustomPickerViewModel.restoreStateForCustomRingtoneFragment()
    }

    private fun setLanguage(): String? {

        val localeList = Resources.getSystem().configuration.locales
        return if (localeList.size() > 0) {
            localeList[0].toString()
        } else {
            null
        }
    }

    private fun initListOfRingtones() {
        context?.let {
            listOfRingtones = ListOfCustomRingtones(it).getListOfRingtones()
        }
    }

    private fun initSavedInstanceState(savedInstanceState: Bundle?) {
        ringtoneUri = savedInstanceState?.getString("ringtoneUri") ?: emptyString
        ringtoneSeekPosition = savedInstanceState?.getInt("ringtoneSeekPosition") ?: zeroPosition
        currentRingTonePositionInList =
            savedInstanceState?.getInt("currentRingTonePositionInList") ?: impossiblePositionInList
        isPlaying = savedInstanceState?.getBoolean("isPlaying") ?: false
        isSaveState = savedInstanceState?.getBoolean("isSaveState") ?: false
    }
}