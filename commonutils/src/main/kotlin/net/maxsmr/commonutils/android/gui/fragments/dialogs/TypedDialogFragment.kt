package net.maxsmr.commonutils.android.gui.fragments.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import net.maxsmr.commonutils.android.gui.actions.TypedAction
import net.maxsmr.commonutils.data.text.EMPTY_STRING

const val ARG_TITLE = "AlertDialogFragment#ARG_TITLE"
const val ARG_MESSAGE = "AlertDialogFragment#ARG_MESSAGE"
const val ARG_STYLE_RES_ID = "AlertDialogFragment#ARG_STYLE_RES_ID"
const val ARG_ICON_RES_ID = "AlertDialogFragment#ARG_ICON_RES_ID"
const val ARG_BACKGROUND_RES_ID = "AlertDialogFragment#ARG_BACKGROUND_RES_ID"
const val ARG_CUSTOM_VIEW_RES_ID = "AlertDialogFragment#ARG_CUSTOM_VIEW_RES_ID"
const val ARG_CANCELABLE = "AlertDialogFragment#ARG_CANCELABLE"
const val ARG_BUTTON_POSITIVE = "AlertDialogFragment#ARG_BUTTON_OK"
const val ARG_BUTTON_NEGATIVE = "AlertDialogFragment#ARG_BUTTON_NEGATIVE"
const val ARG_BUTTON_NEUTRAL = "AlertDialogFragment#ARG_BUTTON_NEUTRAL"

/**
 * @param D type of created dialog in this fragment
 */
