package net.maxsmr.commonutils.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.SystemClock
import android.util.Log
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("AlarmUtils")

fun setAlarm(context: Context, pIntent: PendingIntent, delayTime: Long, shouldWakeUp: Boolean): Boolean {
    if (delayTime <= 0) {
        logger.e("Incorrect delay time: $delayTime")
        return false
    }
    return setAlarm(context, pIntent, System.currentTimeMillis() + delayTime, if (shouldWakeUp) AlarmType.RTC_WAKE_UP else AlarmType.RTC)
}

/** compat use of [AlarmManager]  */
fun setAlarm(context: Context, pIntent: PendingIntent, triggerTime: Long, alarmType: AlarmType): Boolean {
    logger.d("setAlarm(), pIntent=$pIntent, triggerTime=$triggerTime, alarmType=$alarmType")

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            ?: throw RuntimeException(AlarmManager::class.java.simpleName + " is null")

    val alarmTypeInt: Int

    when (alarmType) {
        AlarmType.RTC -> alarmTypeInt = AlarmManager.RTC
        AlarmType.RTC_WAKE_UP -> alarmTypeInt = AlarmManager.RTC_WAKEUP
        AlarmType.ELAPSED_REALTIME -> alarmTypeInt = AlarmManager.ELAPSED_REALTIME
        AlarmType.ELAPSED_REALTIME_WAKE_UP -> alarmTypeInt = AlarmManager.ELAPSED_REALTIME_WAKEUP
        else -> throw IllegalArgumentException("Unknown " + AlarmType::class.java.simpleName + ": " + alarmType)
    }

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

    if (SDK_INT < Build.VERSION_CODES.KITKAT) {
        alarmManager.set(alarmTypeInt, triggerTime, pIntent)
    } else if (SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        alarmManager.setExact(alarmTypeInt, triggerTime, pIntent)
    } else if (SDK_INT < Build.VERSION_CODES.M) {
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, null), pIntent)
    } else {
        alarmManager.setExactAndAllowWhileIdle(alarmTypeInt, triggerTime, pIntent)
    }

    return true
}

fun cancelAlarm(context: Context, pendingIntent: PendingIntent) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            ?: throw RuntimeException(AlarmManager::class.java.simpleName + " is null")
    alarmManager.cancel(pendingIntent)
}

enum class AlarmType {
    RTC, RTC_WAKE_UP, ELAPSED_REALTIME, ELAPSED_REALTIME_WAKE_UP;

    val isRTC: Boolean
        get() = this == RTC || this == RTC_WAKE_UP

    val isElapsed: Boolean
        get() = this == ELAPSED_REALTIME || this == ELAPSED_REALTIME_WAKE_UP
}
