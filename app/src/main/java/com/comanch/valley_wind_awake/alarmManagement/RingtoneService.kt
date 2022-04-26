package com.comanch.valley_wind_awake.alarmManagement

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import com.comanch.valley_wind_awake.DefaultPreference
import com.comanch.valley_wind_awake.stringKeys.IntentKeys
import com.comanch.valley_wind_awake.MainActivity
import com.comanch.valley_wind_awake.stringKeys.PreferenceKeys
import com.comanch.valley_wind_awake.R
import com.comanch.valley_wind_awake.broadcastreceiver.AlarmReceiver
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class RingtoneService : Service(),
    OnPreparedListener,
    OnErrorListener,
    OnAudioFocusChangeListener {

    @Inject
    lateinit var preferences: DefaultPreference

    private var mMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var stringUri: String? = null
    private var focusRequest: AudioFocusRequest? = null
    private val binder = LocalBinder()
    private var isStarted = false
    private var pausePosition: Int? = 0
    private var isRotation: Boolean = false
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var previousTimeId: String = "-1"
    private var previousTimeStr: String = "0000"
    private var previousRingtoneUri: String = ""
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent == null) {
            return START_NOT_STICKY
        }

        val context = this.applicationContext
        when (intent.action) {

            IntentKeys.playAction -> {
                if (!isStarted) {
                    isStarted = true
                    previousTimeId =
                        (intent.extras?.getLong(IntentKeys.timeId, -1L) ?: -1L).toString()
                    getIntentString(intent, IntentKeys.timeStr).let {
                        previousTimeStr = it
                    }
                    getIntentString(intent, IntentKeys.ringtoneUri).let {
                        previousRingtoneUri = it
                    }
                    startForeground(17131415, setNotificationAndTimer(context))
                } else {
                    sendOffDuplicateSignal(
                        context,
                        (intent.extras?.getLong(IntentKeys.timeId, -1L) ?: -1L).toString()
                    )
                }
            }
            IntentKeys.stopAction -> {
                mStopForeground()
            }
        }
        return START_NOT_STICKY
    }

    private fun getIntentString(intent: Intent, key: String): String {
        return intent.extras?.getString(key) ?: ""
    }

    private fun setNotificationAndTimer(context: Context): Notification {

        val notification = createFullScreenNotification(
            context,
            previousTimeId,
            previousTimeStr,
            previousRingtoneUri
        )

        if (previousRingtoneUri.isNotEmpty()) {
            setUri(previousRingtoneUri)
            startPlay()
            if (preferences.getBoolean(PreferenceKeys.isVibrate)) {
                startVibrate()
            }
        }
        setTimer(context)

        return notification
    }

    private fun setTimer(context: Context) {
        clearTimer()
        timer = Timer()
        timerTask = MTimerTask(context, previousTimeId)
        timer?.schedule(timerTask, preferences.getString(PreferenceKeys.signalDuration).toLong() * 60000)
    }

    private fun sendOffIntent(context: Context, timeId: String) {

        val offIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = IntentKeys.offAlarm
            putExtra(IntentKeys.timeId, timeId)
        }
        sendBroadcast(offIntent)
    }

    private fun sendOffDuplicateSignal(context: Context, timeId: String) {

        val offIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = IntentKeys.offDuplicateSignal
            putExtra(IntentKeys.timeId, timeId)
        }
        sendBroadcast(offIntent)
    }


    private fun mStopForeground() {

        vibrator?.cancel()
        clearTimer()
        offMediaPlayer()
        stopForeground(true)
        stopSelf()
        isStarted = false
    }

    private fun clearTimer() {

        timer?.cancel()
        timer = null
        timerTask?.cancel()
        timerTask = null
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {

        if (requestAudioFocus()) {

            mediaPlayer.setOnCompletionListener {
                mediaPlayer.start()
            }

            if (isRotation) {
                pausePosition?.let { mediaPlayer.seekTo(it) }
                mediaPlayer.start()
                isRotation = false
                pausePosition = 0
            } else {
                mediaPlayer.start()
            }
        }
    }

    private fun startVibrate() {

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val delay = 0
        val vibrate = 1000
        val sleep = 3000
        val start = 0
        val vibratePattern = longArrayOf(delay.toLong(), vibrate.toLong(), sleep.toLong())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(vibratePattern, start))
        } else {
            vibrator?.vibrate(vibratePattern, start)
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {

        when (what) {
            MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
            )
            MEDIA_ERROR_SERVER_DIED -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR SERVER DIED $extra"
            )
            MEDIA_ERROR_UNKNOWN -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR UNKNOWN $extra"
            )
        }
        stopPlay()
        stopSelf()
        return false
    }

