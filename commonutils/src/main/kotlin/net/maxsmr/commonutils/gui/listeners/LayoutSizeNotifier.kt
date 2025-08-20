package net.maxsmr.commonutils.gui.listeners

import android.app.Activity
import android.view.ViewTreeObserver
import net.maxsmr.commonutils.getDisplaySize
import net.maxsmr.commonutils.getKeyboardHeight

class LayoutSizeNotifier(private val activity: Activity) : ViewTreeObserver.OnGlobalLayoutListener {

    var callback: ((keyboardHeight: Int, isWidthGreater: Boolean) -> Unit)? = null

    private var _previousKeyboardHeight: Int? = null
    private var _keyboardHeight = _previousKeyboardHeight

    override fun onGlobalLayout() {
        val callback = callback ?: return
        val size = activity.getDisplaySize()
        val isWidthGreater = size.width > size.height
        if (_keyboardHeight != _previousKeyboardHeight) {
            _previousKeyboardHeight = _keyboardHeight
            callback(getKeyboardHeight(), isWidthGreater)
        }
    }

    fun getKeyboardHeight(): Int {
        return _keyboardHeight.let {
            if (it == null || it == 0) {
                activity.getKeyboardHeight().also { height ->
                    _keyboardHeight = height
                }
            } else {
                it
            }
        }
    }
}