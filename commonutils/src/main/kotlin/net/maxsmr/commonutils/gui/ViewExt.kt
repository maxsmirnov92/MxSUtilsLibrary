package net.maxsmr.commonutils.gui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.URLSpan
import android.util.Base64
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.gui.listeners.DefaultTextWatcher
import net.maxsmr.commonutils.gui.listeners.OnTextWatcher
import net.maxsmr.commonutils.live.setValueIfNew
import net.maxsmr.commonutils.media.toBase64
import net.maxsmr.commonutils.format.getFormattedText
import net.maxsmr.commonutils.format.getUnformattedText
import net.maxsmr.commonutils.graphic.scaleDownBitmap
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.number.isZero
import net.maxsmr.commonutils.text.*
import ru.tinkoff.decoro.Mask
import ru.tinkoff.decoro.watchers.MaskFormatWatcher
import java.nio.charset.Charset

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("ViewExt")

private const val IMAGE_VIEW_MAX_WIDTH = 4096

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

/**
 * Запрещает ввод текста в EditText и убирает нижнюю полосу. Также в случае отсутствия введенного текста
 * пустую строку заменяет на пробел, чтобы когда EditText внутри TextInputLayout подсказка оставалась на месте.
 * Удобно использовать, когда рядом с readOnly полем есть изменяемые поля, чтобы выглядело одинаково.
 */
fun TextView.setReadOnly() {
    background = null
    isFocusable = false
    isClickable = false
    isFocusableInTouchMode = false
    addInputFilters(InputFilter { source, start, end, dest, dstart, dend ->
        val target = dest.replaceRange(dstart, dend, source.substring(start, end))
        if (target.isEmpty()) " " else null
    })
    //триггерим срабатываение InputFilter
    text = text
}

/**
 * Добавляет фильтры ввода к EditText
 *
 * @param filter Массив InputFilter
 */
fun TextView.addInputFilters(vararg filter: InputFilter) {
    val editFilters = filters
    val newFilters = arrayOfNulls<InputFilter>(editFilters.size + filter.size)
    System.arraycopy(editFilters, 0, newFilters, 0, editFilters.size)
    System.arraycopy(filter, 0, newFilters, editFilters.size, newFilters.size - editFilters.size)
    filters = newFilters
}

/**
 * Устанавливает курсор в конец строки
 */
