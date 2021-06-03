package net.maxsmr.commonutils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationWrapper(val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: throw NullPointerException("NotificationManager is null")

    @MainThread
    fun showNotification(
            notificationId: Int,
            params: ChannelParams,
            config: NotificationCompat.Builder.() -> Unit
    ) {
        notificationManager.notify(notificationId, createNotification(params, config))
    }

    @MainThread
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun createNotification(
            params: ChannelParams,
            config: NotificationCompat.Builder.() -> Unit
    ): Notification {
        if (isAtLeastOreo()) {
            notificationManager.createNotificationChannel(params.channel())
        }
        return NotificationCompat.Builder(context, params.id).apply(config).build()
    }

    class ChannelParams(
            val id: String,
            val name: CharSequence,
            val importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT
    ) {

        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.O)
        fun channel() = NotificationChannel(id, name, importance)
    }
}