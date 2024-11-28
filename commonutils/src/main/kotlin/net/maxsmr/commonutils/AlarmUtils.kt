package net.maxsmr.commonutils

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.AlarmManagerCompat
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException


private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AlarmUtils")

@JvmOverloads
@RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
fun Context.setAlarm(
    alarmIntent: PendingIntent,
    delayTime: Long,
    shouldWakeUp: Boolean,
    showIntent: PendingIntent? = null
): Boolean {
    if (delayTime <= 0) {
        logger.e("Incorrect delay time: $delayTime")
        return false
    }
    return setAlarm(
        alarmIntent,
        System.currentTimeMillis() + delayTime,
        if (shouldWakeUp) AlarmType.RTC_WAKE_UP else AlarmType.RTC,
        showIntent
    )
}

@JvmOverloads
@RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
fun Context.setAlarm(
    alarmIntent: PendingIntent,
    triggerTime: Long,
    alarmType: AlarmType,
    showIntent: PendingIntent? = null
): Boolean {
    logger.d("setAlarm(), alarmIntent: $alarmIntent, triggerTime: $triggerTime, alarmType: $alarmType, showIntent: $showIntent")

    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        ?: throw RuntimeException(AlarmManager::class.java.simpleName + " is null")

    if (alarmType.isRTC) {
        val currentTime = System.currentTimeMillis()
        if (triggerTime <= currentTime) {
            logger.e("trigger time ($triggerTime) <= current time ($currentTime)")
            return false
        }
    } else {
        val elapsedTime = SystemClock.elapsedRealtime()
        if (triggerTime <= elapsedTime) {
            logger.e("trigger time ($triggerTime) <= elapsed time ($elapsedTime)")
            return false
        }
    }

    if (isAtLeastS()) {
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            if (isAtLeastTiramisu()) {
                intent.setData(Uri.parse("package:$packageName"))
            }
            startActivity(intent)
            return false
        }
    }

    return try {
        if (isAtLeastLollipop()) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, showIntent), alarmIntent)
        } else {
            // setExact
            AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, alarmType.value, triggerTime, alarmIntent)
        }
        true
    } catch (e: Exception) {
        logger.e(formatException(e, "set alarm"))
        false
    }
}

fun Context.cancelAlarm(pendingIntent: PendingIntent): Boolean {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        ?: throw RuntimeException(AlarmManager::class.java.simpleName + " is null")
    return try {
        alarmManager.cancel(pendingIntent)
        true
    } catch (e: Exception) {
        logger.e(formatException(e, "cancel"))
        false
    }
}

enum class AlarmType(val value: Int) {

    RTC(AlarmManager.RTC),
    RTC_WAKE_UP(AlarmManager.RTC_WAKEUP),
    ELAPSED_REALTIME(AlarmManager.ELAPSED_REALTIME),
    ELAPSED_REALTIME_WAKE_UP(AlarmManager.ELAPSED_REALTIME_WAKEUP);

    val isRTC: Boolean
        get() = this == RTC || this == RTC_WAKE_UP

    val isElapsed: Boolean
        get() = this == ELAPSED_REALTIME || this == ELAPSED_REALTIME_WAKE_UP
}
