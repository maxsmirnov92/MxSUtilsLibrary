package net.maxsmr.rx_extensions.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import net.maxsmr.rx_extensions.actions.TypedAction

open class AlertTypedDialogFragment: BaseTypedDialogFragment<Dialog>() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // here you may not use AlertDialog.Builder at all
        return createBuilder(args).create()
    }

    /** override if want to create own [AlertDialog.Builder]  */
    protected open fun createBuilder(args: Bundle): AlertDialog.Builder {
        val context = requireContext()
        val styleResId = args.getInt(ARG_STYLE_RES_ID)
        val builder = if (styleResId == 0) AlertDialog.Builder(context) else AlertDialog.Builder(context, styleResId)
        builder.setTitle(args.getString(ARG_TITLE))
        if (args.containsKey(ARG_ICON_RES_ID)) {
            builder.setIcon(args.getInt(ARG_ICON_RES_ID))
        }
        val customViewId = args.getInt(ARG_CUSTOM_VIEW_RES_ID)
        if (customViewId != 0) {
            with(LayoutInflater.from(context).inflate(customViewId, null)) {
                customView = this
                builder.setView(this)
            }
        } else {
            builder.setMessage(args.getString(ARG_MESSAGE))
            (args.getSerializable(ARG_ADAPTER_CONTENT) as AdapterData?)?.let { adapterData ->
                builder.setAdapter(ArrayAdapter(context, adapterData.itemLayoutResId, adapterData.textResId, adapterData.messages)) { _, which ->
                    itemClickSubject.onNext(TypedAction(which))
                    if (adapterData.dismissOnClick) {
                        dismiss()
                    }
                }
            }
        }
        if (args.containsKey(ARG_BUTTON_POSITIVE)) {
            builder.setPositiveButton(args.getString(ARG_BUTTON_POSITIVE), this)
        }
        if (args.containsKey(ARG_BUTTON_NEUTRAL)) {
            builder.setNeutralButton(args.getString(ARG_BUTTON_NEUTRAL), this)
        }
        if (args.containsKey(ARG_BUTTON_NEGATIVE)) {
            builder.setNegativeButton(args.getString(ARG_BUTTON_NEGATIVE), this)
        }
        builder.setOnKeyListener { dialog: DialogInterface?, keyCode: Int, event: KeyEvent? ->
            keySubject.onNext(KeyAction(keyCode, event))
            return@setOnKeyListener false
        }
        return builder
    }

    class Builder : BaseTypedDialogFragment.Builder<Dialog, AlertTypedDialogFragment>() {

        override fun build(context: Context): AlertTypedDialogFragment = newInstance(createArgs(context))
    }

    companion object {

        private fun newInstance(args: Bundle?): AlertTypedDialogFragment {
            val fragment = AlertTypedDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}