package net.maxsmr.commonutils.gui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.PointF
import android.os.Build
import android.os.CountDownTimer
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.getColorFromAttrs
import net.maxsmr.commonutils.isAtLeastLollipop
import net.maxsmr.commonutils.isAtLeastMarshmallow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.util.concurrent.TimeUnit

private const val DEFAULT_DARK_COLOR_RATIO = 0.7

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

fun Resources.getStatusBarHeight(): Int {
    val resourceId = getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        getDimensionPixelSize(resourceId)
    } else 0
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
 * @return true if focus cleared, false otherwise
 */
fun Activity?.clearFocus(): Boolean =
        this?.currentFocus.clearFocusWithCheck()

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

fun getCurrentDisplayOrientation(context: Context): Int {
    return when (context.display?.rotation) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
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

fun getFixedSize(sourceSize: Pair<Int, Int>, maxSize: Int): PointF =
        getFixedSize(sourceSize.first, sourceSize.second, maxSize)

fun getFixedSize(
        sourceWidth: Int,
        sourceHeight: Int,
        maxSize: Int
): PointF {
    var w = sourceWidth.toFloat()
    var h = sourceHeight.toFloat()
    if (w > maxSize || h > maxSize) {
        if (w > h) {
            h = h * maxSize / w
            w = maxSize.toFloat()
        } else {
            w = w * maxSize / h
            h = maxSize.toFloat()
        }
    }
    return PointF(w, h)
}