fun EditText.setSelectionToEnd() {
    if (text.isNotEmpty()) {
        setSelection(text.length)
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

fun EditText.bindToTextUnformatted(field: MutableLiveData<String>, maskWatcher: MaskFormatWatcher) {
    bindToTextUnformatted(field, maskWatcher.maskOriginal)
}

fun EditText.bindToTextUnformatted(field: MutableLiveData<String>, mask: Mask) {
    bindToText(field) {
        getUnformattedText(mask, it)
        // или maskWatcher.mask.toUnformattedString()
    }
}

@JvmOverloads
fun TextView.bindToText(
        field: MutableLiveData<String>,
        ifNew: Boolean = true,
        toFieldValue: ((CharSequence?) -> String)? = null
) {
    bindTo(field, ifNew) {
        toFieldValue?.invoke(it) ?: it?.toString().orEmpty()
    }
}

/**
 * При срабатывании [TextWatcher] для укзанного [EditText]
 * будет выставляться значение в [field]
 */
@JvmOverloads
fun <D> TextView.bindTo(
        field: MutableLiveData<D>,
        ifNew: Boolean = true,
        toFieldValue: (CharSequence?) -> D
) {
    addTextChangedListener(OnTextWatcher { s: CharSequence?, _: Int, _: Int, _: Int ->
        val newValue = toFieldValue.invoke(s)
        if (ifNew) {
            field.setValueIfNew(newValue)
        } else {
            field.value = newValue
        }
    })
}

/**
 * Обозревание форматированного или неформатированного (второй вариант более правильный)
 * из [MutableLiveData] с целью выставления в целевую [view] форматированного значения методом refreshMask
 * @param maskWatcher уже привязанный к [view]
 */
@JvmOverloads
fun MutableLiveData<String>.observeFromTextUnformatted(
        view: TextView,
        viewLifecycleOwner: LifecycleOwner,
        maskWatcher: MaskFormatWatcher,
        onChanged: ((CharSequence?) -> Unit)? = null
) {
    observe(viewLifecycleOwner) {
        if (it != null && it.isNotEmpty()) {
            maskWatcher.refreshMask(it)
        } else {
            view.text = EMPTY_STRING
        }
        onChanged?.invoke(it)
    }
}

@JvmOverloads
fun MutableLiveData<String>.observeFromTextUnformatted(
        view: TextView,
        viewLifecycleOwner: LifecycleOwner,
        mask: Mask,
        distinct: Boolean = true,
        onChanged: ((CharSequence?) -> Unit)? = null
) {
    observeFromText(view, viewLifecycleOwner, distinct) {
        onChanged?.invoke(it)
        getFormattedText(mask, it)
    }
}

/**
 * Обозревание форматированного или неформатированного (второй вариант более правильный)
 * из [Field] с целью выставления в целевую [view] форматированного значения через [formatFunc]
 */
fun MutableLiveData<String>.observeFromText(
        view: TextView,
        viewLifecycleOwner: LifecycleOwner,
        distinct: Boolean = true,
        asString: Boolean = true,
        formatFunc: ((CharSequence?) -> CharSequence)? = null
) {
    observeFrom(view, viewLifecycleOwner, distinct, asString) {
        formatFunc?.invoke(it) ?: it.toString()
    }
}

@JvmOverloads
fun <D> MutableLiveData<D>.observeFrom(
        view: TextView,
        viewLifecycleOwner: LifecycleOwner,
        distinct: Boolean = true,
        asString: Boolean = true,
        formatFunc: (D?) -> CharSequence?
) {
    observe(viewLifecycleOwner) {
        view.setTextChecked(formatFunc(it), distinct, asString)
    }
}

@JvmOverloads
fun CompoundButton.bindTo(field: MutableLiveData<Boolean>, ifNew: Boolean = true) {
    setOnCheckedChangeListener { _, isChecked ->
        if (ifNew) {
            field.setValueIfNew(isChecked)
        } else {
            field.value = isChecked
        }
    }
}

/**
 * Добавить кликабельную картинку вместо последнего символа в строке
 */
@JvmOverloads
fun TextView.appendClickableImageTextView(
        @DrawableRes drawableResId: Int,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        clickFunc: () -> Unit = {}
): CharSequence =
        text.appendClickableImage(context, drawableResId, spanFlags, clickFunc)


/**
 * Убрать нижнее подчеркивание для текущего text
 */
fun TextView.resetNotUnderlinedUrlSpans(): CharSequence =
        setTextWithMovementMethod(text.removeUnderlineFromUrlSpans(false))

@JvmOverloads
fun TextView.setHtmlText(@StringRes resId: Int, clearHtml: Boolean = false) =
        setHtmlText(resources.getString(resId), clearHtml)

/**
 * Установить html текст в TextView
 */
@JvmOverloads
fun TextView.setHtmlText(text: CharSequence?, clearHtml: Boolean = false): CharSequence =
        setTextWithMovementMethod(
                if (clearHtml) {
                    text.parseClearedHtml()
                } else {
                    text.parseHtmlToSpannedString()
                }
        )

@JvmOverloads
fun TextView.setLinkFromHtml(@StringRes htmlLinkResId: Int, removeUnderline: Boolean = true) =
        setLinkFromHtml(resources.getString(htmlLinkResId), removeUnderline)

/**
 * Установить ссылку из html-текста
 */
@JvmOverloads
fun TextView.setLinkFromHtml(htmlLink: String, removeUnderline: Boolean = true): CharSequence = if (removeUnderline) {
    setTextWithMovementMethod(htmlLink.removeUnderlineFromUrlSpans(true))
} else {
    setHtmlText(htmlLink)
}

/**
 * Установить [text] с заданными настройками [spanInfo]
 */
fun TextView.setSpanText(
        text: CharSequence,
        vararg spanInfo: IRangeSpanInfo
): CharSequence =
        setTextWithMovementMethod(text.createSpanText(*spanInfo))

/**
 * @param text в строке аргументы с префиксами "^" будут заменены на [CharacterStyle]
 * @param spanInfoMap маппинг информации о [Spannable] + link для перехода по клику по нему
 * (range в каждом [ISpanInfo] задействован не будет)
 */
fun TextView.setSpanTextExpanded(
        text: CharSequence,
        spanInfoMap: Map<ISpanInfo, String>
): CharSequence =
        setTextWithMovementMethod(text.createSpanTextExpanded(spanInfoMap))

/**
 * Альтернатива [setLinkFromHtml], в котором в кач-ве [text]
 * вместо html-разметки обычный текст с кликабельными ссылкоми
 * в указанных диапазонах
 * @param spanInfoMap маппинг информации о [Spannable] + link для перехода по клику по нему
 */
fun TextView.setLinkableText(
        text: CharSequence,
        spanInfoMap: Map<IRangeSpanInfo, String>
): CharSequence =
        setTextWithMovementMethod(text.createLinkableText(spanInfoMap))

fun TextView.setLinkableTextExpanded(
        text: CharSequence,
        spanInfoMap: Map<ISpanInfo, ExpandValueInfo>,
): CharSequence =
        setTextWithMovementMethod(text.createLinkableTextExpanded(spanInfoMap))

/**
 * Выставить [html] в кач-ве html текста, но для кликабельных сегментов оповещать о клике
 */
@JvmOverloads
fun TextView.replaceUrlSpans(
        html: String,
        parseHtml: Boolean,
        isUnderlineText: Boolean = false,
        action: ((URLSpan) -> Boolean)? = null
): CharSequence = setTextWithMovementMethod(
        html.replaceUrlSpansByClickableSpans(parseHtml, isUnderlineText, action)
)

/**
 * Установить текст с выделенным текстом
 * @param highlightColor цвет выделенного текста
 * @param str текст
 * @param selection текст для выделения (ищется первое вхождение [selection] в [str]
 */
@JvmOverloads
fun TextView.setTextWithSelection(
        text: String,
        @ColorInt highlightColor: Int,
        selection: String,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
) {
    setTextWithMovementMethod(text.createSelectedText(highlightColor, selection, spanFlags))
}

@JvmOverloads
fun TextView.setTextOrHide(
        @StringRes textResId: Int?,
        isGoneOrInvisible: Boolean = true,
        distinct: Boolean = true,
        asString: Boolean = true,
        isEmptyFunc: (CharSequence?) -> Boolean = { isEmpty(it) },
        showFunc: (() -> Unit)? = null,
        hideFunc: (() -> Unit)? = null
): Boolean =
        setTextOrHide(
                textResId?.takeIf { it != 0 }?.let { context.getString(it) },
                isGoneOrInvisible,
                distinct,
                asString,
                isEmptyFunc,
                showFunc,
                hideFunc
        )

/**
 * @return true, если текст был выставлен
 */
@JvmOverloads
fun TextView.setTextOrHide(
        text: CharSequence?,
        isGoneOrInvisible: Boolean = true,
        distinct: Boolean = true,
        asString: Boolean = true,
        isEmptyFunc: (CharSequence?) -> Boolean = { isEmpty(it) },
        showFunc: (() -> Unit)? = null,
        hideFunc: (() -> Unit)? = null
): Boolean {
    if (isEmptyFunc(text)) {
        this.text = EMPTY_STRING
        if (hideFunc != null) {
            hideFunc.invoke()
        } else {
            setVisible(false, isGoneOrInvisible)
        }
        return false
    } else {
        setTextChecked(text, distinct, asString)
        if (showFunc != null) {
            showFunc.invoke()
        } else {
            setVisible(true)
        }
        return true
    }
}

@JvmOverloads
fun TextView.setSumOrHide(
        sum: Double,
        isGoneOrInvisible: Boolean = true,
        distinct: Boolean = true,
        asString: Boolean = true,
        formatFunc: ((Double) -> CharSequence)? = null,
        showFunc: (() -> Unit)? = null,
        hideFunc: (() -> Unit)? = null
): Boolean = if (!sum.isZero()) {
        setTextOrHide(
                formatFunc?.let { it(sum) } ?: sum.toString(),
                isGoneOrInvisible,
                distinct,
                asString,
                showFunc = showFunc,
                hideFunc = hideFunc
        )
    } else {
        setTextOrHide(null as String?, isGoneOrInvisible,
                distinct,
                false,
                showFunc = null,
                hideFunc = hideFunc)
    }

fun ImageView.setImageResourceOrHide(@DrawableRes iconResId: Int?): Boolean = if (iconResId == null || iconResId == 0) {
        isVisible = false
        false
    } else {
        setImageResource(iconResId)
        isVisible = true
        true
    }

@JvmOverloads
fun EditText.setTextWithSelectionToEnd(
        text: CharSequence,
        distinct: Boolean = true,
        asString: Boolean = true
) {
    setTextChecked(text, distinct, asString)
    // после возможных фильтров текст мог измениться
    setSelectionToEnd()
}

fun TextInputLayout.setInputErrorTextColor(color: Int) {
    try {
        val view = ReflectionUtils.getFieldValue<TextView, TextInputLayout>(TextInputLayout::class.java, this, "mErrorView")
        view?.let {
            it.setTextColor(color)
            it.requestLayout()
        }
    } catch (e: Exception) {
        logger.e("An exception occurred: ${e.message}", e)
    }
}

@JvmOverloads
fun TextInputLayout.setInputError(
        @StringRes messageResId: Int?,
        errorEnabledByDefault: Boolean = false,
        requestFocusIfError: Boolean = true,
        distinct: Boolean = true
) {
    val message = if (messageResId == null || messageResId == 0) {
        null
    } else {
        resources.getString(messageResId)
    }
    setInputError(message, errorEnabledByDefault, requestFocusIfError, distinct)
}

@JvmOverloads
fun TextInputLayout.setInputError(
        message: CharSequence?,
        errorEnabledByDefault: Boolean = false,
        requestFocusIfError: Boolean = true,
        distinct: Boolean = true
) {
    if (!distinct || error != message) {
        val isErrorEnabled = errorEnabledByDefault || !isEmpty(message)
        error = message
        this.isErrorEnabled = isErrorEnabled
        refreshDrawableState()
        if (requestFocusIfError && isErrorEnabled) {
            requestFocusWithCheck()
        }
    }
}

fun TextInputLayout.clearInputError(force: Boolean = false) {
    if (force || error != null) {
        error = null
        isErrorEnabled = false
        refreshDrawableState()
    }
}

fun TextInputLayout.setEditTextHintByError(hint: String) {
    editText?.let {
        it.hint = if (isEmpty(this.error)) null else hint
    }
}

/**
 * Меняет цвет иконок статусбара на темный/светлый
 *
 * @param v      корневая [View] лейаута
 * @param isLight true тёмные icons, false - светлые
 */
fun View.setStatusBarLightColor(isLight: Boolean) {
    if (isAtLeastMarshmallow()) {
        try {
            var flags = systemUiVisibility
            flags = if (isLight) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            systemUiVisibility = flags
        } catch (e: Exception) {
            logger.e("An exception occurred: ${e.message}", e)
        }
    }
}

fun View.setBackgroundTint(@ColorRes tintResId: Int) {
    ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, tintResId)))
}

