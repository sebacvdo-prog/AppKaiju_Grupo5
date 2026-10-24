package com.example.appkaiju_grupo5.device

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

/** Se invoca por confirmación de escritura, nunca desde la composición. */
class VibracionInventario(private val context: Context) {
    @Suppress("DEPRECATION")
    fun confirmar(): Boolean {
        if (Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return false
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else context.getSystemService(Vibrator::class.java)
        if (vibrator?.hasVibrator() != true) return false
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                else -> vibrator.vibrate(30)
            }
        } catch (_: SecurityException) {
            return false
        }
        return true
    }
}
