package net.maxsmr.commonutils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock
import androidx.core.app.AlarmManagerCompat
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AlarmUtils")

@JvmOverloads
fun setAlarm(
        context: Context,
        alarmIntent: PendingIntent,
        delayTime: Long,
        shouldWakeUp: Boolean,
        showIntent: PendingIntent? = null
): Boolean {
    if (delayTime <= 0) {
        logger.e("Incorrect delay time: $delayTime")
        return false
    }
    return setAlarm(context,
            alarmIntent,
            System.currentTimeMillis() + delayTime,
            if (shouldWakeUp) AlarmType.RTC_WAKE_UP else AlarmType.RTC,
            showIntent
    )
}

/** compat use of [AlarmManager]  */
@JvmOverloads
fun setAlarm(
        context: Context,
        alarmIntent: PendingIntent,
        triggerTime: Long,
        alarmType: AlarmType,
        showIntent: PendingIntent? = null
): Boolean {
    logger.d("setAlarm(), alarmIntent: $alarmIntent, triggerTime: $triggerTime, alarmType: $alarmType, showIntent: $showIntent")

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
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

    // setExactAndAllowWhileIdle is not working in doze mode
    return try {
        if (isAtLeastLollipop()) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, showIntent), alarmIntent)
        } else {
            AlarmManagerCompat.setExact(alarmManager, alarmType.value, triggerTime, alarmIntent)
        }
        true
    } catch (e: Exception) {
        logger.e(formatException(e, "set alarm"))
        false
    }
}

fun cancelAlarm(context: Context, pendingIntent: PendingIntent): Boolean {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
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
