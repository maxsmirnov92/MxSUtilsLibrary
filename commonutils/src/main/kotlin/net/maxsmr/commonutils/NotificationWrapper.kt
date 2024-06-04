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
    private val notificationPermissionChecker: () -> Boolean
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * @param channelConfig дополнительный конфигуратор для [NotificationChannel] для версий выше O,
     * плюсом к тому, что был в констуркторе [ChannelParams]
     */
    fun show(
        notificationId: Int,
        params: ChannelParams,
        notificationConfig: NotificationCompat.Builder.() -> Unit,
    ) {
        show(notificationId, create(params, notificationConfig))
    }

    fun show(notificationId: Int, notification: Notification) {
        if (notificationPermissionChecker()) {
            notificationManager.notify(notificationId, notification)
        }
    }

    fun create(
        params: ChannelParams,
        notificationConfig: NotificationCompat.Builder.() -> Unit,
    ): Notification {
        if (isAtLeastOreo()) {
            val channel = params.channel()
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(context, params.id).apply(notificationConfig).build()
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    class ChannelParams(
        val id: String,
        val name: CharSequence,
        val importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
        val config: (NotificationChannel.() -> Unit)? = null,
    ) {

        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.O)
        fun channel() = NotificationChannel(id, name, importance).apply {
            config?.invoke(this)
        }
    }

    companion object {

        fun NotificationCompat.Builder.setContentBigText(text: String?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setStyle(NotificationCompat.BigTextStyle().bigText(text))
            } else {
                setContentText(text)
            }
        }
    }
}