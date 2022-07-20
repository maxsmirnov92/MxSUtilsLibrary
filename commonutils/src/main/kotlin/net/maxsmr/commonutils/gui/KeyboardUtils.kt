package net.maxsmr.commonutils.gui

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("KeyboardUtils")

fun getKeyboardHeight(rootView: View, targetView: View): Int {
    val rect = Rect()
    targetView.getWindowVisibleDisplayFrame(rect)
    val usableViewHeight =
        rootView.height - (if (rect.top != 0) rootView.context.resources.getStatusBarHeight() else 0) - rootView.getViewInset()
    return usableViewHeight - (rect.bottom - rect.top)
}

/**
 * Добавление слушателя на состояние клавиатуры
 *
 * @param rootView корневай [View] на экране
 */
fun addSoftInputStateListener(
    rootView: View,
    keyboardVisibilityChangeListener: (Boolean) -> Unit,
): ViewTreeObserver.OnGlobalLayoutListener {
    ViewTreeObserver.OnGlobalLayoutListener {
        val insets = ViewCompat.getRootWindowInsets(rootView)
        insets?.let {
            keyboardVisibilityChangeListener(insets.isVisible(WindowInsetsCompat.Type.ime()))
        }
    }.let { listener ->
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        return listener
    }
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
        hostView.requestFocus()
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
            hostView.clearFocus()
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

/**
 * Запросить фокус или показать клавиатуру в зав-ти от состояния view
 */
@JvmOverloads
fun toggleFocusOrKeyboardState(view: View, activity: Activity, toggle: Boolean = true) {
    if (toggle) {
        if (!view.isFocused) {
            view.requestFocus()
        } else {
            showKeyboard(view)
        }
    } else {
        hideKeyboard(activity)
        // очистка фокуса не убирает клавиатуру
        activity.clearFocus()
    }
}