package net.maxsmr.rx_extensions.dialogs

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog

const val ARG_PROGRESS_BAR_ID = "BaseProgressTypedDialogFragment#ARG_PROGRESS_BAR_ID"
const val ARG_LOADING_MESSAGE_VIEW_ID = "BaseProgressTypedDialogFragment#ARG_LOADING_MESSAGE_VIEW_ID"
const val ARG_IS_ALERT = "AlertDialogFragment#ARG_IS_ALERT"
const val ARG_CONTAINER_ID = "AlertDialogFragment#ARG_CONTAINER_ID"

open class ProgressDialogFragment : AlertTypedDialogFragment() {

    protected var progressBar: ProgressBar? = null
        private set

    protected var loadingMessageView: TextView? = null
        private set

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (args.getBoolean(ARG_IS_ALERT, true)) {
            super.onCreateDialog(savedInstanceState) as AlertDialog
        } else {
            ProgressDialog(requireContext(),
                    args.getString(ARG_TITLE),
                    args.getInt(ARG_CUSTOM_VIEW_RES_ID),
                    shouldBeCancelable())
        }
    }

    override fun setupDialog(dialog: Dialog) {
        super.setupDialog(dialog)
        if (dialog is AlertDialog) {

            // FIXME wrap_content при использовании AlertDialog
            dialog.setCanceledOnTouchOutside(isCancelable)
//        dialog.setContentView(layoutRes)
            dialog.window?.let {
                val containerId = args.getInt(ARG_CONTAINER_ID)
//            it.setContentView(layoutRes)
                if (containerId != 0) {
                    val dialogContainer = it.findViewById(containerId) as View
                    dialogContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            it.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            dialogContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    })
//                forceLayoutParams(dialogContainer, WRAP_CONTENT, WRAP_CONTENT)
                } else {
                    it.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    it.setGravity(Gravity.CENTER)
                }
            }

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
    }

    abstract class BaseProgressBuilder<F : ProgressDialogFragment> @JvmOverloads constructor(
            private val isAlert: Boolean = false,
            @IdRes
            private val containerId: Int = 0,
            @IdRes
            protected val progressBarId: Int = 0,
            @IdRes
            protected val loadingMessageViewId: Int = 0
    ) : BaseTypedDialogFragment.Builder<Dialog, F>() {

        override fun createArgs(context: Context): Bundle {
            return super.createArgs(context).apply {
                if (containerId != 0) {
                    putInt(ARG_CONTAINER_ID, containerId)
                }
                if (progressBarId != 0) {
                    putInt(ARG_PROGRESS_BAR_ID, progressBarId)
                }
                if (loadingMessageViewId != 0) {
                    putInt(ARG_LOADING_MESSAGE_VIEW_ID, loadingMessageViewId)
                }
            }
        }
    }

    class ProgressBuilder : BaseProgressBuilder<ProgressDialogFragment>() {

        override fun build(context: Context) = newInstance(createArgs(context))
    }

    private class ProgressDialog(
            context: Context,
            title: String?,
            @LayoutRes contentViewResId: Int?,
            cancelable: Boolean
    ) : Dialog(context) {

        init {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setTitle(title)
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            setOnCancelListener(null)
            contentViewResId?.takeIf { it != 0 }?.let {
                setContentView(it)
            }
        }
    }

    companion object {

        private fun newInstance(args: Bundle?): ProgressDialogFragment {
            val fragment = ProgressDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}