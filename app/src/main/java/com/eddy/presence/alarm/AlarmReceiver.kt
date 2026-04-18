package com.eddy.presence.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.eddy.presence.service.PresenceForegroundService
import com.eddy.presence.state.SessionStateStore

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = SessionStateStore(context)
        store.timerExpired = true
        store.pendingAcknowledgement = true

        if (store.notifyFlashlight) {
            TorchController.turnOn(context)
        }

        if (store.notifyVibration) {
            vibrate(context)
        }

        if (store.notifyAlarm) {
            // Delegate looping ringtone to the service — it outlives this receiver
            PresenceForegroundService.fireAlarm(context)
        }
    }

    private fun vibrate(context: Context) {
        val effect = VibrationEffect.createOneShot(3_000, VibrationEffect.DEFAULT_AMPLITUDE)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(effect, audioAttr)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, audioAttr)
        }
    }
}
