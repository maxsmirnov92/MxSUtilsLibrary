package net.maxsmr.networkutils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.android.BaseBroadcastWrapper
import net.maxsmr.commonutils.android.live.setValueIfNew

open class WifiConnectionChecker(context: Context) : BaseBroadcastWrapper(context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
            ?: throw NullPointerException("WifiManager is null")

    val connectionInfo = MutableLiveData<WifiInfo>()

    override val intentFilter: IntentFilter
        get() = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
        }

    @CallSuper
    override fun doAction(intent: Intent) {
        connectionInfo.setValueIfNew(wifiManager.connectionInfo)
    }
}