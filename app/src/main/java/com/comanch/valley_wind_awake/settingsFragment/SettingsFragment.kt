package com.comanch.valley_wind_awake.settingsFragment

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.*
import com.comanch.valley_wind_awake.DefaultPreference
import com.comanch.valley_wind_awake.NavigationBetweenFragments
import com.comanch.valley_wind_awake.stringKeys.AppStyleKey
import com.comanch.valley_wind_awake.stringKeys.FragmentResultKey
import com.comanch.valley_wind_awake.stringKeys.PreferenceKeys
import com.comanch.valley_wind_awake.dialogFragments.DialogRestartActivity
import com.comanch.valley_wind_awake.keyboardFragment.Correspondent
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.SoundPoolForFragments
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    private var previousAppStylePref: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previousAppStylePref = preferences.getString(AppStyleKey.appStyle)

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@SettingsFragment,
                SettingsFragmentDirections.actionSettingsFragmentToListFragment()
            )
        }
        callback.isEnabled = true

        setFragmentResultListener(FragmentResultKey.restartActivity) { _, bundle ->
            when (bundle.get(FragmentResultKey.restartActivityExtraKey)) {
                FragmentResultKey.ok -> {
                    activity?.let {
                        val intent = it.intent
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                        it.overridePendingTransition(0, 0)
                        it.finish()

                        it.overridePendingTransition(0, 0)
                        startActivity(intent)
                    }
                }
            }
        }

        val backButton = findPreference<Preference>(PreferenceKeys.backButton)
        backButton?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                navigation.navigateToDestination(
                    this,
                    SettingsFragmentDirections.actionSettingsFragmentToListFragment())
                true
            }
        }

        val ringtoneVolumeSelection = findPreference<Preference>(PreferenceKeys.defaultRingtoneUri)
        ringtoneVolumeSelection?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                navigation.navigateToDestination(
                    this,
                    SettingsFragmentDirections.actionSettingsFragmentToRingtonePickerFragment(
                        -1,
                        "",
                        Correspondent.SettingsFragment
                    )
                )
                true
            }

        }

        val appStyle = findPreference<Preference>(AppStyleKey.appStyle)
        appStyle?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference,
                                                    newValue ->
                preference.summary = newValue.toString()
                val dialogPicker = DialogRestartActivity(previousAppStylePref)
                parentFragmentManager.let { fragmentM ->
                    dialogPicker.show(fragmentM, "dialogPicker")
                }
                true
            }

        appStyle?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                true
            }
        }

        val signalDuration = findPreference<Preference>(PreferenceKeys.signalDuration)
        signalDuration?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference,
                                                    newValue ->
                preference.summary = newValue.toString()
                true
            }

        signalDuration?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                true
            }
        }

        val pauseDuration = findPreference<Preference>(PreferenceKeys.pauseDuration)
        pauseDuration?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference,
                                                    newValue ->
                preference.summary = newValue.toString()
                true
            }

        pauseDuration?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                true
            }
        }

        val isVibrate = findPreference<Preference>(PreferenceKeys.isVibrate)
        isVibrate?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                preferences.putBoolean(PreferenceKeys.isVibrate, newValue as Boolean)
                true
            }

        isVibrate?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val colorBackground = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorPrimaryVariant, colorBackground, true)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.setBackgroundColor(colorBackground.data)
        return view
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        val backButton = Preference(context)
        backButton.icon = ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_baseline_keyboard_arrow_left_36,
            context.theme
        )
        backButton.key = PreferenceKeys.backButton
        backButton.title = resources.getString(R.string.settings_title)
        backButton.summary = resources.getString(R.string.back_button)
        backButton.layoutResource = R.layout.preference_custom_layout

        val ringtoneVolumeSelection = Preference(context)
        ringtoneVolumeSelection.key = PreferenceKeys.defaultRingtoneUri
        ringtoneVolumeSelection.title = resources.getString(R.string.settings_ringtone_choice)
        ringtoneVolumeSelection.summary = preferences.getString(PreferenceKeys.defaultRingtoneTitle)
        ringtoneVolumeSelection.layoutResource = R.layout.preference_custom_layout
        ringtoneVolumeSelection.setDefaultValue("")

        val appStyle = ListPreference(context)
        appStyle.key = AppStyleKey.appStyle
        appStyle.title = resources.getString(R.string.settings_app_style)
        appStyle.summary = preferences.getString(AppStyleKey.appStyle)
        appStyle.dialogTitle = resources.getString(R.string.application_style)
        appStyle.layoutResource = R.layout.preference_custom_layout
        appStyle.setEntries(R.array.app_style)
        appStyle.setEntryValues(R.array.app_style)
        appStyle.setDefaultValue(AppStyleKey.blue)

        val signalDuration = ListPreference(context)
        signalDuration.key = PreferenceKeys.signalDuration
        signalDuration.title = resources.getString(R.string.signal_duration)
        signalDuration.summary = preferences.getString(PreferenceKeys.signalDuration)
        signalDuration.dialogTitle = resources.getString(R.string.choose_signal_duration)
        signalDuration.layoutResource = R.layout.preference_custom_layout
        signalDuration.setEntries(R.array.signal_duration)
        signalDuration.setEntryValues(R.array.signal_duration)
        signalDuration.setDefaultValue("2")

        val pauseDuration = ListPreference(context)
        pauseDuration.key = PreferenceKeys.pauseDuration
        pauseDuration.title = resources.getString(R.string.pause_duration)
        pauseDuration.summary = preferences.getString(PreferenceKeys.pauseDuration)
        pauseDuration.dialogTitle = resources.getString(R.string.choose_pause_duration)
        pauseDuration.layoutResource = R.layout.preference_custom_layout
        pauseDuration.setEntries(R.array.pause_duration)
        pauseDuration.setEntryValues(R.array.pause_duration)
        pauseDuration.setDefaultValue("5")

        val isVibrate = SwitchPreference(context)
        isVibrate.key = PreferenceKeys.isVibrate
        isVibrate.title = resources.getString(R.string.vibration_signal)
        isVibrate.layoutResource = R.layout.switch_custom

        screen.addPreference(backButton)
        screen.addPreference(ringtoneVolumeSelection)
        screen.addPreference(appStyle)
        screen.addPreference(signalDuration)
        screen.addPreference(pauseDuration)
        screen.addPreference(isVibrate)

        preferenceScreen = screen
    }

    override fun onResume() {
        super.onResume()
        soundPoolContainer.setTouchSound()
    }

}