/**
 * Выставить [iconResId] с фильтром [colorResId] в кач-ве background для [View]
 */
@JvmOverloads
@SuppressLint("ResourceType")
fun View.setBackgroundTint(
        @DrawableRes iconResId: Int,
        @ColorInt colorResId: Int,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    with(getColoredDrawable(context.resources, iconResId, colorResId, mode)) {
        background = this
        return this
    }
}

/**
 * Выставить [icon] с фильтром [color] в кач-ве src для [ImageView]
 */
@JvmOverloads
fun ImageView.setTint(
        @DrawableRes icon: Int,
        @ColorInt color: Int,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    with(getColoredDrawable(context.resources, icon, color, mode)) {
        setImageDrawable(this)
        return this
    }
}

/**
 * Выставить фильтр с [ColorStateList], полученным по [colorResId]
 */
fun ImageView.setTintResource(@ColorRes colorResId: Int) {
    setTint(ContextCompat.getColorStateList(context, colorResId))
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
@JvmOverloads
fun ImageView.setTint(
        colorStateList: ColorStateList?,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_ATOP
) {
    if (isAtLeastLollipop()) {
        imageTintList = colorStateList
        imageTintMode = mode
    } else {
        setColorFilter(colorStateList?.defaultColor ?: Color.TRANSPARENT, mode)
    }
}

fun ImageView.getContentSize(): Pair<Int, Int> =
        drawable?.let { Pair(it.intrinsicWidth, it.intrinsicHeight) } ?: Pair(0, 0)

fun ImageView.getRescaledImageViewSize(): Pair<Int?, Int?> {
    var measuredWidth: Int
    var measuredHeight: Int
    val intrinsicHeight: Int
    val intrinsicWidth: Int
    measuredWidth = this.measuredWidth //width of imageView
    measuredHeight = this.measuredHeight //height of imageView
    val size = getContentSize()
    intrinsicWidth = size.first //original width of underlying image
    intrinsicHeight = size.second //original height of underlying image
    if (intrinsicHeight > 0 && intrinsicWidth > 0) {
        if (measuredHeight / intrinsicHeight <= measuredWidth / intrinsicWidth) {
            measuredWidth = intrinsicWidth * measuredHeight / intrinsicHeight
        } //rescaled width of image within ImageView
        else {
            measuredHeight = intrinsicHeight * measuredWidth / intrinsicWidth
        } //rescaled height of image within ImageView;
    }
    return Pair(measuredWidth, measuredHeight)
}

fun View.getViewInset(): Int {
    with(this.context.resources) {
        val statusBarHeight = getStatusBarHeight()
        if (statusBarHeight < 0) {
            return 0
        }
        val dm = displayMetrics
        if (isPreLollipop() || this@getViewInset.height == dm.heightPixels || this@getViewInset.height == dm.heightPixels - statusBarHeight) {
            return 0
        }
        try {
            val infoField = View::class.java.getDeclaredField("mAttachInfo")
            infoField.isAccessible = true
            val info = infoField[this@getViewInset]
            if (info != null) {
                val insetsField = info.javaClass.getDeclaredField("mStableInsets")
                insetsField.isAccessible = true
                val insets = insetsField[info] as Rect
                return insets.bottom
            }
        } catch (e: Exception) {
            logger.e("An exception occurred: ${e.message}", e)
        }
    }
    return 0
}

/**
 * @return true if focused, false otherwise
 */
fun View?.requestFocusWithCheck(): Boolean {
    if (this != null) {
        if (this.isFocusable) {
//            act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            if (this.requestFocus()) {
                return true
            }
        }
    }
    return false
}

