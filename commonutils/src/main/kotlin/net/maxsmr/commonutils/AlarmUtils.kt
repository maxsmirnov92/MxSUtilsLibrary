package net.maxsmr.commonutils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AlarmUtils")

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

    if (isPreKitkat()) {
        alarmManager.set(alarmType.type, triggerTime, pIntent)
    } else if (isPreLollipop()) {
        alarmManager.setExact(alarmType.type, triggerTime, pIntent)
    } else if (isPreMarshmallow()) {
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, null), pIntent)
    } else {
        alarmManager.setExactAndAllowWhileIdle(alarmType.type, triggerTime, pIntent)
    }
    return true
}

fun cancelAlarm(context: Context, pendingIntent: PendingIntent) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            ?: throw RuntimeException(AlarmManager::class.java.simpleName + " is null")
    alarmManager.cancel(pendingIntent)
}

enum class AlarmType(val type: Int) {

    RTC(AlarmManager.RTC),
    RTC_WAKE_UP(AlarmManager.RTC_WAKEUP),
    ELAPSED_REALTIME(AlarmManager.ELAPSED_REALTIME),
    ELAPSED_REALTIME_WAKE_UP(AlarmManager.ELAPSED_REALTIME_WAKEUP);

    val isRTC: Boolean
        get() = this == RTC || this == RTC_WAKE_UP

    val isElapsed: Boolean
        get() = this == ELAPSED_REALTIME || this == ELAPSED_REALTIME_WAKE_UP
}
