package net.maxsmr.commonutils.gui.fragments.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import net.maxsmr.commonutils.gui.actions.TypedAction
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage
import java.io.Serializable

const val ARG_TITLE = "TypedDialogFragment#ARG_TITLE"
const val ARG_MESSAGE = "TypedDialogFragment#ARG_MESSAGE"
const val ARG_STYLE_RES_ID = "TypedDialogFragment#ARG_STYLE_RES_ID"
const val ARG_ICON_RES_ID = "TypedDialogFragment#ARG_ICON_RES_ID"
const val ARG_BACKGROUND_RES_ID = "TypedDialogFragment#ARG_BACKGROUND_RES_ID"
const val ARG_CUSTOM_VIEW_RES_ID = "TypedDialogFragment#ARG_CUSTOM_VIEW_RES_ID"
const val ARG_CANCELABLE = "TypedDialogFragment#ARG_CANCELABLE"
const val ARG_BUTTON_POSITIVE = "TypedDialogFragment#ARG_BUTTON_OK"
const val ARG_BUTTON_NEGATIVE = "TypedDialogFragment#ARG_BUTTON_NEGATIVE"
const val ARG_BUTTON_NEUTRAL = "TypedDialogFragment#ARG_BUTTON_NEUTRAL"
const val ARG_ADAPTER_CONTENT = "TypedDialogFragment#ARG_ADAPTER_CONTENTL"

/**
 * @param D type of created dialog in this fragment
 */
abstract class BaseTypedDialogFragment<D : Dialog> : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    protected val createdSubject = SingleSubject.create<TypedAction<D>>()
    protected val buttonClickSubject = PublishSubject.create<TypedAction<Int>>()
    protected val itemClickSubject = PublishSubject.create<TypedAction<Int>>()
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

    // because of onCreateDialog overriding onCreateView is restricted!
    abstract override fun onCreateDialog(savedInstanceState: Bundle?): D

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

    fun itemClickObservable(): Observable<TypedAction<Int>> = itemClickSubject.hide()

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

    abstract class Builder<D : Dialog, F : BaseTypedDialogFragment<D>>() {

        protected var title: TextMessage? = null

        protected var message: TextMessage? = null

        @StyleRes
        protected var styleResId = 0

        @DrawableRes
        protected var iconResId = 0

        @DrawableRes
        protected var backgroundResId = 0

        @LayoutRes
        protected var customViewResId = 0

        protected var cancelable = true

        protected var buttonPositive: TextMessage? = null
        protected var buttonNeutral: TextMessage? = null
        protected var buttonNegative: TextMessage? = null

        protected var adapterContent: AdapterData? = null

        fun setTitle(title: TextMessage?): Builder<D, F> = apply {
            this.title = title
        }

        fun setMessage(message: TextMessage?): Builder<D, F> = apply {
            this.message = message
        }

        fun setStyleResId(@StyleRes styleResId: Int): Builder<D, F> = apply {
            this.styleResId = styleResId
        }

        fun setIconResId(@DrawableRes iconResId: Int): Builder<D, F> = apply {
            this.iconResId = iconResId
        }

        fun setBackgroundResId(@DrawableRes backgroundResId: Int): Builder<D, F> = apply {
            this.backgroundResId = backgroundResId
        }

        fun setCustomView(@LayoutRes customViewResId: Int): Builder<D, F> = apply {
            this.customViewResId = customViewResId
        }

        fun setCancelable(cancelable: Boolean): Builder<D, F> = apply {
            this.cancelable = cancelable
        }

        fun setButtons(
                positive: TextMessage? = null,
                neutral: TextMessage? = null,
                negative: TextMessage? = null
        ): Builder<D, F> {
            buttonNegative = negative
            buttonNeutral = neutral
            buttonPositive = positive
            return this
        }

        fun setAdapterData(adapterContent: AdapterData?): Builder<D, F> {
            this.adapterContent = adapterContent
            return this
        }

        protected open fun createArgs(context: Context): Bundle {
            val args = Bundle()
            title?.let {
                args.putCharSequence(ARG_TITLE, it.get(context))
            }
            message?.let {
                args.putCharSequence(ARG_MESSAGE, it.get(context))
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
            buttonPositive?.let {
                args.putCharSequence(ARG_BUTTON_POSITIVE, it.get(context))
            }
            buttonNeutral?.let {
                args.putCharSequence(ARG_BUTTON_NEUTRAL, it.get(context))
            }
            buttonNegative?.let {
                args.putCharSequence(ARG_BUTTON_NEGATIVE, it.get(context))
            }
            adapterContent?.let {
                args.putSerializable(ARG_ADAPTER_CONTENT, it)
            }
            args.putBoolean(ARG_CANCELABLE, cancelable)
            return args
        }

        abstract fun build(context: Context): F
    }

    data class KeyAction(
            val keyCode: Int,
            val event: KeyEvent?
    )

    data class AdapterData(
            val messages: ArrayList<String>,
            @LayoutRes
            val itemLayoutResId: Int,
            @IdRes
            val textResId: Int,
            val dismissOnClick: Boolean = false
    ): Serializable
}