/**
 * @return true if focus cleared, false otherwise
 */
fun View?.clearFocusWithCheck(): Boolean {
    if (this != null) {
        this.clearFocus()
//        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        return true
    }
    return false
}

/**
 * Выполнить [action] с проверкой
 * во избежание [IllegalStateException]
 */
fun RecyclerView.runAction(action: (() -> Unit)) {
    if (!isComputingLayout) {
        action()
    } else {
        post {
            action()
        }
    }
}

fun CoordinatorLayout.collapseToolbar(coordinatorChild: View, appBarLayout: AppBarLayout) {
    var found = false
    for (i in 0 until childCount) {
        if (getChildAt(i) === coordinatorChild) {
            found = true
        }
    }
    require(found) { "View $coordinatorChild is not a child of $this" }
    val params = coordinatorChild.layoutParams as CoordinatorLayout.LayoutParams
    val behavior = params.behavior as AppBarLayout.ScrollingViewBehavior?
    behavior?.onNestedFling(this, appBarLayout, coordinatorChild, 0f, 10000f, true)
}

fun BottomSheetDialog.setHideable(toggle: Boolean) {
    val behavior: BottomSheetBehavior<*>? = ReflectionUtils.getFieldValue<BottomSheetBehavior<*>, BottomSheetDialog>(BottomSheetDialog::class.java, this, "mBehavior")
    behavior?.let {
        it.isHideable = toggle
    }
}

