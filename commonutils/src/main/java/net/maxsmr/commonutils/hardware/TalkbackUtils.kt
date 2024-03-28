package net.maxsmr.commonutils.hardware

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

@JvmOverloads
fun View.requestAccessibilityFocus(shouldSelect: Boolean = true): View {
    performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    if (shouldSelect) {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
    }
    return this
}

/**
 * Устанавливает фокус без озвучки
 */
fun View.requestFocusWithoutAnnounce() {
    val isImportantForAccessibility = importantForAccessibility
    post {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        // запрашиваем фокус
        requestAccessibilityFocus(false)
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                importantForAccessibility = isImportantForAccessibility
                accessibilityDelegate = null
            }
        }
    }
}