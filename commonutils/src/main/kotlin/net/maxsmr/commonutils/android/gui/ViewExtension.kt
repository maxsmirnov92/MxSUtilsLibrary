package net.maxsmr.commonutils.android.gui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.CountDownTimer
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.android.SdkUtils
import net.maxsmr.commonutils.android.getColorFilteredDrawable
import net.maxsmr.commonutils.android.getColorForAttrs
import net.maxsmr.commonutils.android.gui.GuiUtils.hideKeyboard
import net.maxsmr.commonutils.android.gui.GuiUtils.showKeyboard
import net.maxsmr.commonutils.android.getBrowseLinkIntent
import net.maxsmr.commonutils.data.parseHtmlToSpannedString
import java.util.concurrent.TimeUnit

private const val DEFAULT_DARK_COLOR_RATIO = 0.7

/**
 * Устанавливает курсор в конец строки
 */
fun EditText.selectionToEnd() {
    if (text.isNotEmpty()) {
        setSelection(text.length)
    }
}

/**
 * Установка [IntervalEditorActionListener] на editor action у [EditText]
 * срабатывает на любой actionId
 * @param action пользовательское действие, которое необходимо выполнить при срабатывании
 * (должно вернуть true, если было обработано)
 */
fun TextView.setOnIntervalEditorActionListener(action: ((TextView, Int, KeyEvent?) -> Boolean)?) {
    setOnEditorActionListener(object : IntervalEditorActionListener() {

        override fun shouldDoAction(v: TextView, actionId: Int, event: KeyEvent?) = true

        override fun doAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
            action?.let {
                return it(v, actionId, event)
            }
            return false
        }
    })
}

/**
 * Установка [IntervalEditorActionListener] на editor action у [EditText],
 * срабатывает только на Enter
 * @param action пользовательское действие, которое необходимо выполнить при срабатывании
 */
fun TextView.setOnEnterIntervalEditorActionListener(action: ((TextView) -> Unit)?) {
    setOnEditorActionListener(object : IntervalEditorActionListener() {

        override fun doAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
            action?.let {
                it(v)
            }
            return true
        }
    })
}

/**
 *  Установка [IntervalEditorActionListener] на editor action у [EditText],
 *  срабатывает на Enter и на [EditorInfo.IME_ACTION_NEXT]
 *  @param nextView [View], на которую следует перевести фокус
 */
fun TextView.setOnFocusIntervalEditorActionListener(nextView: View) {
    setOnEditorActionListener(object : IntervalEditorActionListener() {

        override fun shouldDoAction(v: TextView, actionId: Int, event: KeyEvent?) = actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_NULL // именно такой id будет при хардварном Enter

        override fun doAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
            return nextView.requestFocus()
        }
    })
}

fun TextInputLayout.setCheckedError(@StringRes messageResId: Int) {
    val message =
            if (messageResId == 0)
                null
            else
                resources.getString(messageResId)
    setCheckedError(message)
}

fun TextInputLayout.setCheckedError(message: String?) {
    isErrorEnabled = message != null
    error = message
}

/**
 * Убрать нижнее подчеркивание для текущего text
 */
fun TextView.removeUnderline() {
    val s = SpannableString(text)
    val spans = s.getSpans(0, s.length, URLSpan::class.java)
    for (span in spans) {
        val start = s.getSpanStart(span)
        val end = s.getSpanEnd(span)
        s.removeSpan(span)
        s.setSpan(object : URLSpan(span.url) {

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, start, end, 0)
    }
    text = s
}

/**
 * Добавить кликабельную картинку вместо последнего символа в строке
 */
fun TextView.addClickableImageToEnd(@DrawableRes drawableResId: Int, clickFunc: () -> Unit = {}) {
    val text = text
    if (text.isNotEmpty()) {
        val s = SpannableString(text)
        val imageSpan = ImageSpan(context, drawableResId, ImageSpan.ALIGN_BASELINE)
        s.setSpan(imageSpan, s.length - 1, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                if (!SdkUtils.isPreLollipop()) {
                    widget.cancelPendingInputEvents()
                }
                clickFunc.invoke()
            }
        }, s.length - 1, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setText(s)
        movementMethod = LinkMovementMethod.getInstance()
    }
}

/**
 * Установить html текст в TextView
 */
fun TextView.setHtmlText(text: String) {
    try {
        this.text = text.parseHtmlToSpannedString()
    } catch (e: RuntimeException) {
        this.text = text
    }
}