fun View.getSize(): Pair<Int, Int> {
    layoutParams.let {
        return Pair(it.width, it.height)
    }
}

fun View.setSize(size: Pair<Int, Int>) {
    require(!(size.first < -1 || size.second < -1)) { "incorrect view size: " + size.first + "x" + size.second }
    val layoutParams = this.layoutParams
    layoutParams.width = size.first
    layoutParams.height = size.second
    this.layoutParams = layoutParams
}

fun View.setAllPadding(paddingPx: Int) {
    setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
}

@JvmOverloads
fun View.setPadding(startPx: Int? = null, topPx: Int? = null, endPx: Int? = null, bottomPx: Int? = null) {
    setPadding(startPx ?: paddingStart, topPx ?: paddingTop, endPx ?: paddingEnd,
            bottomPx ?: paddingBottom)
}

fun RadioGroup.getSelectedIndex(): Int {
    val radioButtonId = checkedRadioButtonId
    val radioButton = findViewById<RadioButton>(radioButtonId) ?: null
    return radioButton?.let { indexOfChild(it) } ?: RecyclerView.NO_POSITION
}

@JvmOverloads
fun TextView.calculateMaxTextSize(
        maxWidthTextLength: Int,
        maxTextSize: Float,
        maxTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
        measuredViewWidth: Int = width
): Float {
    val text = StringBuilder()
    for (i in 0 until maxWidthTextLength) {
        text.append("0")
    }
    return calculateMaxTextSize(text.toString(), maxTextSize, maxTextSizeUnit, measuredViewWidth)
}

