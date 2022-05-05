package com.comanch.valley_wind_awake.alarmFragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.comanch.valley_wind_awake.*
import com.comanch.valley_wind_awake.stringKeys.IntentKeys
import com.comanch.valley_wind_awake.stringKeys.PreferenceKeys
import com.comanch.valley_wind_awake.alarmManagement.RingtoneService
import com.comanch.valley_wind_awake.broadcastreceiver.AlarmReceiver
import com.comanch.valley_wind_awake.databinding.DetailFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private val args: DetailFragmentArgs by navArgs()
    private val detailViewModel: DetailViewModel by viewModels()

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            val offIntent = createIntentAlarmReceiver(IntentKeys.offAlarm, args.itemId)
            context?.sendBroadcast(offIntent)
            navigation.navigateToDestination(this@DetailFragment,
                DetailFragmentDirections.actionDetailFragmentToListFragment())
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
        val binding: DetailFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.detail_fragment, container, false
        )

        binding.detailViewModel = detailViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val pauseDurationPreference = getPauseDurationPreference()
        val signalDurationPreference = getSignalDurationPreference()

        binding.delaySignal.text = setDelaySignalText(pauseDurationPreference)

        detailViewModel.startDelay(signalDurationPreference.toLong())

        detailViewModel.navigateToList.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                stopPlayRingtone()
                closeApp()
            }
        }

        detailViewModel.setPause.observe(viewLifecycleOwner) { content ->
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            content.getContentIfNotHandled()?.let {
                val pauseIntent = createIntentAlarmReceiver(IntentKeys.pauseAlarm, args.itemId)
                context?.sendBroadcast(pauseIntent)
                stopPlayRingtone()
                closeApp()
            }
        }

        detailViewModel.offAlarm.observe(viewLifecycleOwner) { content ->
            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            content.getContentIfNotHandled()?.let {
                val offIntent = createIntentAlarmReceiver(IntentKeys.offAlarm, args.itemId)
                context?.sendBroadcast(offIntent)
                stopPlayRingtone()
                closeApp()
            }
        }

        detailViewModel.stopPlay.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                closeApp()
            }
        }

        return binding.root
    }

    override fun onResume() {

        super.onResume()
        soundPoolContainer.setTouchSound()
    }

    private fun stopPlayRingtone() {
        context?.startService(createIntentRingtoneService(IntentKeys.stopAction))
    }

    fun createIntentRingtoneService(actionKey: String): Intent {

        return Intent(requireContext(), RingtoneService::class.java).apply {
            action = actionKey
        }
    }

    fun createIntentAlarmReceiver(actionKey: String, timeId: Long): Intent {

        return Intent(context, AlarmReceiver::class.java).apply {
            action = actionKey
            putExtra(IntentKeys.timeId, timeId.toString())
        }
    }

    fun getPauseDurationPreference(): String {
        return preferences.getString(PreferenceKeys.pauseDuration)
    }

    fun getSignalDurationPreference(): String {
        return preferences.getString(PreferenceKeys.signalDuration)
    }

    private fun setDelaySignalText(pauseDuration: String): String {

        return "${resources.getString(R.string.delay_signal)} $pauseDuration ${
            resources.getString(R.string.min)
        }"
    }

    private fun closeApp() {

        activity?.finishAndRemoveTask()
    }
}