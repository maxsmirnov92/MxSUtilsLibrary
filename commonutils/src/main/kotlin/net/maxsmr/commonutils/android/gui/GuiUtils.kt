package net.maxsmr.commonutils.android.gui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.CountDownTimer
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.URLSpan
import android.util.Base64
import android.util.TypedValue
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.android.*
import net.maxsmr.commonutils.android.gui.listeners.DefaultTextWatcher
import net.maxsmr.commonutils.android.gui.listeners.OnTextWatcher
import net.maxsmr.commonutils.android.livedata.setValueIfNew
import net.maxsmr.commonutils.data.Pair
import net.maxsmr.commonutils.data.ReflectionUtils
import net.maxsmr.commonutils.data.text.*
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

private const val DEFAULT_DARK_COLOR_RATIO = 0.7

private const val POPUP_HEIGHT_CORRECTION = 24f

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("GuiUtils")

private val defaultWindowConfigurator: (PopupWindow, Context) -> Unit = { window, context ->
    with(window) {
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)))
        isFocusable = true
        isTouchable = true
        isOutsideTouchable = true
    }
}

fun Activity.setFullScreen(toggle: Boolean) {
    with(window) {
        if (toggle) {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        } else {
            addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        decorView.requestLayout()
    }
}

fun AppCompatActivity.setHomeButtonEnabled(toggle: Boolean) {
    supportActionBar?.let {
        it.setDisplayShowHomeEnabled(toggle)
        it.setDisplayHomeAsUpEnabled(toggle)
    }
}

fun KeyEvent?.isEnterKeyPressed(actionId: Int): Boolean =
        this != null && this.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_NULL

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
        isEmptyFunc: (CharSequence?) -> Boolean = { isEmpty(it)}
) {
    visibility = if (isEmptyFunc(text)) {
        this.text = EMPTY_STRING
        if (isGoneOrInvisible) View.GONE else View.INVISIBLE
    } else {
        setTextChecked(text, distinct, asString)
        View.VISIBLE
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
            requestFocus(this)
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

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setDefaultStatusBarColor() {
    setStatusBarColor(getColorFromAttrs(window.context, intArrayOf(R.attr.colorPrimaryDark)))
}

/**
 * Красит статус бар в указанный цвет.
 * Так же, красит иконки статус бара в серый цвет, если SDK >= 23 и устанавливаемый цвет слишком белый
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setStatusBarColor(@ColorInt color: Int) {
    if (isAtLeastLollipop()) {
        window.statusBarColor = color
        if (isAtLeastMarshmallow()) {
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

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setDefaultNavigationColor() {
    setNavigationBarColor(getColorFromAttrs(window.context, intArrayOf(R.attr.colorPrimaryDark)))
}

/**
 * Красит нав бар в указанный цвет
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.setNavigationBarColor(@ColorInt color: Int) {
    if (isAtLeastLollipop()) {
        window.navigationBarColor = color
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
        drawable?.let {  Pair(it.intrinsicWidth, it.intrinsicHeight) } ?: Pair(0, 0)

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

fun Resources.getStatusBarHeight(): Int {
    val resourceId = getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        getDimensionPixelSize(resourceId)
    } else 0
}

fun getKeyboardHeight(rootView: View, targetView: View): Int {
    val rect = Rect()
    targetView.getWindowVisibleDisplayFrame(rect)
    val usableViewHeight = rootView.height - (if (rect.top != 0) rootView.context.resources.getStatusBarHeight() else 0) - rootView.getViewInset()
    return usableViewHeight - (rect.bottom - rect.top)
}

/**
 * Добавление слушателя [OnSoftInputStateListener] на состояние клавиатуры
 *
 * @param rootView корневай [View] на экране
 */
fun addSoftInputStateListener(
        rootView: View,
        openedAction: () -> Unit,
        closedAction: () -> Unit
): OnGlobalLayoutListener {
    val listener = object : OnGlobalLayoutListener {
        private val HEIGHT_ROOT_THRESHOLD = 100
        override fun onGlobalLayout() {
            val heightDiff = rootView.rootView.height - rootView.height
            if (heightDiff > HEIGHT_ROOT_THRESHOLD) {
                openedAction()
            } else {
                closedAction()
            }
        }
    }
    rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    return listener
}

fun isKeyboardShown(activity: Activity): Boolean =
        isKeyboardShown(activity.currentFocus)

fun isKeyboardShown(view: View?): Boolean {
    if (view == null) {
        return false
    }
    try {
        val inputManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager? ?: return false
        return inputManager.isActive(view)
    } catch (e: Exception) {
        logger.e("An exception occurred: ${e.message}", e)
        return false
    }
}

@JvmOverloads
fun showKeyboard(
        activity: Activity,
        flags: Int = InputMethodManager.SHOW_IMPLICIT
): Boolean =
        showKeyboard(activity.currentFocus, flags, false)

@JvmOverloads
fun showKeyboard(
        hostView: View?,
        flags: Int = InputMethodManager.SHOW_IMPLICIT,
        requestFocus: Boolean = true
): Boolean {
    if (hostView == null) return false
    if (requestFocus) {
        requestFocus(hostView)
    }
    val imm = hostView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            ?: return false
    return imm.showSoftInput(hostView, flags)
}

@JvmOverloads
fun hideKeyboard(
        activity: Activity,
        flags: Int = 0,
        clearFocus: Boolean = true
) = hideKeyboard(activity.currentFocus, flags, clearFocus)

@JvmOverloads
fun hideKeyboard(
        hostView: View?,
        flags: Int = 0,
        clearFocus: Boolean = true
): Boolean {
    hostView?.let {
        if (clearFocus) {
            clearFocus(hostView)
        }
        hostView.windowToken?.let {
            val imm = hostView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                    ?: return false
            return imm.hideSoftInputFromWindow(it, flags)
        }
    }
    return false
}

@JvmOverloads
fun toggleKeyboard(activity: Activity, toggleFocus: Boolean = true) {
    toggleKeyboard(activity.currentFocus, toggleFocus)
}

@JvmOverloads
fun toggleKeyboard(hostView: View?, toggleFocus: Boolean = true) {
    if (isKeyboardShown(hostView)) {
        hideKeyboard(hostView, clearFocus = toggleFocus)
    } else {
        showKeyboard(hostView, requestFocus = toggleFocus)
    }
}

fun showAboveLockscreen(window: Window, wakeScreen: Boolean) {
    toggleAboveLockscreen(window, wakeScreen, true)
}

fun hideAboveLockscreen(window: Window, wakeScreen: Boolean) {
    toggleAboveLockscreen(window, wakeScreen, false)
}

private fun toggleAboveLockscreen(window: Window, wakeScreen: Boolean, toggle: Boolean) {
    var flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
    if (wakeScreen) {
        flags = flags or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    }
    if (toggle) {
        window.addFlags(flags)
    } else {
        window.clearFlags(flags)
    }
}

/**
 * Запросить фокус или показать клавиатуру в зав-ти от состояния view
 */
@JvmOverloads
fun toggleFocusOrKeyboardState(view: View, activity: Activity, toggle: Boolean = true) {
    if (toggle) {
        if (!view.isFocused) {
            requestFocus(view)
        } else {
            showKeyboard(view)
        }
    } else {
        hideKeyboard(activity)
        // очистка фокуса не убирает клавиатуру
        clearFocus(activity)
    }
}

/**
 * @return true if focused, false otherwise
 */
fun requestFocus(view: View?): Boolean {
    if (view != null) {
        if (view.isFocusable) {
//            act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            if (view.requestFocus()) {
                return true
            }
        }
    }
    return false
}

/**
 * @return true if focus cleared, false otherwise
 */
fun clearFocus(view: View?): Boolean {
    if (view != null) {
        view.clearFocus()
//        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        return true
    }
    return false
}

/**
 * @return true if focus cleared, false otherwise
 */
fun clearFocus(act: Activity?): Boolean =
        clearFocus(act?.currentFocus)

/**
 * Выполнить [action] с проверкой
 * во избежание [IllegalStateException]
 */
fun runActionOnRecycler(recyclerView: RecyclerView, action: (() -> Unit)) {
    if (!recyclerView.isComputingLayout) {
        action()
    } else {
        recyclerView.post {
            action()
        }
    }
}

/**
 * Показать тост с длительностью, отличающейся от стандартных
 * [Toast.LENGTH_SHORT] и [Toast.LENGTH_LONG]
 */
fun showToastWithDuration(targetDuration: Long, toastFunc: (() -> Toast)): CountDownTimer {
    require(targetDuration > 0) {
        throw IllegalArgumentException("Incorrect targetDuration: $targetDuration")
    }
    return object : CountDownTimer(targetDuration, TimeUnit.SECONDS.toMillis(1)) {

        private var previousToast: Toast? = null

        override fun onFinish() {
            show()
        }

        override fun onTick(millisUntilFinished: Long) {
            show()
        }

        private fun show() {
            previousToast?.cancel()
            with(toastFunc()) {
                // меняем на 1 секунду для правильного интервала таймера
                duration = Toast.LENGTH_SHORT
                previousToast = this
                show()
            }
        }
    }.start()
}

fun RadioGroup.getSelectedIndex(): Int {
    val radioButtonId = checkedRadioButtonId
    val radioButton = findViewById<RadioButton>(radioButtonId) ?: null
    return radioButton?.let { indexOfChild(it) } ?: RecyclerView.NO_POSITION
}

fun collapseToolbar(rootLayout: CoordinatorLayout, coordinatorChild: View, appBarLayout: AppBarLayout) {
    var found = false
    for (i in 0 until rootLayout.childCount) {
        if (rootLayout.getChildAt(i) === coordinatorChild) {
            found = true
        }
    }
    require(found) { "view $coordinatorChild is not a child of $rootLayout" }
    val params = coordinatorChild.layoutParams as CoordinatorLayout.LayoutParams
    val behavior = params.behavior as ScrollingViewBehavior?
    behavior?.onNestedFling(rootLayout, appBarLayout, coordinatorChild, 0f, 10000f, true)
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

fun setViewSize(view: View, size: Pair<Int, Int>) {
    require(!(size.first < -1 || size.second < -1)) { "incorrect view size: " + size.first + "x" + size.second }
    val layoutParams = view.layoutParams
    layoutParams.width = size.first
    layoutParams.height = size.second
    view.layoutParams = layoutParams
}

fun getCurrentDisplayOrientation(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            ?: throw NullPointerException(WindowManager::class.java.simpleName + " is null")
    var degrees = 0
    when (windowManager.defaultDisplay.rotation) {
        Surface.ROTATION_90 -> degrees = 90
        Surface.ROTATION_180 -> degrees = 180
        Surface.ROTATION_270 -> degrees = 270
    }
    return degrees
}

fun getCorrectedDisplayRotation(rotation: Int): Int {
    val correctedRotation = rotation % 360
    var result = OrientationIntervalListener.ROTATION_NOT_SPECIFIED
    if (correctedRotation in 315..359 || correctedRotation in 0..44) {
        result = 0
    } else if (correctedRotation in 45..134) {
        result = 90
    } else if (correctedRotation in 135..224) {
        result = 180
    } else if (correctedRotation in 225..314) {
        result = 270
    }
    return result
}

fun getViewsRotationForDisplay(context: Context, displayRotation: Int): Int {
    var result = 0
    val correctedDisplayRotation = getCorrectedDisplayRotation(displayRotation)
    if (correctedDisplayRotation != OrientationIntervalListener.ROTATION_NOT_SPECIFIED) {
        val currentOrientation = context.resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            when (correctedDisplayRotation) {
                0 -> result = 270
                90 -> result = 180
                180 -> result = 90
                270 -> result = 0
            }
        } else {
            when (correctedDisplayRotation) {
                0 -> result = 0
                90 -> result = 270
                180 -> result = 180
                270 -> result = 90
            }
        }
    }
    return result
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
fun WebView.loadDataBase64(value: String, charset: Charset = Charsets.UTF_8) {
    loadData(Base64.encodeToString(value.toByteArray(charset), Base64.DEFAULT), "text/html; charset=${charset}", "base64")
}

/**
 * @param onDismissed - true, если дальнейший показ не требуется
 * @return показанный с 0-ой высотой [PopupWindow]
 */
@JvmOverloads
fun showWindowPopupWithObserver(
        context: Context,
        currentPopup: PopupWindow?,
        anchorView: View,
        gravity: Int = Gravity.TOP,
        contentViewCreator: (View) -> View,
        onDismissed: ((PopupWindow) -> Boolean)? = null,
        onShowed: ((PopupWindow) -> Unit)? = null,
        windowConfigurator: (PopupWindow, Context) -> Unit = defaultWindowConfigurator
): PopupWindow? {
    val contentViewCreatorListener: (ViewReadyListener) -> View = {
        val contentView = contentViewCreator(anchorView)
        observeViewReady(contentView, it)
    }
    return showWindowPopup(
            context,
            currentPopup,
            anchorView,
            gravity,
            contentViewCreatorListener,
            onDismissed,
            onShowed,
            windowConfigurator
    )
}

@JvmOverloads
fun showWindowPopup(
        context: Context,
        currentPopup: PopupWindow?,
        anchorView: View,
        gravity: Int = Gravity.TOP,
        contentViewCreator: (ViewReadyListener) -> View,
        onDismissed: ((PopupWindow) -> Boolean)? = null,
        onShowed: ((PopupWindow) -> Unit)? = null,
        windowConfigurator: (PopupWindow, Context) -> Unit = defaultWindowConfigurator
): PopupWindow? {
    var popup = currentPopup
    if (popup != null) {
        popup.dismiss()
        if (onDismissed?.invoke(popup) == true) {
            return null
        }
    }
    val listener = object : ViewReadyListener {
        override fun onViewReady(width: Int, height: Int) {
            popup?.let {
                // Костыль. Нужно знать высоту попапа перед показом для определения правильной позиции,
                // но по какой-то причине она определяется некорректно (при вызове measure и обращении к measuredWidth, measuredHeight)
                // поэтому быстро показываем его дважды: после первого показа забираем корректные ширину и высоту,
                // и показываем еще раз уже в нужном месте
                it.show(anchorView, height, gravity)
                onShowed?.invoke(it)
                popup?.setOnDismissListener {
                    onDismissed?.invoke(it)
                }
            }
        }
    }
    popup = createWindowPopup(context, contentViewCreator(listener), windowConfigurator)
    // первый показ, когда высота неизвестна
    popup.show(anchorView, 0, gravity)
    return popup
}

fun observeViewReady(contentView: View, listener: ViewReadyListener): View {
    return contentView.apply {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (viewTreeObserver.isAlive) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                listener.onViewReady(width, height)
            }
        })
    }
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
fun PopupWindow.show(
        anchor: View,
        popupContentHeight: Int,
        gravity: Int = Gravity.TOP
) {
    dismiss()
    if (popupContentHeight < 0) {
        return
    }
    val hCorrection = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, POPUP_HEIGHT_CORRECTION, anchor.context.resources.displayMetrics).toInt()
    val anchorLocation = anchor.screenLocation()
    val y = anchorLocation.top - popupContentHeight + hCorrection
    showAtLocation(anchor, gravity, 0, y)
}

private fun createWindowPopup(
        context: Context,
        contentView: View,
        configurator: (PopupWindow, Context) -> Unit
): PopupWindow = with(PopupWindow(context)) {
    configurator(this, context)
    this.contentView = contentView
    return this
}

interface ViewReadyListener {

    fun onViewReady(width: Int, height: Int)
}

private fun TextView.setTextWithMovementMethod(text: CharSequence): CharSequence {
    this.text = text
    this.movementMethod = LinkMovementMethod.getInstance()
    return text
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