package net.maxsmr.commonutils.gui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw

private const val POPUP_HEIGHT_CORRECTION = 24f

/**
 * @param onDismissed - true, если дальнейший показ не требуется
 * @return показанный с 0-ой высотой [PopupWindow]
 */
fun showPopupWindowWithObserver(
        context: Context,
        currentPopup: PopupWindow?,
        params: PopupParams,
        contentViewCreator: (View) -> View
): PopupWindow? = showPopupWindow(
        context,
        currentPopup,
        params
) { listener ->
    contentViewCreator(params.anchorView).apply { doOnPreDraw(listener::onViewReady) }
}

fun showPopupWindow(
        context: Context,
        currentPopup: PopupWindow?,
        params: PopupParams,
        contentViewCreator: (ViewReadyListener) -> View,
): PopupWindow? {
    with(params) {
        var popup = currentPopup
        if (popup != null && params.dismissIfShowed) {
            popup.dismiss()
            if (onDismissed?.invoke(popup) == true) {
                return null
            }
        }
        val listener = object : ViewReadyListener {
            override fun onViewReady(view: View) {
                popup?.let {
                    // Костыль. Нужно знать высоту попапа перед показом для определения правильной позиции,
                    // но по какой-то причине она определяется некорректно (при вызове measure и обращении к measuredWidth, measuredHeight)
                    // поэтому быстро показываем его дважды: после первого показа забираем корректные ширину и высоту,
                    // и показываем еще раз уже в нужном месте
                    it.show(anchorView, view.height, gravity)
                    onShowed?.invoke(it)
                    popup?.setOnDismissListener {
                        onDismissed?.invoke(it)
                    }
                }
            }
        }
        popup = createPopupWindow(context, contentViewCreator(listener), windowConfigurator)
        // первый показ, когда высота неизвестна
        popup.show(anchorView, 0, gravity)
        return popup
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

private fun createPopupWindow(
        context: Context,
        contentView: View,
        configurator: (PopupWindow, Context) -> Unit
): PopupWindow = with(PopupWindow(context)) {
    configurator(this, context)
    this.contentView = contentView
    return this
}

interface ViewReadyListener {

    fun onViewReady(view: View)
}

class PopupParams @JvmOverloads constructor(
        val anchorView: View,
        val gravity: Int = Gravity.TOP,
        val dismissIfShowed: Boolean = true,
        val onDismissed: ((PopupWindow) -> Boolean)? = null,
        val onShowed: ((PopupWindow) -> Unit)? = null,
        windowConfigurator: ((PopupWindow, Context) -> Unit)? = null
) {

    val windowConfigurator = windowConfigurator ?: defaultWindowConfigurator()

    private fun defaultWindowConfigurator(): (PopupWindow, Context) -> Unit = { window, context ->
        with(window) {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)))
            isFocusable = true
            isTouchable = true
            isOutsideTouchable = true
        }
    }
}