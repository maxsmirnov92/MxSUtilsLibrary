package net.maxsmr.commonutils.service

import android.app.Service
import android.content.Intent
import androidx.annotation.CallSuper
import net.maxsmr.commonutils.service.ServiceBroadcastReceiver.Companion.sendBroadcastCreated
import net.maxsmr.commonutils.service.ServiceBroadcastReceiver.Companion.sendBroadcastDestroyed
import net.maxsmr.commonutils.service.ServiceBroadcastReceiver.Companion.sendBroadcastStarted

abstract class BaseBroadcastedService: Service() {

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        sendBroadcastCreated(this)
    }

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendBroadcastStarted(this)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        sendBroadcastDestroyed(this)
    }
}