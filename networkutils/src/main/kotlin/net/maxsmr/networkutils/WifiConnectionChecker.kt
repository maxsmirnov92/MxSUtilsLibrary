package net.maxsmr.networkutils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.BaseBroadcastWrapper
import net.maxsmr.commonutils.live.setValueIfNew

open class WifiConnectionChecker(context: Context) : BaseBroadcastWrapper(context,
        IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
        }) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
            ?: throw NullPointerException("WifiManager is null")

    val connectionInfo = MutableLiveData<WifiInfo>()

    override fun onReceive(intent: Intent) {
        connectionInfo.setValueIfNew(wifiManager.connectionInfo)
    }
}