package net.maxsmr.commonutils.android.gui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.CountDownTimer
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.android.*
import net.maxsmr.commonutils.data.Pair
import net.maxsmr.commonutils.data.ReflectionUtils
import net.maxsmr.commonutils.data.text.*
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.util.concurrent.TimeUnit

private const val DEFAULT_DARK_COLOR_RATIO = 0.7

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("GuiUtils")

fun setFullScreen(activity: Activity, toggle: Boolean) {
    if (toggle) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
    } else {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
    activity.window.decorView.requestLayout()
}

fun setHomeButtonEnabled(activity: AppCompatActivity, toggle: Boolean) {
    activity.supportActionBar?.let {
        it.setDisplayShowHomeEnabled(toggle)
        it.setDisplayHomeAsUpEnabled(toggle)
    }
}

fun isEnterKeyPressed(event: KeyEvent?, actionId: Int): Boolean {
    return event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_NULL
}

/**
 * Установка [IntervalEditorActionListener] на editor action у [EditText]
 * срабатывает на любой actionId
 * @param action пользовательское действие, которое необходимо выполнить при срабатывании
 * (должно вернуть true, если было обработано)
 */
fun setOnIntervalEditorActionListener(textView: TextView, action: ((TextView, Int, KeyEvent?) -> Boolean)?) {
    textView.setOnEditorActionListener(object : IntervalEditorActionListener() {

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
fun setOnEnterIntervalEditorActionListener(textView: TextView, action: ((TextView) -> Unit)?) {
    textView.setOnEditorActionListener(object : IntervalEditorActionListener() {

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
fun setOnFocusIntervalEditorActionListener(textView: TextView, nextView: View) {
    textView.setOnEditorActionListener(object : IntervalEditorActionListener() {

        override fun shouldDoAction(v: TextView, actionId: Int, event: KeyEvent?) = actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_NULL // именно такой id будет при хардварном Enter

        override fun doAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
            return nextView.requestFocus()
        }
    })
}

/**
 * Убрать нижнее подчеркивание для текущего text
 */
fun removeTextViewUnderline(view: TextView) {
    setText(view, removeUnderline(view.text), true)
}

/**
 * Добавить кликабельную картинку вместо последнего символа в строке
 */
fun addClickableImageToEnd(
        textView: TextView,
        @DrawableRes drawableResId: Int,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        clickFunc: () -> Unit = {}) {
    val text = textView.text
    if (text.isNotEmpty()) {
        val s = SpannableString(text)
        val imageSpan = ImageSpan(textView.context, drawableResId, ImageSpan.ALIGN_BASELINE)
        s.setSpan(imageSpan, s.length - 1, s.length, spanFlags)
        s.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                if (!SdkUtils.isPreLollipop()) {
                    widget.cancelPendingInputEvents()
                }
                clickFunc.invoke()
            }
        }, s.length - 1, s.length, spanFlags)
        textView.text = s
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}

/**
 * Устанавливает курсор в конец строки
 */
fun setSelectionToEnd(edit: EditText) {
    if (edit.text.isNotEmpty()) {
        edit.setSelection(edit.text.length)
    }
}

/**
 * Установка лимита на количество символов допустимых к набору в [EditText]
 *
 * @param length целочисленное значение, соответствующее максимальному
 * количеству символов, допустимых к набору в [EditText].
 */
fun setMaxLength(view: EditText, length: Int) {
    if (length >= 0) {
        val inputTextFilter = InputFilter.LengthFilter(length)
        view.filters = view.filters
                .filterNot { it is InputFilter.LengthFilter }
                .toTypedArray()
                .plus(inputTextFilter)
    }
}

/**
 * Снятие лимита на количество символов допустимых к набору в [EditText]
 */
fun clearMaxLength(view: EditText) {
    view.filters = view.filters
            .filterNot { it is InputFilter.LengthFilter }
            .toTypedArray()
}

/**
 * Установить html текст в TextView
 */
fun setHtmlText(textView: TextView, text: String) {
    try {
        textView.text = parseHtmlToSpannedString(text)
    } catch (e: Throwable) {
        textView.text = text
    }
}

fun setHtmlText(textView: TextView, @StringRes resId: Int) =
        setHtmlText(textView, textView.resources.getString(resId))

/**
 * Установить ссылку из html-текста
 */
fun setLinkFromHtml(textView: TextView, htmlLink: String, removeUnderlying: Boolean = true) {
    setHtmlText(textView, htmlLink)
    if (removeUnderlying) {
        removeTextViewUnderline(textView)
    }
    textView.movementMethod = LinkMovementMethod.getInstance()
}

fun setLinkFromHtml(textView: TextView, @StringRes htmlLinkResId: Int, removeUnderlying: Boolean = true) =
        setLinkFromHtml(textView, textView.resources.getString(htmlLinkResId), removeUnderlying)

/**
 * Альтернатива [setLinkFromHtml], в котором в кач-ве [text]
 * вместо html-разметки обычный текст с кликабельной ссылкой
 * в указанном диапазоне
 */
fun setLinkableText(
        textView: TextView,
        text: CharSequence,
        startIndex: Int,
        endIndex: Int,
        removeUnderlying: Boolean,
        link: String,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        clickAction: (() -> Unit)? = null
) {
    setTextWithClickableSpan(textView, text, startIndex, endIndex, removeUnderlying, spanFlags) {
        textView.context.startActivity(getBrowseLinkIntent(link))
        clickAction?.invoke()
    }
}

/**
 * Установить кликабельный span
 * с кастомным действием при нажатии
 */
fun setTextWithClickableSpan(
        textView: TextView,
        text: CharSequence,
        startIndex: Int,
        endIndex: Int,
        removeUnderlying: Boolean,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        clickAction: () -> Unit
) {

    textView.text = SpannableStringBuilder(text).apply {
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
        }, startIndex, endIndex, spanFlags)
    }
    textView.movementMethod = LinkMovementMethod.getInstance()
}

/**
 * Выставить [html] в кач-ве html текста, но для кликабельных сегментов оповещать о клике
 */
fun setHtmlTextWithCustomClick(
        textView: TextView,
        html: String,
        removeUnderlying: Boolean = true,
        action: ((URLSpan) -> Unit)? = null
) {
    val sequence = parseHtmlToSpannedString(html)
    val strBuilder = SpannableStringBuilder(sequence)
    val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
    urls.forEach { span ->
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        strBuilder.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                textView.context.startActivity(getBrowseLinkIntent(span.url))
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
    textView.text = strBuilder
    textView.movementMethod = LinkMovementMethod.getInstance()
}

/**
 * Установить текст с выделенным текстом
 * @param highlightColor цвет выделенного текста
 * @param str текст
 * @param selection текст для выделения (ищется первое вхождение [selection] в [str]
 */
fun setTextWithSelection(
        view: TextView,
        @ColorInt highlightColor: Int,
        str: String,
        selection: String,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
) {
    view.text = SpannableString(str)
            .apply {
                val start = str.indexOf(selection, ignoreCase = true)
                        .takeIf { it >= 0 }
                        ?: return@apply
                setSpan(
                        ForegroundColorSpan(highlightColor),
                        start,
                        start + selection.length,
                        spanFlags
                )
            }
}

fun setTextWithVisibility(
        view: TextView,
        text: CharSequence?,
        distinct: Boolean = true
) {
    if (isEmpty(text)) {
        view.visibility = View.GONE
    } else {
        setText(view, text, distinct)
        view.visibility = View.VISIBLE
    }
}

fun setText(
        view: TextView,
        text: CharSequence?,
        distinct: Boolean = true
) {
    if (!distinct || view.text?.toString() != text.toString()) {
        view.text = text
    }
}

fun setTextWithSelectionToEnd(
        edit: EditText,
        text: CharSequence,
        distinct: Boolean = true
) {
    setText(edit, text, distinct)
    // после возможных фильтров текст мог измениться
    setSelectionToEnd(edit)
}

fun setEditTextHintByError(on: TextInputLayout, hint: String = EMPTY_STRING) {
    val et = on.editText
    if (et != null) {
        et.hint = if (isEmpty(on.error)) null else hint
    }
}

fun setInputErrorTextColor(on: TextInputLayout?, color: Int) {
    try {
        val view = ReflectionUtils.getFieldValue<TextView, TextInputLayout>(TextInputLayout::class.java, on, "mErrorView")
        view?.let {
            it.setTextColor(color)
            it.requestLayout()
        }
    } catch (e: Exception) {
        logger.e("An exception occurred: ${e.message}", e)
    }
}

fun setInputError(til: TextInputLayout, @StringRes messageResId: Int?, isChecked: Boolean = true) {
    val message = if (messageResId == null || messageResId == 0) {
        null
    } else {
        til.resources.getString(messageResId)
    }
    setInputError(til, message, isChecked)
}

fun setInputError(
        til: TextInputLayout,
        message: CharSequence?,
        noError: Boolean = true,
        shouldRequestFocus: Boolean = true
) {
    if (til.error != message) {
        til.isErrorEnabled = !noError || message != null
        til.error = message
        til.refreshDrawableState()
        if (shouldRequestFocus && message != null) {
            requestFocus(til.editText)
        }
    }
}

fun clearInputError(on: TextInputLayout, force: Boolean = false) {
    if (force || !isEmpty(on.error)) {
        on.error = null
        on.refreshDrawableState()
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun setDefaultStatusBarColor(activity: Activity) {
    setStatusBarColor(activity,
            getColorFromAttrs(activity.window.context, intArrayOf(R.attr.colorPrimaryDark))
    )
}

/**
 * Красит статус бар в указанный цвет.
 * Так же, красит иконки статус бара в серый цвет, если SDK >= 23 и устанавливаемый цвет слишком белый
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun setStatusBarColor(activity: Activity, @ColorInt color: Int) {
    if (SdkUtils.isAtLeastLollipop()) {
        activity.window.statusBarColor = color
        if (SdkUtils.isAtLeastMarshmallow()) {
            // если цвет слишком белый, то красим иконки statusbar'а в серый цвет
            // инчае возвращаем к дефолтному белому
            if (ColorUtils.calculateLuminance(color).compareTo(DEFAULT_DARK_COLOR_RATIO) != -1) {
                var flags = activity.window.decorView.systemUiVisibility
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                activity.window.decorView.systemUiVisibility = flags
            } else {
                activity.window.decorView.systemUiVisibility = 0
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
fun setStatusBarLightColor(v: View, isLight: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            var flags = v.systemUiVisibility
            flags = if (isLight) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            v.systemUiVisibility = flags
        } catch (e: Exception) {
            logger.e("An exception occurred: ${e.message}", e)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun setDefaultNavigationColor(activity: Activity) {
    setNavigationBarColor(activity,
            getColorFromAttrs(activity.window.context, intArrayOf(R.attr.colorPrimaryDark))
    )
}

/**
 * Красит нав бар в указанный цвет
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun setNavigationBarColor(activity: Activity, @ColorInt color: Int) {
    if (SdkUtils.isAtLeastLollipop()) {
        activity.window.navigationBarColor = color
    }
}

/**
 * Выставить [icon] с фильтром [color] в кач-ве background для [View]
 */
@SuppressLint("ResourceType")
fun setViewBackgroundTint(
        view: View,
        @DrawableRes icon: Int,
        @ColorInt color: Int,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    with(getColoredDrawable(view.context.resources, icon, color, mode)) {
        view.background = this
        return this
    }
}

/**
 * Выставить [icon] с фильтром [color] в кач-ве src для [ImageView]
 */
@SuppressLint("ResourceType")
fun setImageViewTint(
        view: ImageView,
        @DrawableRes icon: Int,
        @ColorInt color: Int,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    with(getColoredDrawable(view.context.resources, icon, color, mode)) {
        view.setImageDrawable(this)
        return this
    }
}

/**
 * Выставить фильтр с [ColorStateList], полученным по [resourceId]
 */
fun setImageViewTintResource(view: ImageView, resourceId: Int) {
    setImageViewTint(view, ContextCompat.getColorStateList(view.context, resourceId))
}

/**
 * Выставить фильтр с цветом [color] для [ImageView]
 */
fun setImageViewTint(view: ImageView, @ColorInt color: Int) {
    setImageViewTint(view, ColorStateList.valueOf(color))
}

/**
 * Выставить цветовой фильтр [ColorStateList] для src в [ImageView]
 */
fun setImageViewTint(
        view: ImageView,
        colorStateList: ColorStateList?,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_ATOP
) {
    if (SdkUtils.isAtLeastLollipop()) {
        view.imageTintList = colorStateList
        view.imageTintMode = mode
    } else {
        view.setColorFilter(colorStateList?.defaultColor ?: Color.TRANSPARENT, mode)
    }
}


fun getImageViewContentSize(imageView: ImageView): Pair<Int, Int> {
    val d = imageView.drawable
    return if (d != null) Pair(d.intrinsicWidth, d.intrinsicHeight) else Pair(0, 0)
}

fun getRescaledImageViewSize(imageView: ImageView): Pair<Int?, Int?> {
    var measuredWidth: Int
    var measuredHeight: Int
    val intrinsicHeight: Int
    val intrinsicWidth: Int
    measuredWidth = imageView.measuredWidth //width of imageView
    measuredHeight = imageView.measuredHeight //height of imageView
    val dSize = getImageViewContentSize(imageView)
    intrinsicWidth = dSize.first //original width of underlying image
    intrinsicHeight = dSize.second //original height of underlying image
    if (intrinsicHeight != 0 && intrinsicWidth != 0) {
        if (measuredHeight / intrinsicHeight <= measuredWidth / intrinsicWidth) {
            measuredWidth = intrinsicWidth * measuredHeight / intrinsicHeight
        } //rescaled width of image within ImageView
        else {
            measuredHeight = intrinsicHeight * measuredWidth / intrinsicWidth
        } //rescaled height of image within ImageView;
    }
    return Pair(measuredWidth, measuredHeight)
}

fun getViewInset(view: View?): Int {
    if (view == null) {
        return 0
    }
    val statusBarHeight = getStatusBarHeight(view.context)
    if (statusBarHeight < 0) {
        return 0
    }
    val dm = view.context.resources.displayMetrics
    if (Build.VERSION.SDK_INT < 21 || view.height == dm.heightPixels || view.height == dm.heightPixels - statusBarHeight) {
        return 0
    }
    try {
        val infoField = View::class.java.getDeclaredField("mAttachInfo")
        infoField.isAccessible = true
        val info = infoField[view]
        if (info != null) {
            val insetsField = info.javaClass.getDeclaredField("mStableInsets")
            insetsField.isAccessible = true
            val insets = insetsField[info] as Rect
            return insets.bottom
        }
    } catch (e: Exception) {
        logger.e("An exception occurred: ${e.message}", e)
    }
    return 0
}

fun getStatusBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        context.resources.getDimensionPixelSize(resourceId)
    } else 0
}

fun getKeyboardHeight(rootView: View, targetView: View): Int {
    val rect = Rect()
    targetView.getWindowVisibleDisplayFrame(rect)
    val usableViewHeight = rootView.height - (if (rect.top != 0) getStatusBarHeight(rootView.context) else 0) - getViewInset(rootView)
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

fun isKeyboardShown(activity: Activity): Boolean {
    return isKeyboardShown(activity.currentFocus)
}

fun isKeyboardShown(view: View?): Boolean {
    if (view == null) {
        return false
    }
    try {
        val inputManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputManager.isActive(view)
    } catch (e: Exception) {
        logger.e("An exception occurred: ${e.message}", e)
        return false
    }
}

fun showKeyboard(activity: Activity?) {
    if (activity != null) {
        showKeyboard(activity.currentFocus)
    }
}

fun showKeyboard(hostView: View?): Boolean {
    return if (hostView != null && hostView.windowToken != null) {
        val imm = hostView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(hostView, InputMethodManager.SHOW_IMPLICIT)
    } else {
        true
    }
}

fun hideKeyboard(activity: Activity?) {
    if (activity != null) {
        hideKeyboard(activity.currentFocus)
    }
}

fun hideKeyboard(hostView: View?): Boolean {
    return hostView != null && hostView.windowToken != null && (hostView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(hostView.windowToken, 0)
}

fun toggleKeyboard(activity: Activity) {
    toggleKeyboard(activity.currentFocus)
}

fun toggleKeyboard(hostView: View?) {
    if (isKeyboardShown(hostView)) {
        hideKeyboard(hostView)
    } else {
        showKeyboard(hostView)
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
fun clearFocus(act: Activity?): Boolean {
    return clearFocus(act?.currentFocus)
}

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

fun getSelectedIndexInRadioGroup(group: RadioGroup): Int {
    val radioButtonId = group.checkedRadioButtonId
    val radioButton = group.findViewById<RadioButton>(radioButtonId) ?: null
    return radioButton?.let { group.indexOfChild(it) } ?: RecyclerView.NO_POSITION
}

fun collapseToolbar(rootLayout: CoordinatorLayout, coordinatorChild: View, appbarLayout: AppBarLayout) {
    var found = false
    for (i in 0 until rootLayout.childCount) {
        if (rootLayout.getChildAt(i) === coordinatorChild) {
            found = true
        }
    }
    require(found) { "view $coordinatorChild is not a child of $rootLayout" }
    val params = coordinatorChild.layoutParams as CoordinatorLayout.LayoutParams
    val behavior = params.behavior as ScrollingViewBehavior?
    behavior?.onNestedFling(rootLayout, appbarLayout, coordinatorChild, 0f, 10000f, true)
}

fun setBottomSheetHideable(dialog: BottomSheetDialog, toggle: Boolean) {
    val behavior: BottomSheetBehavior<*>? = ReflectionUtils.getFieldValue<BottomSheetBehavior<*>, BottomSheetDialog>(BottomSheetDialog::class.java, dialog, "mBehavior")
    behavior?.let {
        it.isHideable = toggle
    }
}

fun getViewSize(view: View): Pair<Int, Int> {
    view.layoutParams.let {
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
    val rotation = windowManager.defaultDisplay.rotation
    when (rotation) {
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
fun calculateMaxTextSize(
        textView: TextView,
        maxWidthTextLength: Int,
        maxTextSize: Float,
        maxTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
        measuredViewWidth: Int = 0
): Float {
    val text = StringBuilder()
    for (i in 0 until maxWidthTextLength) {
        text.append("0")
    }
    return calculateMaxTextSize(textView, text.toString(), maxTextSize, maxTextSizeUnit, measuredViewWidth)
}

@JvmOverloads
fun calculateMaxTextSize(
        textView: TextView,
        maxWidthText: CharSequence,
        maxTextSize: Float,
        maxTextSizeUnit: Int = TypedValue.COMPLEX_UNIT_DIP,
        measuredViewWidth: Int = 0
): Float {
    if (maxWidthText.isEmpty() || maxTextSize <= 0) return maxTextSize
    val measuredWidth = if (measuredViewWidth <= 0) textView.width else measuredViewWidth
    if (measuredWidth == 0) return maxTextSize

    val maxWidthString = maxWidthText.toString()
    var resultSize = maxTextSize

    val paint = Paint()
    paint.typeface = textView.typeface

    paint.textSize = convertAnyToPx(resultSize, maxTextSizeUnit, textView.context)
    var measureText = paint.measureText(maxWidthString)

    while (resultSize > 0 && measureText > measuredWidth) {
        resultSize--
        paint.textSize = convertAnyToPx(resultSize, maxTextSizeUnit, textView.context)
        measureText = paint.measureText(maxWidthString)
    }
    if (resultSize == 0f) {
        resultSize = maxTextSize
    }
    return resultSize
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
                clearInputError(l)
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
                // user has input more than limited - lets do something
// about that
                if (editTextRowCount >= linesLimit) { // find the last break
                    val lastBreakIndex = text.lastIndexOf("\n")
                    // compose new text
                    val newText = text.substring(0, lastBreakIndex)
                    // add new text - delete old one and append new one
// (append because I want the cursor to be at the end)
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