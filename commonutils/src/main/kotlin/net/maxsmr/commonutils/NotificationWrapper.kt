package net.maxsmr.commonutils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.lang.RuntimeException

@MainThread
class NotificationWrapper(
    private val context: Context,
    private val notificationPermissionChecker: () -> Boolean
) {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("NotificationWrapper")

    private val notificationManager = NotificationManagerCompat.from(context)

    private val channelGroupsMap = mutableMapOf<String, NotificationChannelGroup>()

    private val channelsMap = mutableMapOf<String, NotificationChannel>()

    private val notificationsMap = mutableMapOf<Int, NotificationCompat.Builder>()

    fun createNotificationChannelGroup(
        params: ChannelParams.ChannelGroupParams
    ): NotificationChannelGroup? {
        return if (isAtLeastOreo()) {
            NotificationChannelGroup(params.groupId, params.name).apply {
                if (isAtLeastPie()) {
                    params.description?.takeIf { it.isNotEmpty() }?.let {
                        description = it
                    }
                }
                channelGroupsMap[params.groupId] = this
                notificationManager.createNotificationChannelGroup(this)
            }
        } else {
            null
        }
    }

    fun createNotificationChannel(params: ChannelParams): Boolean {
        return if (isAtLeastOreo()) {
            val channel = params.channel()
            params.groupParams?.groupId?.takeIf { it.isNotEmpty() }?.let {
                if (channelGroupsMap.containsKey(it)) {
                    channel.group = it
                }
            }
            notificationManager.createNotificationChannel(channel)
            channelsMap[params.id] = channel
            true
        } else {
            false
        }
    }

    fun getNotificationChannel(channelId: String) = notificationManager.getNotificationChannel(channelId)

    fun show(
        notificationId: Int,
        params: ChannelParams,
        notificationConfig: NotificationCompat.Builder.() -> Unit,
    ) {
        show(notificationId, create(notificationId, params, notificationConfig))
    }

    @SuppressLint("MissingPermission")
    fun show(notificationId: Int, notification: Notification) {
        if (notificationPermissionChecker()) {
            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: RuntimeException) {
                logger.e(e)
            }
        }
    }

    fun create(
        notificationId: Int,
        params: ChannelParams,
        notificationConfig: NotificationCompat.Builder.() -> Unit,
    ): Notification {
        if (!channelsMap.containsKey(params.id)) {
            params.groupParams?.let {
                createNotificationChannelGroup(it)
            }
            createNotificationChannel(params)
        }
        // реюз Builder
        val b = notificationsMap.getOrPut(notificationId) {NotificationCompat.Builder(context, params.id)}
        b.clearActions()
        b.clearPeople()
        return b.apply(notificationConfig).build()
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    class ChannelParams(
        val id: String,
        val name: CharSequence,
        val groupParams: ChannelGroupParams? = null,
        val importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
        val config: (NotificationChannel.() -> Unit)? = null,
    ) {

        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.O)
        fun channel() = NotificationChannel(id, name, importance).apply {
            config?.invoke(this)
        }

        data class ChannelGroupParams(
            val groupId: String,
            val name: CharSequence,
            val description: String? = null
        )
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