fun TextView.setHtmlText(@StringRes resId: Int) =
        setHtmlText(resources.getString(resId))

/**
 * Установить ссылку из html-текста
 */
fun TextView.setLinkFromHtml(htmlLink: String, removeUnderlying: Boolean = true) {
    setHtmlText(htmlLink)
    if (removeUnderlying) {
        removeUnderline()
    }
    movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.setLinkFromHtml(@StringRes htmlLinkResId: Int, removeUnderlying: Boolean = true) =
        setLinkFromHtml(resources.getString(htmlLinkResId), removeUnderlying)

/**
 * Альтернатива [setLinkFromHtml], в котором в кач-ве [text]
 * вместо html-разметки обычный текст с кликабельной ссылкой
 * в указанном диапазоне
 */
fun TextView.setLinkableText(text: CharSequence,
                             startIndex: Int,
                             endIndex: Int,
                             removeUnderlying: Boolean,
                             link: String,
                             clickAction: (() -> Unit)? = null
) {

    setTextWithClickableSpan(text, startIndex, endIndex, removeUnderlying) {
        context.startActivity(getBrowseLinkIntent(link))
        clickAction?.invoke()
    }
}

/**
 * Установить кликабельный span
 * с кастомным действием при нажатии
 */
fun TextView.setTextWithClickableSpan(
        text: CharSequence,
        startIndex: Int,
        endIndex: Int,
        removeUnderlying: Boolean,
        clickAction: () -> Unit) {

    setText(SpannableStringBuilder(text).apply {
        setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                clickAction()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                if (removeUnderlying) {
                    ds.isUnderlineText = false
                }
            }
        }, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    })
    movementMethod = LinkMovementMethod.getInstance()
}

/**
 * Выставить [html] в кач-ве html текста, но для кликабельных сегментов оповещать о клике
 */
fun TextView.setHtmlTextWithCustomClick(html: String, removeUnderlying: Boolean = true, action: ((URLSpan) -> Unit)? = null) {
    val sequence = html.parseHtmlToSpannedString()
    val strBuilder = SpannableStringBuilder(sequence)
    val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
    urls.forEach { span ->
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        strBuilder.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                context.startActivity(getBrowseLinkIntent(span.url))
                action?.invoke(span)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                if (removeUnderlying) {
                    ds.isUnderlineText = false
                }
            }
        }, start, end, flags)
        strBuilder.removeSpan(span)
    }
    text = strBuilder
    movementMethod = LinkMovementMethod.getInstance()
}

/**
 * Установить текст с выделенным текстом
 * @param highlightColor цвет выделенного текста
 * @param str текст
 * @param selection текст для выделения (ищется первое вхождение [selection] в [str]
 */
