package net.maxsmr.commonutils.gui

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.maxsmr.commonutils.getStatusBarHeight
import net.maxsmr.commonutils.isAtLeastR
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("KeyboardUtils")

fun getKeyboardHeight(rootView: View, targetView: View): Int {
    val rect = Rect()
    targetView.getWindowVisibleDisplayFrame(rect)
    val usableViewHeight =
        rootView.height - (if (rect.top != 0) {
            rootView.context.resources.getStatusBarHeight()
        } else {
            0
        }) - rootView.getViewInset()
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

fun Activity.isKeyboardShown(): Boolean =
    currentFocus?.isKeyboardShown() ?: false

fun View.isKeyboardShown(): Boolean {
    try {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager? ?: return false
        return inputManager.isActive(this)
    } catch (e: Exception) {
        logger.e("An exception occurred: ${e.message}", e)
        return false
    }
}

@JvmOverloads
fun Activity.showKeyboard(
    flags: Int = InputMethodManager.SHOW_IMPLICIT
): Boolean =
    currentFocus?.showKeyboard(flags, false) ?: false

@JvmOverloads
fun View.showKeyboard(
    flags: Int = InputMethodManager.SHOW_IMPLICIT,
    requestFocus: Boolean = true
): Boolean {
    if (requestFocus) {
        requestFocus()
    }
    if (isAtLeastR()) {
        windowInsetsController?.show(WindowInsetsCompat.Type.ime())
    }
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        ?: return false
    return imm.showSoftInput(this, flags)
}

@JvmOverloads
fun Activity.hideKeyboard(
    flags: Int = 0,
    clearFocus: Boolean = true
) = currentFocus?.hideKeyboard(flags, clearFocus) ?: false

@JvmOverloads
fun View.hideKeyboard(
    flags: Int = 0,
    clearFocus: Boolean = true
): Boolean {
    if (clearFocus) {
        this.clearFocus()
    }
    if (isAtLeastR()) {
        this.windowInsetsController?.hide(WindowInsetsCompat.Type.ime())
    }
    return this.windowToken?.let {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            ?: return false
        imm.hideSoftInputFromWindow(it, flags)
    } ?: false
}

@JvmOverloads
fun Activity.toggleKeyboard(toggleFocus: Boolean = true) {
    currentFocus?.toggleKeyboard(toggleFocus) ?: false
}

@JvmOverloads
fun View.toggleKeyboard(toggleFocus: Boolean = true) {
    if (isKeyboardShown()) {
        hideKeyboard(clearFocus = toggleFocus)
    } else {
        showKeyboard(requestFocus = toggleFocus)
    }
}

/**
 * Запросить фокус или показать клавиатуру в зав-ти от состояния [view]
 */
@JvmOverloads
fun Activity.toggleFocusOrKeyboardState(view: View, toggle: Boolean = true) {
    if (toggle) {
        if (!view.isFocused) {
            view.requestFocus()
        } else {
            view.showKeyboard()
        }
    } else {
        hideKeyboard()
        // очистка фокуса не убирает клавиатуру
        clearFocus()
    }
}