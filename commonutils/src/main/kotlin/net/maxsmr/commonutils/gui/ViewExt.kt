package net.maxsmr.commonutils.gui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.URLSpan
import android.util.Base64
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.*
import androidx.annotation.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.ReflectionUtils.getFieldValue
import net.maxsmr.commonutils.format.getFormattedText
import net.maxsmr.commonutils.format.getUnformattedText
import net.maxsmr.commonutils.graphic.scaleDownBitmap
import net.maxsmr.commonutils.gui.listeners.AfterTextChangeListener
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.setValueIfNew
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.number.isZero
import net.maxsmr.commonutils.text.*
import ru.tinkoff.decoro.Mask
import ru.tinkoff.decoro.watchers.MaskFormatWatcher
import java.nio.charset.Charset

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("ViewExt")

private const val IMAGE_VIEW_MAX_WIDTH = 4096

private const val MIME_TYPE_WITH_CHARSET_FORMAT = "%s; charset=%s"

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
fun TextView.setMaxLength(length: Int) {
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
fun TextView.clearMaxLength() {
    filters = filters
        .filterNot { it is InputFilter.LengthFilter }
        .toTypedArray()
}

fun TextView.bindToTextUnformatted(data: MutableLiveData<String>, maskWatcher: MaskFormatWatcher) {
    bindToTextUnformatted(data, maskWatcher.maskOriginal)
}

fun TextView.bindToTextUnformatted(data: MutableLiveData<String>, mask: Mask) {
    bindToTextNotNull(data) {
        getUnformattedText(mask, it)
        // или maskWatcher.mask.toUnformattedString()
    }
}

@JvmOverloads
fun TextView.bindToText(
    data: MutableLiveData<String?>,
    ifNew: Boolean = true,
    toDataValue: ((CharSequence?) -> String?)? = null
) {
    bindTo(data, ifNew) {
        if (toDataValue != null) {
            toDataValue.invoke(it)
        } else {
            it?.toString()
        }
    }
}

@JvmOverloads
fun TextView.bindToTextNotNull(
    data: MutableLiveData<String>,
    ifNew: Boolean = true,
    toDataValue: ((CharSequence?) -> String)? = null
) {
    bindTo(data, ifNew) {
        toDataValue?.invoke(it) ?: it?.toString().orEmpty()
    }
}

/**
 * При срабатывании [TextWatcher] для указанного [EditText]
 * будет выставляться значение в [data]
 */
@JvmOverloads
fun <D> TextView.bindTo(
    data: MutableLiveData<D>,
    ifNew: Boolean = true,
    toDataValue: (CharSequence?) -> D
) {
    addTextChangedListener(AfterTextChangeListener { s: Editable ->
        val newValue = toDataValue.invoke(s)
        if (ifNew) {
            data.setValueIfNew(newValue)
        } else {
            data.value = newValue
        }
    })
}

/**
 * При срабатывании [TextWatcher] для указанного [EditText]
 * будет выставляться значение в [field]
 */
fun <D> TextView.bindTo(field: Field<D>, toFieldValue: ((CharSequence?) -> D)) {
    addTextChangedListener(AfterTextChangeListener { s: Editable ->
        field.value = toFieldValue.invoke(s)
    })
}


@JvmOverloads
fun CompoundButton.bindTo(data: MutableLiveData<Boolean>, ifNew: Boolean = true) {
    setOnCheckedChangeListener { _, isChecked ->
        if (ifNew) {
            data.setValueIfNew(isChecked)
        } else {
            data.value = isChecked
        }
    }
}

// region: Field

fun TextView.bindToTextUnformatted(field: Field<String>, maskWatcher: MaskFormatWatcher) {
    bindToTextUnformatted(field, maskWatcher.maskOriginal)
}

fun TextView.bindToTextUnformatted(field: Field<String>, mask: Mask) {
    bindToTextNotNull(field) {
        getUnformattedText(mask, it)
        // или maskWatcher.mask.toUnformattedString()
    }
}

/**
 * Для варианта [Field] с возможным нулабельным значением;
 * @param toFieldValue может вернуть null или отсутствовать
 */
@JvmOverloads
fun TextView.bindToText(
    field: Field<String?>,
    toFieldValue: ((CharSequence?) -> String?)? = null
) {
    bindTo(field) {
        if (toFieldValue != null) {
            toFieldValue.invoke(it)
        } else {
            it?.toString()
        }
    }
}

@JvmOverloads
fun TextView.bindToTextNotNull(
    field: Field<String>,
    toFieldValue: ((CharSequence?) -> String)? = null
) {
    bindTo(field) {
        toFieldValue?.invoke(it) ?: it?.toString().orEmpty()
    }
}

fun CompoundButton.bindTo(field: Field<Boolean>) {
    setOnCheckedChangeListener { _, isChecked ->
        field.value = isChecked
    }
}

// endregion

/**
 * Добавить кликабельную картинку вместо последнего символа в строке
 */
@JvmOverloads
fun TextView.appendClickableImageTextView(
    @DrawableRes drawableResId: Int,
    clickFunc: () -> Unit = {}
): CharSequence =
    text.appendClickableImage(context, drawableResId, clickFunc)

/**
 * Убрать нижнее подчеркивание для текущего text
 */
@JvmOverloads
fun TextView.removeUnderlineFromUrlSpans(replaceNewLine: Boolean = true): CharSequence =
    setTextWithMovementMethod(text.removeUnderlineFromUrlSpans(false, replaceNewLine))

@JvmOverloads
fun TextView.setHtmlText(
    @StringRes resId: Int,
    clearHtml: Boolean = false,
    replaceNewLine: Boolean = true
) = setHtmlText(resources.getString(resId), clearHtml, replaceNewLine)

/**
 * Установить html текст в TextView
 */
@JvmOverloads
fun TextView.setHtmlText(
    text: CharSequence?,
    clearHtml: Boolean = false,
    replaceNewLine: Boolean = true
): CharSequence = setTextWithMovementMethod(
    if (clearHtml) {
        text.parseClearedHtml(replaceNewLine)
    } else {
        text.parseHtmlToSpannedString(replaceNewLine)
    }
)

@JvmOverloads
fun TextView.setLinkFromHtml(
    @StringRes htmlLinkResId: Int,
    removeUnderline: Boolean = true,
    replaceNewLine: Boolean = true
) = setLinkFromHtml(resources.getString(htmlLinkResId), removeUnderline, replaceNewLine)

/**
 * Установить ссылку из html-текста
 */
@JvmOverloads
fun TextView.setLinkFromHtml(
    htmlLink: String,
    removeUnderline: Boolean = true,
    replaceNewLine: Boolean = true
): CharSequence = if (removeUnderline) {
    setTextWithMovementMethod(htmlLink.removeUnderlineFromUrlSpans(true, replaceNewLine))
} else {
    setHtmlText(htmlLink, replaceNewLine)
}

/**
 * Выставить сформированные [ISpanInfo] по заданной подстроке
 */
fun TextView.setSpanText(
    text: CharSequence,
    vararg spanInfo: ISpanInfo
): CharSequence = setTextWithMovementMethod(text.createSpanText(*spanInfo))

/**
 * @param text в строке аргументы с префиксами "^" будут заменены на [CharacterStyle]
 * @param spanInfoMap маппинг информации о [Spannable] + link для перехода по клику по нему
 * (range в каждом [ISpanInfo] задействован не будет)
 */
fun TextView.setSpanTextExpanded(
    text: CharSequence,
    args: Map<String, List<CharacterStyle>>
): CharSequence = setTextWithMovementMethod(text.createSpanTextExpanded(args))


/**
 * Выставить [html] в кач-ве html текста, но для кликабельных сегментов оповещать о клике
 */
@JvmOverloads
fun TextView.replaceUrlSpans(
    html: String,
    parseHtml: Boolean,
    isUnderlineText: Boolean = false,
    replaceNewLine: Boolean = true,
    action: ((URLSpan) -> Boolean)? = null
): CharSequence = setTextWithMovementMethod(
    html.replaceUrlSpansByClickableSpans(parseHtml, isUnderlineText, replaceNewLine, action)
)

@JvmOverloads
fun TextView.setTextOrGone(
    text: CharSequence?,
    distinct: Boolean = true,
    asString: Boolean = true,
    isEmptyFunc: (CharSequence?) -> Boolean = { isEmpty(it) },
    formatFunc: ((CharSequence) -> CharSequence)? = null,
    vararg dependedViews: View
) {
    setTextWithVisibility(
        text,
        VisibilityHide.GONE,
        distinct,
        asString,
        isEmptyFunc,
        formatFunc,
        dependedViews.toList()
    )
}

@JvmOverloads
fun TextView.setTextOrInvisible(
    text: CharSequence?,
    distinct: Boolean = true,
    asString: Boolean = true,
    isEmptyFunc: (CharSequence?) -> Boolean = { isEmpty(it) },
    formatFunc: ((CharSequence) -> CharSequence)? = null,
    vararg dependedViews: View
) {
    setTextWithVisibility(
        text,
        VisibilityHide.INVISIBLE,
        distinct,
        asString,
        isEmptyFunc,
        formatFunc,
        dependedViews.toList()
    )
}

private fun TextView.setTextWithVisibility(
    text: CharSequence?,
    visibilityHide: VisibilityHide = VisibilityHide.GONE,
    distinct: Boolean = true,
    asString: Boolean = true,
    isEmptyFunc: (CharSequence?) -> Boolean = { isEmpty(it) },
    formatFunc: ((CharSequence) -> CharSequence)? = null,
    dependedViews: Collection<View> = emptyList()
) {
    val isEmpty = isEmptyFunc(text)
    if (text == null || isEmpty) {
        this.text = EMPTY_STRING
        setVisible(false, visibilityHide)
    } else {
        setTextChecked(formatFunc?.invoke(text) ?: text, distinct, asString)
        setVisible(true)
    }
    dependedViews.forEach { it.isVisible = isVisible }
}

@JvmOverloads
fun TextView.setSumOrGone(
    sum: Double,
    distinct: Boolean = true,
    asString: Boolean = true,
    formatFunc: ((Double) -> CharSequence)? = null,
    vararg dependedViews: View
) {
    setSumWithVisibility(
        sum,
        VisibilityHide.GONE,
        distinct,
        asString,
        formatFunc,
        dependedViews.toList()
    )
}

@JvmOverloads
fun TextView.setSumOrInvisible(
    sum: Double,
    distinct: Boolean = true,
    asString: Boolean = true,
    formatFunc: ((Double) -> CharSequence)? = null,
    vararg dependedViews: View
) {
    setSumWithVisibility(
        sum,
        VisibilityHide.INVISIBLE,
        distinct,
        asString,
        formatFunc,
        dependedViews.toList()
    )
}

private fun TextView.setSumWithVisibility(
    sum: Double,
    visibilityHide: VisibilityHide = VisibilityHide.GONE,
    distinct: Boolean = true,
    asString: Boolean = true,
    formatFunc: ((Double) -> CharSequence)? = null,
    dependedViews: Collection<View> = emptyList()
) {
    if (!sum.isZero()) {
        setTextWithVisibility(
            formatFunc?.let { it(sum) } ?: sum.toString(),
            visibilityHide,
            distinct,
            asString,
            dependedViews = dependedViews
        )
    } else {
        setTextWithVisibility(
            null as String?,
            visibilityHide,
            distinct,
            false,
            dependedViews = dependedViews
        )
    }
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
        val view = getFieldValue<TextView, TextInputLayout>(TextInputLayout::class.java, this, "mErrorView")
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
    with(context.resources.getColoredDrawable(iconResId, colorResId, mode)) {
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
    with(context.resources.getColoredDrawable(icon, color, mode)) {
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
    val statusBarHeight = context.resources.getStatusBarHeight()
    if (statusBarHeight < 0) {
        return 0
    }
    val dm = context.resources.displayMetrics
    if (isPreLollipop() || this.height == dm.heightPixels || this.height == dm.heightPixels - statusBarHeight) {
        return 0
    }
    val info: Any? = getFieldValue<Any?, View>(View::class.java, this, "mAttachInfo")
    if (info != null) {
        val insets: Rect? = getFieldValue(info.javaClass, info, "mStableInsets")
        return insets?.bottom ?: 0
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
    val behavior: BottomSheetBehavior<*>? =
        ReflectionUtils.getFieldValue<BottomSheetBehavior<*>, BottomSheetDialog>(BottomSheetDialog::class.java, this, "mBehavior")
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
    updatePadding(
        startPx ?: paddingStart, topPx ?: paddingTop, endPx ?: paddingEnd,
        bottomPx ?: paddingBottom
    )
}

@JvmOverloads
fun View.updateMargin(
    @Px left: Int = marginLeft,
    @Px top: Int = marginTop,
    @Px right: Int = marginRight,
    @Px bottom: Int = marginBottom,
) {
    if (layoutParams !is ViewGroup.MarginLayoutParams) return
    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        setMargins(left, top, right, bottom)
    }
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
        || reduceStepCount <= 0
    ) {
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
        || reduceStepCount <= 0
    ) {
        return sourceText
    }
    if (measuredWidth <= 0
        || measuredHeight <= 0
    ) {
        return sourceText
    }

    if (startHeightIndex < 0 || startHeightIndex >= sourceText.length
        || endHeightIndex < 0 || endHeightIndex >= sourceText.length
    ) {
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
fun View.getOffsetByParent(parent: ViewGroup): Rect {
    val offsetViewBounds = Rect()
    getDrawingRect(offsetViewBounds)
    parent.offsetDescendantRectToMyCoords(this, offsetViewBounds)
    return offsetViewBounds
}

@JvmOverloads
fun TextView.setTextDistinctFormatted(
    text: CharSequence?,
    maskWatcher: MaskFormatWatcher,
    asString: Boolean = true,
    refreshMask: Boolean = true
): Boolean {
    return if (refreshMask) {
        if (text.isNullOrEmpty()) {
            setText(EMPTY_STRING)
        } else {
            // getMask возвращает UnmodifiableMask -
            // format на её копии не сработает
            // maskOriginal - MaskImpl текущая изменяемая маска
            maskWatcher.refreshMask(text)
        }
        true
    } else {
        setTextDistinctFormatted(text, maskWatcher.maskOriginal, asString)
    }
}

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
fun View.setVisible(isVisible: Boolean, visibilityHide: VisibilityHide = VisibilityHide.GONE) {
    visibility = if (isVisible) {
        View.VISIBLE
    } else {
        if (visibilityHide == VisibilityHide.GONE) View.GONE else View.INVISIBLE
    }
}

enum class VisibilityHide {
    INVISIBLE, GONE
}

fun CompoundButton.setCheckedDistinct(checked: Boolean) {
    if (isChecked != checked) {
        isChecked = checked
    }
}

/**
 * Аналог [updateLayoutParams] с единственным отличием, что если каст LP к [T] не пройдет, вместо
 * краша просто не выполнится [block]
 */
inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParamsSafe(block: T.() -> Unit) {
    layoutParams ?: return
    updateLayoutParams { (this as? T)?.block() }
}

fun View.hideByReferenceViews(vararg otherViews: View) {
    val isGone = otherViews.all { it.visibility == View.GONE }
    visibility = if (isGone) View.GONE else View.INVISIBLE
}

fun TextView.setTextWithMovementMethod(text: CharSequence?): CharSequence {
    this.text = text
    movementMethod = LinkMovementMethod.getInstance()
    return orEmpty(text)
}

fun View.setTextWithAccessibilityDelegate(@StringRes descriptionResId: Int) {
    setTextWithAccessibilityDelegate(context.getString(descriptionResId))
}

fun View.setTextWithAccessibilityDelegate(description: String) {
    setAccessibilityAction { info ->
        info.text = description
    }
}

fun View.setClickTextWithAccessibilityDelegate(@StringRes labelResId: Int) {
    setClickTextWithAccessibilityDelegate(context.getString(labelResId))
}

fun View.setClickTextWithAccessibilityDelegate(label: String) {
    setAccessibilityAction { info ->
        info.addAction(
            AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                AccessibilityNodeInfo.ACTION_CLICK,
                label
            )
        )
    }
}

/**
 * Может использоваться для отключения озвучивания процентов в SeekBar
 */
fun View.disableTalkback(focusAction: ((Boolean) -> Unit)? = null) {
    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {

        override fun sendAccessibilityEvent(host: View, eventType: Int) {
            var eventType: Int = eventType
            if (eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
//                focusAction?.invoke(true)
                eventType = AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED;
            }
            super.sendAccessibilityEvent(host, eventType)
        }

        override fun sendAccessibilityEventUnchecked(host: View, event: AccessibilityEvent) {
            if (event.eventType !in arrayOf(
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.TYPE_VIEW_SELECTED
                )
            ) {
                super.sendAccessibilityEventUnchecked(host, event)
            }
        }

        override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
            super.onInitializeAccessibilityEvent(host, event)
            val eventType = event.eventType
            if (eventType == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
                focusAction?.invoke(true)
            } else if (eventType == AccessibilityEvent.TYPE_VIEW_HOVER_EXIT) {
                focusAction?.invoke(false)
            }
        }
    })
}

private fun View.setAccessibilityAction(action: (AccessibilityNodeInfoCompat) -> Unit) {
    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
        var didPerformAccessibilityAction = false

        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            if (didPerformAccessibilityAction) {
                didPerformAccessibilityAction = false
                action(info)
            }
        }

        override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
            didPerformAccessibilityAction = super.performAccessibilityAction(host, action, args)
            return didPerformAccessibilityAction
        }
    })
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
fun WebView.loadDataCompat(
    data: String,
    mimeType: String = "text/html",
    charset: Charset = Charsets.UTF_8,
    baseUrlParams: BaseUrlParams? = null,
    forceBase64: Boolean = true,
) {
    if (forceBase64 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        loadDataBase64(data, mimeType, charset, baseUrlParams)
    } else {
        loadData(
            data,
            MIME_TYPE_WITH_CHARSET_FORMAT.format(mimeType, charset.toString()),
            charset.name(),
            baseUrlParams
        )
    }
}

