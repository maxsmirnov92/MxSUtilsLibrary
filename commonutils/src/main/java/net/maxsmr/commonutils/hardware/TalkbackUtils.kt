package net.maxsmr.commonutils.hardware

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

fun Context.sendAnnouncementEvent(text: String?) {
    if (text.isNullOrEmpty()) return
    val accessibilityEvent = AccessibilityEvent.obtain()
    accessibilityEvent.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
    accessibilityEvent.text.add(text)
    val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager? ?: return
    if (!manager.isEnabled) return
    manager.sendAccessibilityEvent(accessibilityEvent)
}