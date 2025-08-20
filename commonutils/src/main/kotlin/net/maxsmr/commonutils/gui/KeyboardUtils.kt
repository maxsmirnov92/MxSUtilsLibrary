package net.maxsmr.commonutils.gui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowInsetsCompat
import net.maxsmr.commonutils.getRootWindowInsetsCompat
import net.maxsmr.commonutils.getWindowInsetsCompat
import net.maxsmr.commonutils.isAtLeastR
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("KeyboardUtils")

fun Activity.addSoftInputStateListener(
    onVisibilityChange: (Boolean) -> Unit
): ViewTreeObserver.OnGlobalLayoutListener =
    window.decorView.addSoftInputStateListener(onVisibilityChange)

/**
 * Добавление слушателя на состояние клавиатуры
 *
 * [this] корневая [View] на экране
 */
fun View.addSoftInputStateListener(
    onVisibilityChange: (Boolean) -> Unit
): ViewTreeObserver.OnGlobalLayoutListener {
    var lastState: Boolean? = null
    return ViewTreeObserver.OnGlobalLayoutListener {
        val state = isKeyboardVisible()
        if (lastState != state) {
            onVisibilityChange(state)
            lastState = state
        }
    }.also {
        viewTreeObserver.addOnGlobalLayoutListener(it)
    }
}

fun Activity.isKeyboardVisible(): Boolean =
    getWindowInsetsCompat().isVisible(WindowInsetsCompat.Type.ime())

fun View.isKeyboardVisible(): Boolean =
    getRootWindowInsetsCompat().isKeyboardVisible()

fun WindowInsetsCompat.isKeyboardVisible(): Boolean =
    isVisible(WindowInsetsCompat.Type.ime())

fun Activity.isKeyboardShown(): Boolean =
    currentFocus?.isKeyboardShown() ?: false

fun View.isKeyboardShown(): Boolean {
    try {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager? ?: return false
        return inputManager.isActive(this)
    } catch (e: Exception) {
        logger.e(formatException(e))
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
        windowInsetsController?.let {
            it.show(WindowInsetsCompat.Type.ime())
            return true
        }
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
        clearFocus()
    }
    if (isAtLeastR()) {
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.ime())
            return true
        }
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