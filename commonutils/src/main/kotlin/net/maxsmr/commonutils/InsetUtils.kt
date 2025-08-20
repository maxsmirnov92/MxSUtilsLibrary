package net.maxsmr.commonutils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat.getRootWindowInsets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import net.maxsmr.commonutils.gui.isKeyboardVisible
import kotlin.math.max

fun Activity.getWindowInsetsCompat(): WindowInsetsCompat {
    return if (isAtLeastR()) {
        WindowInsetsCompat.toWindowInsetsCompat(
            windowManager.currentWindowMetrics.windowInsets
        )
    } else {
        window.decorView.getRootWindowInsetsCompat()
    }
}

fun View.getRootWindowInsetsCompat(): WindowInsetsCompat {
    return getRootWindowInsets(this) ?: WindowInsetsCompat.Builder().build()
}

fun Activity.getKeyboardHeight(): Int {
    return getWindowInsetsCompat().getKeyboardHeight()
}

fun Activity.getStatusBarHeight(): Int {
    return getWindowInsetsCompat().getStatusBarHeight()
}

fun Activity.getNavigationBarsHeight(): Int {
    return getWindowInsetsCompat().getNavigationBarsHeight()
}

fun View.getKeyboardHeight(): Int {
    return getRootWindowInsetsCompat().getKeyboardHeight()
}

fun View.getStatusBarHeight(): Int {
    return getRootWindowInsetsCompat().getStatusBarHeight()
}

fun View.getNavigationBarsHeight(): Int {
    return getRootWindowInsetsCompat().getNavigationBarsHeight()
}

fun WindowInsetsCompat.getKeyboardHeight(): Int {
    return if (isKeyboardVisible()) {
        val navigationBarsHeight = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val imeHeight = getInsets(WindowInsetsCompat.Type.ime()).bottom
        max(imeHeight - navigationBarsHeight, 0)
    } else {
        0
    }
}

fun WindowInsetsCompat.getStatusBarHeight(): Int {
    return getInsets(WindowInsetsCompat.Type.statusBars()).top
}

fun WindowInsetsCompat.getNavigationBarsHeight(): Int {
    return getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
}

fun WindowInsetsCompat.getHorizontalInsetsSize(@InsetsType mask: Int): Int {
    return getInsets(mask).let {
        it.left + it.right
    }
}

fun WindowInsetsCompat.getVerticalInsetsSize(@InsetsType mask: Int): Int {
    return getInsets(mask).let {
        it.top + it.bottom
    }
}

fun Fragment.setAppearanceLightBars(isLight: Boolean) {
    val controller = getWindowInsetsControllerCompat()
    controller.isAppearanceLightStatusBars = isLight
    controller.isAppearanceLightNavigationBars = isLight
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        requireActivity().window?.insetsController?.setAppearanceLightNavigationBars()
//    }
}

fun Activity.setAppearanceLightBars(isLight: Boolean) {
    val controller = getWindowInsetsControllerCompat()
    controller.isAppearanceLightStatusBars = isLight
    controller.isAppearanceLightNavigationBars = isLight
    //    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        window?.insetsController?.setAppearanceLightNavigationBars()
//    }
}

fun Fragment.getWindowInsetsControllerCompat() =
    WindowCompat.getInsetsController(
        if (this is DialogFragment) {
            dialog?.window
        } else {
            null
        } ?: requireActivity().window,
        requireView())

fun Activity.getWindowInsetsControllerCompat() =
    WindowCompat.getInsetsController(window, window.decorView)

@RequiresApi(Build.VERSION_CODES.R)
private fun WindowInsetsController.setAppearanceLightNavigationBars() {
    setSystemBarsAppearance(
        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS or WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS or WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
    )
}