/*    override fun onCompletion(p0: MediaPlayer?) {

        Log.e("fgbnfghfghf", "onCompletion")
        stopPlay()
        stopSelf()
    }*/

    override fun onDestroy() {

        stopPlay()
        abandonAudioFocus()
        stopSelf()
        vibrator?.cancel()
        isStarted = false
        clearTimer()
        super.onDestroy()
    }

    private fun abandonAudioFocus() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager?.abandonAudioFocusRequest(focusRequest!!)
            }
        } else {
            audioManager?.abandonAudioFocus(this)
        }
    }

    fun stopPlay() {

        mMediaPlayer?.let {
            if (isStarted) {
                offMediaPlayer()
            } else {
                if (mMediaPlayer?.isPlaying == true) {
                    mMediaPlayer?.stop()
                    mMediaPlayer?.reset()
                    abandonAudioFocus()
                }
            }
        }
    }

    fun offMediaPlayer() {

        if (mMediaPlayer != null) {
            if (mMediaPlayer?.isPlaying == true) {
                mMediaPlayer?.stop()
            }
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
        abandonAudioFocus()
    }

    fun startPlayAfterRotation(uri: String, position: Int?) {

        offMediaPlayer()
        if (uri.isNotEmpty()) {
            setUri(uri)
            pausePosition = position
            isRotation = true
            startPlay()
        }
    }

    fun startPlay() {
        if (mMediaPlayer == null) {
            initMediaPlayer()
            mMediaPlayer?.prepareAsync()
        } else {
            if (isStarted) {
                if (mMediaPlayer?.isPlaying == true)
                    mMediaPlayer?.stop()
                mMediaPlayer?.release()
                mMediaPlayer = null
                initMediaPlayer()
            } else {
                setUriOnMediaPlayer()
            }
            mMediaPlayer?.prepareAsync()
        }
    }

    fun setUri(_stringUri: String) {
        stringUri = _stringUri
    }

    fun setUri(musicId: Long) {

        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            musicId
        )
        stringUri = contentUri.toString()
    }

    private fun setUriOnMediaPlayer() {

        mMediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
            setDataSource(this@RingtoneService.applicationContext, Uri.parse(stringUri))
        }
    }

    private fun initMediaPlayer() {
        mMediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        }
        mMediaPlayer?.apply {
            setOnErrorListener(this@RingtoneService)
            setOnPreparedListener(this@RingtoneService)
            setDataSource(this@RingtoneService.applicationContext, Uri.parse(stringUri))
            setVolume(0.0f, 1.0f)
        }
    }

    private fun requestAudioFocus(): Boolean {

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val listener = this
        val result: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).run {
                        setAudioAttributes(AudioAttributes.Builder().run {
                            setUsage(AudioAttributes.USAGE_ALARM)
                            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            build()
                        })
                        setOnAudioFocusChangeListener(listener)
                        build()
                    }
                audioManager?.requestAudioFocus(focusRequest!!)
            } else {
                audioManager?.requestAudioFocus(
                    this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            } ?: -1234
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(focusChange: Int) {

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mMediaPlayer == null) {
                    initMediaPlayer()
                }
                startPlay()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                stopPlay()
                mMediaPlayer?.release()
                mMediaPlayer = null
                stopSelf()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mMediaPlayer?.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mMediaPlayer?.setVolume(0.2f, 0.2f)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): RingtoneService = this@RingtoneService
    }

    private fun createFullScreenNotification(
        context: Context?,
        timeId: String,
        timeStr: String,
        ringtoneUri: String
    ): Notification {

        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(IntentKeys.timeId, timeId)
        intent.putExtra(IntentKeys.ringtoneUri, ringtoneUri)
        intent.putExtra(IntentKeys.Alarm_R, true)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)

        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(context, 234, intent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(context, 234, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

        val color = TypedValue()
        context?.theme?.resolveAttribute(R.attr.colorSecondaryVariant, color, true)

        val notification: NotificationCompat.Builder = NotificationCompat.Builder(
            context!!, "myAlarm_Channel_Id"
        )
            .setSmallIcon(R.drawable.ic_baseline_alarm_24)
            .setContentTitle(setSpan("Alarm clock"))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(setSpan("${timeStr[0]}${timeStr[1]}:${timeStr[2]}${timeStr[3]}", true))
            )
            .addAction(
                0, "Off",
                setOffPendingIntent(context, timeId)
            )
            .addAction(
                0, "delay ${preferences.getString(PreferenceKeys.pauseDuration)}",
                setPausePendingIntent(context, timeId)
            )
            .setColor(context.getColor(R.color.notificationTextColor))
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        return notification.build()
    }

    private fun setOffPendingIntent(context: Context?, timeId: String): PendingIntent {

        val offIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = IntentKeys.offAlarm
            putExtra(IntentKeys.timeId, timeId)
            putExtra("notification", true)
        }
        return PendingIntent.getBroadcast(
            context,
            timeId.toIntOrNull() ?: 0,
            offIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun setPausePendingIntent(context: Context?, timeId: String): PendingIntent {

        val pauseIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = IntentKeys.pauseAlarm
            putExtra(IntentKeys.timeId, timeId)
        }
        return PendingIntent.getBroadcast(
            context,
            timeId.toIntOrNull() ?: 0,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun setSpan(str: String, BOLD: Boolean = false): SpannableString {

        val spannable = SpannableString(str)
        val length = str.length
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#FF2D3238")),
            0,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (BOLD) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    fun getStringUri(): String? {
        return stringUri
    }

    fun getPausePosition(): Int? {
        return mMediaPlayer?.currentPosition
    }

    fun getVolume(): Int? {
        return audioManager?.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    fun isPlaying(): Boolean {
        return mMediaPlayer?.isPlaying ?: false
    }

    inner class MTimerTask(val context: Context, val timeId: String) : TimerTask() {
        override fun run() {
            sendOffIntent(context, timeId)
        }
    }
}
