package net.maxsmr.commonutils.android.gui.fragments.actions.message

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.commonutils.data.text.EMPTY_STRING

/**
 * Действие для показа тоста
 */
data class ToastMessageAction(
        val message: CharSequence = EMPTY_STRING,
        val messageResId: Int = 0,
        val gravity: Int? = null,
        val xOffset: Int = 0,
        val yOffset: Int = 0,
        val duration: Int = Toast.LENGTH_SHORT,
        val customView: View? = null
): BaseMessageAction<Toast>() {

    @SuppressLint("ShowToast")
    override fun show(context: Context): Toast {
        val toast: Toast
        var duration = duration
        if (duration != Toast.LENGTH_SHORT && duration != Toast.LENGTH_LONG) {
            duration = Toast.LENGTH_SHORT
        }
        if (customView == null) {
            val message = if (messageResId != 0) {
                context.getString(messageResId)
            } else {
                message
            }
            toast = Toast.makeText(context, message, duration)
        } else {
            toast = Toast(context)
            toast.view = customView
            toast.duration = duration
        }
        gravity?.let {
            toast.setGravity(gravity, xOffset, yOffset)
        }
        toast.show()
        return toast
    }

    override fun hide(message: Toast) {
        message.cancel()
    }
}