open class TypedDialogFragment<D : Dialog> : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    protected val createdSubject = SingleSubject.create<TypedAction<D>>()
    protected val buttonClickSubject = PublishSubject.create<TypedAction<Int>>()
    protected val keySubject = PublishSubject.create<KeyAction>()
    protected val cancelSubject = CompletableSubject.create()
    protected val dismissSubject = CompletableSubject.create()
    /**
     * initialized after onCreate
     */
    protected lateinit var args: Bundle
        private set
    /**
     * initialized after onGetLayoutInflater
     */
    protected lateinit var createdDialog: D
        private set

    protected var customView: View? = null
        private set

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args = arguments ?: Bundle()
    }

    @CallSuper
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        with(dialog) {
            createdDialog = this
                    ?: throw IllegalStateException(AlertDialog::class.java.simpleName + " was not created")
            onDialogCreated(this)
        }
        return inflater
    }

    @Suppress("UNCHECKED_CAST")
    override fun getDialog(): D? {
        return super.getDialog() as D?
    }

    // because of onCreateDialog overriding onCreateView is restricted!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // here you may not use AlertDialog.Builder at all
        return createBuilder(args).create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissSubject.onComplete()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        cancelSubject.onComplete()
    }

    @CallSuper
    override fun onClick(dialog: DialogInterface?, which: Int) {
        buttonClickSubject.onNext(TypedAction(which))
    }

    fun createdSingle(): Single<TypedAction<D>> = createdSubject.hide()

    fun buttonClickObservable(): Observable<TypedAction<Int>> = buttonClickSubject.hide()

    fun keyActionObservable(): Observable<KeyAction> = keySubject.hide()

    fun cancelCompletable(): Completable = cancelSubject.hide()

    fun dismissCompletable(): Completable = dismissSubject.hide()

    protected open fun shouldBeCancelable() = args.getBoolean(ARG_CANCELABLE, true)

    @CallSuper
    protected open fun onDialogCreated(dialog: D) {
        setupDialog(dialog)
        createdSubject.onSuccess(TypedAction(dialog))
    }

    protected open fun setupDialog(dialog: D) {
        args.getInt(ARG_BACKGROUND_RES_ID).let {
            if (it != 0) {
                dialog.window?.setBackgroundDrawableResource(it)
            }
        }
        // not working for dialog when wrapped in fragment, so call it here
        isCancelable = shouldBeCancelable()
    }

    /** override if want to create own [AlertDialog.Builder]  */
    protected open fun createBuilder(args: Bundle): AlertDialog.Builder {
        val styleResId = args.getInt(ARG_STYLE_RES_ID)
        val builder = if (styleResId == 0) AlertDialog.Builder(requireContext()) else AlertDialog.Builder(requireContext(), styleResId)
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

    abstract class Builder<F : TypedDialogFragment<*>>(protected val context: Context) {

        protected var title: String? = null
        protected var message: String? = null
        @StyleRes
        protected var styleResId = 0
        @DrawableRes
        protected var iconResId = 0
        @DrawableRes
        protected var backgroundResId = 0
        @LayoutRes
        protected var customViewResId = 0
        protected var cancelable = true
        protected var buttonPositive: String? = null
        protected var buttonNeutral: String? = null
        protected var buttonNegative: String? = null

        fun setTitle(title: String?): Builder<*> {
            this.title = title
            return this
        }

        fun setTitle(@StringRes titleResId: Int): Builder<*> {
            this.title = context.getString(titleResId)
            return this
        }

        fun setMessage(message: String?): Builder<*> {
            this.message = message
            return this
        }

        fun setMessage(@StringRes messageResId: Int): Builder<*> {
            this.message = context.getString(messageResId)
            return this
        }

        fun setStyleResId(@StyleRes styleResId: Int): Builder<*> {
            this.styleResId = styleResId
            return this
        }

        fun setIconResId(@DrawableRes iconResId: Int): Builder<*> {
            this.iconResId = iconResId
            return this
        }

        fun setBackgroundResId(@DrawableRes backgroundResId: Int): Builder<*> {
            this.backgroundResId = backgroundResId
            return this
        }

        fun setCustomView(@LayoutRes customViewResId: Int): Builder<*> {
            this.customViewResId = customViewResId
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder<*> {
            this.cancelable = cancelable
            return this
        }

        fun setButtons(positive: String?, neutral: String?, negative: String?): Builder<*> {
            buttonNegative = negative
            buttonNeutral = neutral
            buttonPositive = positive
            return this
        }

        fun setButtons(@StringRes positiveResId: Int?, @StringRes neutralResId: Int?, @StringRes negativeResId: Int?): Builder<*> {
            buttonPositive =  positiveResId?.let { context.getString(it) } ?: EMPTY_STRING
            buttonNeutral =  neutralResId?.let { context.getString(it) } ?: EMPTY_STRING
            buttonNegative = negativeResId?.let { context.getString(it) } ?: EMPTY_STRING
            return this
        }

        protected open fun createArgs(): Bundle {
            val args = Bundle()
            if (title != null) {
                args.putString(ARG_TITLE, title)
            }
            if (message != null) {
                args.putString(ARG_MESSAGE, message)
            }
            if (styleResId != 0) {
                args.putInt(ARG_STYLE_RES_ID, styleResId)
            }
            if (iconResId != 0) {
                args.putInt(ARG_ICON_RES_ID, iconResId)
            }
            if (backgroundResId != 0) {
                args.putInt(ARG_BACKGROUND_RES_ID, backgroundResId)
            }
            if (customViewResId != 0) {
                args.putInt(ARG_CUSTOM_VIEW_RES_ID, customViewResId)
            }
            if (buttonPositive != null) {
                args.putString(ARG_BUTTON_POSITIVE, buttonPositive)
            }
            if (buttonNeutral != null) {
                args.putString(ARG_BUTTON_NEUTRAL, buttonNeutral)
            }
            if (buttonNegative != null) {
                args.putString(ARG_BUTTON_NEGATIVE, buttonNegative)
            }
            args.putBoolean(ARG_CANCELABLE, cancelable)
            return args
        }

        abstract fun build(): F
    }

    data class KeyAction(
            val keyCode: Int,
            val event: KeyEvent?
    )

    class DefaultTypedDialogBuilder(context: Context) : Builder<TypedDialogFragment<AlertDialog>>(context) {

        override fun build(): TypedDialogFragment<AlertDialog> {
            return newInstance(createArgs())
        }
    }

    companion object {

        private fun newInstance(args: Bundle?): TypedDialogFragment<AlertDialog> {
            val fragment = TypedDialogFragment<AlertDialog>()
            fragment.arguments = args
            return fragment
        }
    }
}