@JvmOverloads
fun TextView.calculateMaxTextSize(
        maxWidthText: CharSequence,
        maxTextSize: Float,
        maxTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
        measuredViewWidth: Int = width
): Float {
    if (maxWidthText.isEmpty() || maxTextSize <= 0) return maxTextSize
    if (measuredViewWidth <= 0) return maxTextSize

    val maxWidthString = maxWidthText.toString()
    var resultSize = maxTextSize

    val paint = Paint()
    paint.typeface = typeface

    // применяем в абсолютных пикселях
    paint.textSize = TypedValue.applyDimension(maxTextSizeUnit, resultSize, context.resources.displayMetrics)
    var measuredText = paint.measureText(maxWidthString)

    while (resultSize > 0 && measuredText > measuredViewWidth) {
        // убавляем в исходной размерности
        resultSize--
        paint.textSize = TypedValue.applyDimension(maxTextSizeUnit, resultSize, context.resources.displayMetrics)
        measuredText = paint.measureText(maxWidthString)
    }
    if (resultSize == 0f) {
        resultSize = maxTextSize
    }
    return resultSize
}

/**
 * Урезание текста по ширине view:
 * вычитание кол-ва [reduceStepCount] с конца до тех пор,
 * пока измеренный текущий текст не влезет в [measuredWidth]
 */
@JvmOverloads
fun TextView.reduceTextByWidth(
        sourceText: CharSequence = text,
        measuredWidth: Int = width,
        textSize: Float,
        textSizeUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
        postfix: String = "...",
        reduceStepCount: Int = postfix.length,
        applyText: Boolean = true

): CharSequence {
    if (textSize <= 0
            || reduceStepCount <= 0) {
        return sourceText
    }
    if (measuredWidth <= 0) return sourceText

    val paint = Paint()
    paint.typeface = typeface

    // применяем в абсолютных пикселях
    paint.textSize = TypedValue.applyDimension(textSizeUnit, textSize, context.resources.displayMetrics)

    val resultString = reduceTextByMaxWidth(
            paint,
            sourceText,
            postfix,
            reduceStepCount,
            measuredWidth
    )
    if (applyText) {
        text = resultString
    }
    return resultString
}

/**
 * Урезание текста с учётом ограничений
 * по высоте view и ширины одной строки в ней
 * @param startHeightIndex стартовый индекс символа из диапазона, по которому ведётся расчёт высоты
 */
@JvmOverloads
fun TextView.reduceTextByHeight(
        sourceText: CharSequence = text,
        measuredWidth: Int = width,
        measuredHeight: Int = height,
        startHeightIndex: Int = 0,
        endHeightIndex: Int = sourceText.length,
        textSize: Float,
        textSizeUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
        postfix: String = "...",
        reduceStepCount: Int = postfix.length,
        applyText: Boolean = true

): CharSequence {
    if (textSize <= 0
            || reduceStepCount <= 0) {
        return sourceText
    }
    if (measuredWidth <= 0
            || measuredHeight <= 0) {
        return sourceText
    }

    if (startHeightIndex < 0 || startHeightIndex >= sourceText.length
            || endHeightIndex < 0 || endHeightIndex >= sourceText.length) {
        return sourceText
    }

    val paint = Paint()
    paint.typeface = typeface

    val displayMetrics = context.resources.displayMetrics
    // применяем в абсолютных пикселях
    paint.textSize = TypedValue.applyDimension(textSizeUnit, textSize, displayMetrics)

    val rect = Rect()
    paint.getTextBounds(sourceText.toString(), startHeightIndex, endHeightIndex, rect)
    // фиксированная высота одной строки (размер шрифта текста не меняется)
    val lineHeight = (rect.height() / displayMetrics.density).toInt()
    // кол-во линий, которые могут вместиться в измеренную, ранее известную, высоту
    val linesCount = measuredHeight / lineHeight

    // предельно допустимая ширина текста, с которой будет сравниваться ширина изменяемого текста
    val maxTextWidth = measuredWidth * linesCount

    val resultString = reduceTextByMaxWidth(
            paint,
            sourceText,
            postfix,
            reduceStepCount,
            maxTextWidth
    )
    if (applyText) {
        text = resultString
    }
    return resultString
}

