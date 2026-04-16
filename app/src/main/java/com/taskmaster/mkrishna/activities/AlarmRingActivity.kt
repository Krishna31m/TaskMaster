package com.taskmaster.mkrishna.activities

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.taskmaster.mkrishna.R
import com.taskmaster.mkrishna.databinding.ActivityAlarmRingBinding
import com.taskmaster.mkrishna.utils.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.*

class AlarmRingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingBinding
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val clockHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen and turn screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: ""
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"

        binding.tvLabel.text = "⏰ $label"
        startClock()
        startPulseAnimation()
        playRingtone()
        startVibration()

        binding.btnDismiss.setOnClickListener {
            stopAlarm()
            finish()
        }

        binding.btnSnooze.setOnClickListener {
            stopAlarm()
            // Reschedule 5 minutes later
            val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000)
            AlarmScheduler.scheduleAlarm(this, alarmId, label, snoozeTime)
            finish()
        }
    }

    private fun startClock() {
        val updateClock = object : Runnable {
            override fun run() {
                binding.tvTime.text = timeFormat.format(Date())
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(updateClock)
    }

    private fun startPulseAnimation() {
        val pulse1 = AnimationUtils.loadAnimation(this, R.anim.pulse_ring)
        val pulse2 = AnimationUtils.loadAnimation(this, R.anim.pulse_ring).apply { startOffset = 400 }
        val pulse3 = AnimationUtils.loadAnimation(this, R.anim.pulse_ring).apply { startOffset = 800 }
        val iconBounce = AnimationUtils.loadAnimation(this, R.anim.bounce)

        binding.circlePulse1.startAnimation(pulse1)
        binding.circlePulse2.startAnimation(pulse2)
        binding.circlePulse3.startAnimation(pulse3)
        binding.ivAlarmIcon.startAnimation(iconBounce)
    }

    private fun playRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibrator = vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 500, 500, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        clockHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
