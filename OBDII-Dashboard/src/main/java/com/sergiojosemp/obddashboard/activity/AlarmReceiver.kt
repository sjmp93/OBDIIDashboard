package com.sergiojosemp.obddashboard.activity

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.sergiojosemp.obddashboard.vm.TAG


class AlarmReceiver : BroadcastReceiver() {

    private val _REFRESH_INTERVAL = 60 * 1 // 1 minutes


    private val ALARM_ID = 102 // This can be any random integer.


    var pi: PendingIntent? = null
    var am: AlarmManager? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        /* All actions to be handled here.. */
        Log.d(TAG, "Log ${SystemClock.uptimeMillis() / 1000L}")
        val vibrator: Vibrator = context!!.getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            //vibrator.vibrate(pattern, 0)
        }
        SetContext(context)
        SetAlarm()

    }


    // This is to initialize the alarmmanager and the pending intent. It is done in separate method because, the same alarmmanager and
    // pending intent instance should be used for setting and cancelling the alarm.

    // This is to initialize the alarmmanager and the pending intent. It is done in separate method because, the same alarmmanager and
    // pending intent instance should be used for setting and cancelling the alarm.
    fun SetContext(context: Context) {
        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        pi =
            PendingIntent.getBroadcast(context, ALARM_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // Setting the alarm to call onRecieve every _REFRESH_INTERVAL seconds
    fun SetAlarm() {
        // am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * _REFRESH_INTERVAL , pi);
        try {
            am!!.cancel(pi)
        } catch (ignored: Exception) {
        }
        am!!.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP ,
            System.currentTimeMillis() + 1000 * _REFRESH_INTERVAL.toLong(),
            pi
        )
    }

    // Cancel the alarm.
    fun CancelAlarm() {
        am!!.cancel(pi)
    }

}