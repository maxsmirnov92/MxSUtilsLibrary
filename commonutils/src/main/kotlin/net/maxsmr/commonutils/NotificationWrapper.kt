package net.maxsmr.commonutils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

@MainThread
class NotificationWrapper(
        private val context: Context,
        private val params: ChannelParams
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun show(notificationId: Int, config: NotificationCompat.Builder.() -> Unit) {
        show(notificationId, create(config))
    }

    fun show(notificationId: Int, notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }

    fun create(config: NotificationCompat.Builder.() -> Unit): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(params.channel())
        }
        return NotificationCompat.Builder(context, params.id).apply(config).build()
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    class ChannelParams(
            val id: String,
            val name: CharSequence,
            val importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
            val config:  (NotificationChannel.() -> Unit)? = null
    ) {

        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.O)
        fun channel() = NotificationChannel(id, name, importance).apply {
            config?.invoke(this)
        }
    }
}