@TargetApi(Build.VERSION_CODES.N)
@JvmOverloads
fun WebView.loadDataBase64(
    data: String,
    mimeType: String = "text/html",
    charset: Charset = Charsets.UTF_8,
    baseUrlParams: BaseUrlParams? = null,
) {
    loadData(
        Base64.encodeToString(data.toByteArray(charset), Base64.DEFAULT),
        MIME_TYPE_WITH_CHARSET_FORMAT.format(mimeType, charset),
        "base64",
        baseUrlParams
    )
}

private fun WebView.loadData(
    data: String,
    mimeTypeWithCharset: String,
    encoding: String,
    baseUrlParams: BaseUrlParams?,
) {
    baseUrlParams?.let {
        loadDataWithBaseURL(it.baseUrl, data, mimeTypeWithCharset, encoding, it.failUrl)
    } ?: loadData(data, mimeTypeWithCharset, encoding)
}

data class BaseUrlParams(
    val baseUrl: String?,
    val failUrl: String?,
)

@JvmOverloads
fun TextView.observePlaceholderOrLabelHint(
    inputLayout: TextInputLayout?,
    @StringRes placeholderTextResId: Int,
    @StringRes labelTextResId: Int,
    setHintFunc: ((CharSequence) -> Unit)? = null,
    isForPlaceholderFunc: ((CharSequence?) -> Boolean)? = null
) = observePlaceholderOrLabelHint(
    inputLayout,
    context.getString(placeholderTextResId),
    context.getString(labelTextResId),
    setHintFunc,
    isForPlaceholderFunc
)

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
    val listener = AfterTextChangeListener { e ->
        val text = e.toString()
        val hint = if (isForPlaceholderFunc?.invoke(text)
                ?: TextUtils.isEmpty(text)
        ) placeholderText else labelText
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
) = setupPlaceholderOrLabelHint(
    inputLayout,
    rule,
    context.getString(placeholderTextResId),
    context.getString(labelTextResId),
    setHintFunc,
    isForPlaceholderFunc
)

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
                    ?: TextUtils.isEmpty(text)
            ) placeholderText else labelText
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

class EditTextKeyLimiter(private val et: EditText, private val linesLimit: Int) : View.OnKeyListener {

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (et === v) { // if enter is pressed start calculating
            if (keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_UP
            ) {
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