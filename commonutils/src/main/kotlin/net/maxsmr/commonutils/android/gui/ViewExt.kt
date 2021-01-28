package net.maxsmr.commonutils.android.gui

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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.android.*
import net.maxsmr.commonutils.android.gui.listeners.DefaultTextWatcher
import net.maxsmr.commonutils.android.gui.listeners.OnTextWatcher
import net.maxsmr.commonutils.android.live.setValueIfNew
import net.maxsmr.commonutils.android.media.getBase64
import net.maxsmr.commonutils.data.Pair
import net.maxsmr.commonutils.data.ReflectionUtils
import net.maxsmr.commonutils.data.text.*
import net.maxsmr.commonutils.graphic.scaleDownBitmap
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
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

fun TextView.bindTo(field: MutableLiveData<String>) = bindTo(field) {
    it.toString()
}

fun <D> TextView.bindTo(field: MutableLiveData<D>, fieldMapper: (CharSequence) -> D) {
    addTextChangedListener(OnTextWatcher { s: CharSequence?, start: Int, before: Int, count: Int ->
        field.setValueIfNew(fieldMapper(s?.toString() ?: EMPTY_STRING))
    })
}

fun CompoundButton.bindTo(field: MutableLiveData<Boolean>) {
    setOnCheckedChangeListener { _, isChecked ->
        field.setValueIfNew(isChecked)
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
        appendClickableImage(context, text, drawableResId, spanFlags, clickFunc)


/**
 * Убрать нижнее подчеркивание для текущего text
 */
fun TextView.removeUnderlineTextView(): CharSequence =
        setTextWithMovementMethod(removeUnderline(this.text))

/**
 * Установить html текст в TextView
 */
fun TextView.setHtmlText(text: CharSequence): CharSequence =
        setTextWithMovementMethod(parseHtmlToSpannedString(text))


fun TextView.setHtmlText(@StringRes resId: Int) =
        setHtmlText(resources.getString(resId))

@JvmOverloads
fun TextView.setLinkFromHtml(@StringRes htmlLinkResId: Int, removeUnderlying: Boolean = true) =
        setLinkFromHtml(resources.getString(htmlLinkResId), removeUnderlying)

/**
 * Установить ссылку из html-текста
 */
@JvmOverloads
fun TextView.setLinkFromHtml(htmlLink: String, removeUnderlying: Boolean = true): CharSequence {
    val result = setHtmlText(htmlLink)
    if (removeUnderlying) {
        removeUnderlineTextView()
    }
    return result
}

/**
 * Установить кликабельный span
 * с кастомным действием при нажатии
 */
fun TextView.setTextWithCustomSpan(text: CharSequence, spanInfos: Collection<SpanInfo>): CharSequence =
        setTextWithMovementMethod(createCustomSpanText(text, spanInfos))

/**
 * @param text в строке аргументы с префиксами "^" будут заменены на [CharacterStyle]
 * @param spanInfosMap маппинг информации о [Spannable] + link для перехода по клику по нему
 */
fun TextView.setTextWithCustomSpanExpanded(text: CharSequence, spanInfosMap: Map<SpanInfo, String>): CharSequence =
        setTextWithMovementMethod(createCustomSpanTextExpanded(text, spanInfosMap))


/**
 * Альтернатива [setLinkFromHtml], в котором в кач-ве [text]
 * вместо html-разметки обычный текст с кликабельной ссылкой
 * в указанном диапазоне
 * @param spanInfosMap маппинг информации о [Spannable] + link для перехода по клику по нему
 */
fun TextView.setLinkableText(text: CharSequence, spanInfosMap: Map<SpanInfo, String>): CharSequence =
        setTextWithMovementMethod(createLinkableText(text, spanInfosMap))

/**
 * Выставить [html] в кач-ве html текста, но для кликабельных сегментов оповещать о клике
 */
@JvmOverloads
fun TextView.replaceUrlSpans(
        html: String,
        removeUnderlying: Boolean = true,
        action: ((URLSpan) -> Boolean)? = null
): CharSequence =
        setTextWithMovementMethod(replaceUrlSpansByClickableSpans(context, html, removeUnderlying, action))

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
    setTextWithMovementMethod(createSelectedText(text, highlightColor, selection, spanFlags))
}

@JvmOverloads
fun TextView.setTextWithVisibility(
        text: CharSequence?,
        isGoneOrInvisible: Boolean = true,
        distinct: Boolean = true,
        asString: Boolean = true,
        isEmptyFunc: (CharSequence?) -> Boolean = { isEmpty(it) }
) {
    if (isEmptyFunc(text)) {
        this.text = EMPTY_STRING
        setVisible(false, isGoneOrInvisible)
    } else {
        setTextChecked(text, distinct, asString)
        setVisible(true)
    }
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

/**
 * Выставить [icon] с фильтром [color] в кач-ве background для [View]
 */
@JvmOverloads
@SuppressLint("ResourceType")
fun View.setBackgroundTint(
        @DrawableRes icon: Int,
        @ColorInt color: Int,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    with(getColoredDrawable(context.resources, icon, color, mode)) {
        background = this
        return this
    }
}

/**
 * Выставить [icon] с фильтром [color] в кач-ве src для [ImageView]
 */
@JvmOverloads
@SuppressLint("ResourceType")
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
        measuredViewWidth: Int = 0
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
        measuredViewWidth: Int = 0
): Float {
    if (maxWidthText.isEmpty() || maxTextSize <= 0) return maxTextSize
    val measuredWidth = if (measuredViewWidth <= 0) width else measuredViewWidth
    if (measuredWidth == 0) return maxTextSize

    val maxWidthString = maxWidthText.toString()
    var resultSize = maxTextSize

    val paint = Paint()
    paint.typeface = typeface

    paint.textSize = convertAnyToPx(resultSize, maxTextSizeUnit, context)
    var measureText = paint.measureText(maxWidthString)

    while (resultSize > 0 && measureText > measuredWidth) {
        resultSize--
        paint.textSize = convertAnyToPx(resultSize, maxTextSizeUnit, context)
        measureText = paint.measureText(maxWidthString)
    }
    if (resultSize == 0f) {
        resultSize = maxTextSize
    }
    return resultSize
}

/**
 * @return relative coordinates to the parent
 */
fun View.getBoundsByParent(parent: ViewGroup): Rect {
    val offsetViewBounds = Rect();
    getDrawingRect(offsetViewBounds);
    parent.offsetDescendantRectToMyCoords(this, offsetViewBounds);
    return offsetViewBounds
}

@JvmOverloads
fun TextView.setTextDistinct(text: CharSequence?, asString: Boolean = true) {
    if (if (asString) this.text?.toString() != text.toString() else this.text != text) {
        this.text = text
    }
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

private fun TextView.setTextWithMovementMethod(text: CharSequence): CharSequence {
    this.text = text
    this.movementMethod = LinkMovementMethod.getInstance()
    return text
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
    loadData(getBase64(value, charset, Base64.DEFAULT), "text/html; charset=${charset}", "base64")
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