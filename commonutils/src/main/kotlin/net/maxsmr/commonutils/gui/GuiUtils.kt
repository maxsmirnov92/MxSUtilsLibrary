package net.maxsmr.commonutils.gui

import android.app.Activity
import android.os.CountDownTimer
import android.util.Rational
import android.util.Size
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import net.maxsmr.commonutils.getColorFromAttrs
import net.maxsmr.commonutils.getDisplaySize
import net.maxsmr.commonutils.isAtLeastLollipop
import net.maxsmr.commonutils.isAtLeastMarshmallow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_DARK_COLOR_RATIO = 0.7

private const val RATIO_4_3_VALUE = 4.0 / 3.0
private const val RATIO_16_9_VALUE = 16.0 / 9.0

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("GuiUtils")

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


@Deprecated("")
fun Activity.setDefaultStatusBarColor() {
    setStatusBarColor(getColorFromAttrs(intArrayOf(android.R.attr.colorPrimaryDark)))
}

/**
 * Красит статус бар в указанный цвет.
 * Так же, красит иконки статус бара в серый цвет, если SDK >= 23 и устанавливаемый цвет слишком белый
 */
@Deprecated("")
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



fun Activity.setDefaultNavigationColor() {
    setNavigationBarColor(window.context.getColorFromAttrs(intArrayOf(android.R.attr.colorPrimaryDark)))
}

/**
 * Красит нав бар в указанный цвет
 */
fun Activity.setNavigationBarColor(@ColorInt color: Int) {
    if (isAtLeastLollipop()) {
        window.navigationBarColor = color
    }
}

@Deprecated("")
fun Window.showAboveLockscreen(window: Window, wakeScreen: Boolean) {
    toggleAboveLockscreen(wakeScreen, true)
}

@Deprecated("")
fun Window.hideAboveLockscreen(window: Window, wakeScreen: Boolean) {
    toggleAboveLockscreen(wakeScreen, false)
}

@Deprecated("")
private fun Window.toggleAboveLockscreen(wakeScreen: Boolean, toggle: Boolean) {
    var flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
    if (wakeScreen) {
        flags = flags or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    }
    if (toggle) {
        addFlags(flags)
    } else {
        clearFlags(flags)
    }
}

/**
 * @return true if focus cleared, false otherwise
 */
fun Activity?.clearFocus(): Boolean =
        this?.currentFocus.clearFocusWithCheck()

fun Activity.getDisplayStandardAspectRatio(): StandardAspectRatio {
    return getStandardAspectRatio(getDisplaySize())
}

fun getStandardAspectRatio(size: Size): StandardAspectRatio {
    val previewRatio = max(size.width, size.height).toDouble() / min(size.width, size.height)
    return  if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
        StandardAspectRatio._4_3
    } else {
        StandardAspectRatio._16_9
    }
}

enum class StandardAspectRatio {
    _4_3,
    _16_9;

    val value: Rational
        get() = when(this) {
            _4_3 -> Rational(4, 3)
            _16_9 -> Rational(16, 9)
        }
}

fun getAspectRatio(size: Size): Rational {

    // НОД
    fun gcd(a: Int, b: Int): Int {
        var result: Int = a
        var divider: Int = b
        while (divider != 0) {
            val temp = divider
            divider = result % divider
            result = temp
        }
        return result
    }

    val gcd = gcd(size.width, size.height)
    return Rational(size.width / gcd, size.height / gcd)
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

/**
 * @return одно из возможных значений из 0, 90, 180, 270 как наиболее близкое
 */
fun getCorrectedDisplayRotation(rotation: Int): Int {
    val correctedRotation = if (rotation >= 360) rotation % 360 else rotation
    var result = DiffOrientationEventListener.ROTATION_NOT_SPECIFIED
    when (correctedRotation) {
        in 315..359, in 0..44 -> {
            result = 0
        }
        in 45..134 -> {
            result = 90
        }
        in 135..224 -> {
            result = 180
        }
        in 225..314 -> {
            result = 270
        }
    }
    return result
}

fun getRotationDegrees(surfaceRotation: Int): Int {
    return when (surfaceRotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

fun getSurfaceRotation(rotationDegrees: Int): Int {
    return when (rotationDegrees) {
        0 -> Surface.ROTATION_0
        90 -> Surface.ROTATION_90
        180 -> Surface.ROTATION_180
        270 -> Surface.ROTATION_270
        else -> 0
    }
}