fun TextView.setTextWithSelection(@ColorInt highlightColor: Int, str: String, selection: String) {
    text = SpannableString(str)
            .apply {
                val start = str.indexOf(selection, ignoreCase = true)
                        .takeIf { it >= 0 }
                        ?: return@apply
                setSpan(
                        ForegroundColorSpan(highlightColor),
                        start,
                        start + selection.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setDefaultStatusBarColor() {
    setStatusBarColor(
            window.context.getColorForAttrs(intArrayOf(R.attr.colorPrimaryDark))
    )
}

/**
 * Красит статус бар в указанный цвет.
 * Так же, красит иконки статус бара в серый цвет, если SDK >= 23 и устанавливаемый цвет слишком белый
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setStatusBarColor(@ColorInt color: Int) {
    if (SdkUtils.isAtLeastLollipop()) {
        window.statusBarColor = color
        if (SdkUtils.isAtLeastMarshmallow()) {
            // если цвет слишком белый, то красим иконки statusbar'а в серый цвет
            // инчае возвращаем к дефолтному белому
            if (ColorUtils.calculateLuminance(color).compareTo(DEFAULT_DARK_COLOR_RATIO) != -1) {
                var flags = window.decorView.systemUiVisibility
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.decorView.systemUiVisibility = flags
            } else {
                window.decorView.systemUiVisibility = 0
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setDefaultNavigationColor() {
    setNavigationBarColor(
            window.context.getColorForAttrs(intArrayOf(R.attr.colorPrimaryDark))
    )
}

/**
 * Красит нав бар в указанный цвет
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setNavigationBarColor(@ColorInt color: Int) {
    if (SdkUtils.isAtLeastLollipop()) {
        window.navigationBarColor = color
    }
}

/**
 * Выставить [icon] с фильтром [color] в кач-ве background для [View]
 */
@SuppressLint("ResourceType")
fun View.setBackgroundTint(@DrawableRes icon: Int, @ColorInt color: Int, mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN): Drawable? {
    with(context.resources.getColorFilteredDrawable(icon, color, mode)) {
        background = this
        return this
    }
}

/**
 * Выставить [icon] с фильтром [color] в кач-ве src для [ImageView]
 */
@SuppressLint("ResourceType")
fun ImageView.setImageTint(@DrawableRes icon: Int, @ColorInt color: Int, mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN): Drawable? {
    with(context.resources.getColorFilteredDrawable(icon, color, mode)) {
        setImageDrawable(this)
        return this
    }
}

/**
 * Выставить фильтр с [ColorStateList], полученным по [resourceId]
 */
fun ImageView.setTintResource(resourceId: Int) {
    setTint(ContextCompat.getColorStateList(context, resourceId))
}

/**
 * Выставить фильтр с цветом [color] для [ImageView]
 */
fun ImageView.setTint(@ColorInt color: Int) {
    setTint(ColorStateList.valueOf(color))
}

/**
 * Выставить цветовой фильтр [ColorStateList] для src в [ImageView]
 */
fun ImageView.setTint(colorStateList: ColorStateList?,
                      mode: PorterDuff.Mode = PorterDuff.Mode.SRC_ATOP) {
    if (SdkUtils.isAtLeastLollipop()) {
        imageTintList = colorStateList
        imageTintMode = mode
    } else {
        setColorFilter(colorStateList?.defaultColor ?: Color.TRANSPARENT, mode)
    }
}

/**
 * Выставить цветовой фильтр [ColorStateList] для [Drawable]
 */
fun Drawable.setColor(color: ColorStateList,
             mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN) {
    if (SdkUtils.isAtLeastLollipop()) {
        DrawableCompat.setTintList(this, color)
        DrawableCompat.setTintMode(this, mode)
    } else {
        setColorFilter(color.defaultColor, mode)
    }
}

/**
 * Запросить фокус или показать клавиатуру в зав-ти от состояния view
 */
fun View.toggleFocusOrKeyboardState(toggle: Boolean = true, activity: Activity) {
    if (toggle) {
        if (isFocused.not()) {
            requestFocus()
        } else {
            showKeyboard(this)
        }
    } else {
        hideKeyboard(activity)
        // очистка фокуса не убирает клавиатуру
        clearFocus()
    }
}

/**
 * Выполнить [action] с проверкой
 * во избежание [IllegalStateException]
 */
fun RecyclerView.doActionWithComputingCheck(action: (() -> Unit)) {
    if (!isComputingLayout) {
        action()
    } else {
        post {
            action()
        }
    }
}

/**
 * Установка лимита на количество символов допустимых к набору в [EditText]
 *
 * @param length целочисленное значение, соответствующее максимальному
 * количеству символов, допустимых к набору в [EditText].
 */
fun EditText.setMaxLength(length: Int) {
    if (length >= 0) {
        val inputTextFilter = InputFilter.LengthFilter(length)

        filters = filters
                .filterNot { it is InputFilter.LengthFilter }
                .toTypedArray()
                .plus(inputTextFilter)
    }
}

/**
 * Снятие лимита на количество символов допустимых к набору в [EditText]
 */
fun EditText.clearMaxLength() {
    filters = filters
            .filterNot { it is InputFilter.LengthFilter }
            .toTypedArray()
}

/**
 * Показать тост с длительностью, отличающейся от стандартных
 * [Toast.LENGTH_SHORT] и [Toast.LENGTH_LONG]
 */
fun showWithDuration(targetDuration: Long, toastFunc: (() -> Toast)): CountDownTimer {
    if (targetDuration <= 0) {
        throw IllegalArgumentException("Incorrect targetDuration: $targetDuration")
    }
    return object: CountDownTimer(targetDuration, TimeUnit.SECONDS.toMillis(1)) {

        private var previousToast: Toast? = null

        override fun onFinish() {
            show()
        }

        override fun onTick(millisUntilFinished: Long) {
            show()
        }

        private fun show() {
            previousToast?.cancel()
            with (toastFunc()) {
                // меняем на 1 секунду для правильного интервала таймера
                duration = Toast.LENGTH_SHORT
                previousToast = this
                show()
            }
        }
    }.start()
}