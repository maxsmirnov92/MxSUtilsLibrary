package net.maxsmr.commonutils.gui.fragments.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.IdRes

const val ARG_PROGRESS_BAR_ID = "BaseProgressTypedDialogFragment#ARG_PROGRESS_BAR_ID"
const val ARG_LOADING_MESSAGE_VIEW_ID = "BaseProgressTypedDialogFragment#ARG_LOADING_MESSAGE_VIEW_ID"

abstract class BaseProgressDialogFragment<D : Dialog> : TypedDialogFragment<D>() {

    protected var progressBar: ProgressBar? = null
        private set

    protected var loadingMessageView: TextView? = null
        private set

    override fun onDialogCreated(dialog: D) {
        super.onDialogCreated(dialog)
        customView?.let {
            val progressBarId: Int = args.getInt(ARG_PROGRESS_BAR_ID)
            if (progressBarId != 0) {
                progressBar = it.findViewById(progressBarId)
            }
            val loadingMessageId: Int = args.getInt(ARG_LOADING_MESSAGE_VIEW_ID)
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

    abstract class Builder<D : Dialog, F : BaseProgressDialogFragment<D>> @JvmOverloads constructor(
            @IdRes
            protected val progressBarId: Int = 0,
            @IdRes
            protected val loadingMessageViewId: Int = 0
    ) : TypedDialogFragment.Builder<D, F>() {

        override fun createArgs(context: Context): Bundle {
            return super.createArgs(context).apply {
                if (progressBarId != 0) {
                    putInt(ARG_PROGRESS_BAR_ID, progressBarId)
                }
                if (loadingMessageViewId != 0) {
                    putInt(ARG_LOADING_MESSAGE_VIEW_ID, loadingMessageViewId)
                }
            }
        }
    }
}