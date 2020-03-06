package net.maxsmr.commonutils.android.gui

import android.util.DisplayMetrics
import android.view.View
import android.view.ViewTreeObserver

const val SIZE_UNKNOWN = -1

class LayoutSizeNotifier(val rootView: View, val targetView: View): ViewTreeObserver.OnGlobalLayoutListener {

    var listener: Listener? = null

    private var previousKeyboardHeight = SIZE_UNKNOWN
    private var keyboardHeight = previousKeyboardHeight


    override fun onGlobalLayout() {
        notifyHeightChanged()
    }

    fun getKeyboardHeightWithCheck(): Int {
        if (keyboardHeight == SIZE_UNKNOWN || keyboardHeight == 0) {
            keyboardHeight = getKeyboardHeight(rootView, targetView)
        }
        return keyboardHeight
    }


    private fun notifyHeightChanged() {
        if (listener != null) {
            keyboardHeight = getKeyboardHeightWithCheck()
            val dm: DisplayMetrics = rootView.context.getResources().getDisplayMetrics()
            val isWidthGreater = dm.widthPixels > dm.heightPixels
            if (keyboardHeight != previousKeyboardHeight) {
                previousKeyboardHeight = keyboardHeight
                rootView.post {
                    listener?.onSizeChanged(keyboardHeight, isWidthGreater)
                }
            }
        }
    }

    interface Listener {

        fun onSizeChanged(keyboardHeight: Int, isWidthGreater: Boolean)
    }
}