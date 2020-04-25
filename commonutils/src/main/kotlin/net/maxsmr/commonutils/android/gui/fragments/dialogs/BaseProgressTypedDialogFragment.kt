package net.maxsmr.commonutils.android.gui.fragments.dialogs

import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

abstract class BaseProgressTypedDialogFragment : TypedDialogFragment<AlertDialog>() {

    protected abstract val progressBarId: Int

    protected abstract val loadingMessageViewId: Int

    protected var progressBar: ProgressBar? = null
        private set

    protected var loadingMessageView: TextView? = null
        private set

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)
        customView?.let {
            val progressBarId: Int = progressBarId
            if (progressBarId != 0) {
                progressBar = it.findViewById(progressBarId)
            }
            val loadingMessageId: Int = loadingMessageViewId
            if (loadingMessageId != 0) {
                with(it.findViewById<TextView>(loadingMessageId)) {
                    if (this != null) {
                        val message = args.getString(ARG_MESSAGE)
                        text = message
                    }
                    loadingMessageView = this
                }
            }
        }
    }
}