private fun reduceTextByMaxWidth(
        paint: Paint,
        sourceText: CharSequence,
        postfix: String,
        reduceStepCount: Int,
        maxTextWidth: Int
): CharSequence {

    var measuredTextWidth = paint.measureText(sourceText.toString())

    // общее кол-во вычтенных символов
    var totalReducedCount = 0

    var isLastPostfixAppended = false

    val resultString = StringBuilder()
    resultString.append(sourceText)

    // вычитание n-ого кол-ва сиволов с конца ещё возможно + не проходим по основному критерию:
    // ширина текущего текста больше допустимой
    while (resultString.length > reduceStepCount && measuredTextWidth > maxTextWidth) {
        val previousString = resultString.toString()
        resultString.clear()
        var diff: Int = reduceStepCount
        if (isLastPostfixAppended) {
            diff += postfix.length
        }
        resultString.append(previousString.substring(0, previousString.length - diff))
        totalReducedCount += reduceStepCount
        isLastPostfixAppended = if (postfix.isNotEmpty() && totalReducedCount > postfix.length) {
            resultString.append(postfix)
            true
        } else {
            false
        }
        measuredTextWidth = paint.measureText(resultString.toString())
    }
    return resultString
}

/**
 * @return relative coordinates to the parent
 */
fun View.getBoundsByParent(parent: ViewGroup): Rect {
    val offsetViewBounds = Rect()
    getDrawingRect(offsetViewBounds)
    parent.offsetDescendantRectToMyCoords(this, offsetViewBounds)
    return offsetViewBounds
}

@JvmOverloads
fun TextView.setTextDistinctFormatted(
        text: CharSequence?,
        maskWatcher: MaskFormatWatcher,
        asString: Boolean = true
) = setTextDistinctFormatted(text, maskWatcher.maskOriginal, asString)

@JvmOverloads
fun TextView.setTextDistinctFormatted(
        text: CharSequence?,
        mask: Mask,
        asString: Boolean = true
) = setTextDistinct(getFormattedText(mask, text), asString)

@JvmOverloads
fun TextView.setTextDistinct(text: CharSequence?, asString: Boolean = true): Boolean {
    if (if (asString) this.text?.toString() != text.toString() else this.text != text) {
        this.text = text
        return true
    }
    return false
}

@JvmOverloads
fun TextView.setTextChecked(
        text: CharSequence?,
        distinct: Boolean = true,
        asString: Boolean = true
) {
    if (!distinct) {
        this.text = text
    } else {
        setTextDistinct(text, asString)
    }
}

@JvmOverloads
fun View.setVisible(isVisible: Boolean, isGoneOrInvisible: Boolean = true) {
    visibility = if (isVisible) {
        View.VISIBLE
    } else {
        if (isGoneOrInvisible) View.GONE else View.INVISIBLE
    }
}

fun View.hideByReferenceViews(vararg otherViews: View) {
    val isGone = otherViews.all { it.visibility == View.GONE }
    visibility = if (isGone) View.GONE else View.INVISIBLE
}

private fun TextView.setTextWithMovementMethod(text: CharSequence?): CharSequence {
    this.text = text
    movementMethod = LinkMovementMethod.getInstance()
    return text ?: EMPTY_STRING
}

/**
 * Возвращает текущие экранные координаты View
 */
fun View.screenLocation(): Rect {
    return IntArray(2).let {
        getLocationOnScreen(it)
        Rect().apply {
            left = it[0]
            top = it[1]
            right = left + width
            bottom = top + height
        }
    }
}

@JvmOverloads
fun WebView.loadDataBase64(value: String, charset: Charset = Charsets.UTF_8) {
    loadData(value.toBase64(charset, Base64.DEFAULT), "text/html; charset=${charset}", "base64")
}

@JvmOverloads
fun TextView.observePlaceholderOrLabelHint(
        inputLayout: TextInputLayout?,
        @StringRes placeholderTextResId: Int,
        @StringRes labelTextResId: Int,
        setHintFunc: ((CharSequence) -> Unit)? = null,
        isForPlaceholderFunc: ((CharSequence?) -> Boolean)? = null
) = observePlaceholderOrLabelHint(inputLayout, context.getString(placeholderTextResId), context.getString(labelTextResId), setHintFunc, isForPlaceholderFunc)

/**
 * Меняет в динамике hint в зав-ти от текста
 */
@JvmOverloads
fun TextView.observePlaceholderOrLabelHint(
        inputLayout: TextInputLayout?,
        placeholderText: CharSequence,
        labelText: CharSequence,
        setHintFunc: ((CharSequence) -> Unit)? = null,
        isForPlaceholderFunc: ((CharSequence?) -> Boolean)? = null
): TextWatcher {
    val listener = object : DefaultTextWatcher() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val text = text
            val hint = if (isForPlaceholderFunc?.invoke(text)
                            ?: TextUtils.isEmpty(text)) placeholderText else labelText
            if (setHintFunc != null) {
                setHintFunc.invoke(hint)
            } else {
                if (inputLayout != null) {
                    inputLayout.hint = hint
                } else {
                    setHint(hint)
                }
            }
        }
    }
    addTextChangedListener(listener)
    return listener
}

@JvmOverloads
fun TextView.setupPlaceholderOrLabelHint(
        inputLayout: TextInputLayout?,
        rule: RequiredFieldRule,
        @StringRes placeholderTextResId: Int,
        @StringRes labelTextResId: Int,
        setHintFunc: ((CharSequence) -> Unit)? = null,
        isForPlaceholderFunc: ((CharSequence?) -> Boolean)? = null
) = setupPlaceholderOrLabelHint(inputLayout, rule, context.getString(placeholderTextResId), context.getString(labelTextResId), setHintFunc, isForPlaceholderFunc)

/**
 * В зав-ти от правила [rule] выставляет hint разово или в динамике
 * @param setHintFunc кастомная функция для выставления подсказки
 */
@JvmOverloads
fun TextView.setupPlaceholderOrLabelHint(
        inputLayout: TextInputLayout?,
        rule: RequiredFieldRule,
        placeholderText: CharSequence,
        labelText: CharSequence,
        setHintFunc: ((CharSequence) -> Unit)? = null,
        isForPlaceholderFunc: ((CharSequence?) -> Boolean)? = null
) {
    val hint: CharSequence
    when (rule) {
        RequiredFieldRule.NON_REQUIRED -> hint = labelText
        RequiredFieldRule.REQUIRED_INIT -> hint = placeholderText
        else -> {
            val text = text
            hint = if (isForPlaceholderFunc?.invoke(text)
                            ?: TextUtils.isEmpty(text)) placeholderText else labelText
            observePlaceholderOrLabelHint(inputLayout, placeholderText, labelText, setHintFunc, isForPlaceholderFunc)
        }
    }
    if (setHintFunc != null) {
        setHintFunc.invoke(hint)
    } else {
        if (inputLayout != null) {
            inputLayout.hint = hint
        } else {
            setHint(hint)
        }
    }
}

@JvmOverloads
fun ImageView.setImageBitmapWithResize(
        bitmap: Bitmap?,
        maxSize: Int = IMAGE_VIEW_MAX_WIDTH
): Bitmap? {
    val resultBitmap = scaleDownBitmap(bitmap, maxSize, recycleSource = false) ?: bitmap
    setImageBitmap(resultBitmap)
    return resultBitmap
}

enum class RequiredFieldRule {

    /**
     * Поле не является обязательным, используется labelText
     */
    NON_REQUIRED,

    /**
     * Поле является обязательным, но выставление (placeholder/label) происходит только при инициализации, не зависит от текста
     */
    REQUIRED_INIT,

    /**
     * Поле является обязательным, выставление происходит в зав-ти от текущего текста в поле
     */
    REQUIRED_DYNAMIC
}

class ProtectRangeInputFilter(private val startIndex: Int, private val endIndex: Int) : InputFilter {

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
        return if (dstart in startIndex..endIndex) {
            if (source.isEmpty()) dest.subSequence(dstart, dend) else ""
        } else {
            source
        }
    }
}

class DisableErrorTextWatcher(private val layouts: Collection<TextInputLayout>) : TextWatcher {

    var isClearingEnabled = true

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        for (l in layouts) {
            if (isClearingEnabled) {
                l.clearInputError()
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
}

class EditTextKeyLimiter(private val et: EditText, private val linesLimit: Int) : View.OnKeyListener {

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (et === v) { // if enter is pressed start calculating
            if (keyCode == KeyEvent.KEYCODE_ENTER
                    && event.action == KeyEvent.ACTION_UP) {
                val text = et.text.toString().trim { it <= ' ' }
                // find how many rows it cointains
                val editTextRowCount: Int = text.split(NEXT_LINE).toTypedArray().size
                // user has input more than limited - lets do something about that
                if (editTextRowCount >= linesLimit) { // find the last break
                    val lastBreakIndex = text.lastIndexOf("\n")
                    // compose new text
                    val newText = text.substring(0, lastBreakIndex)
                    // add new text - delete old one and append new one (append because I want the cursor to be at the end)
                    et.setText("")
                    et.append(newText)
                }
                return true
            }
        }
        return false
    }

    init {
        require(linesLimit > 0) { "incorrect linesLimit: $linesLimit" }
    }
}