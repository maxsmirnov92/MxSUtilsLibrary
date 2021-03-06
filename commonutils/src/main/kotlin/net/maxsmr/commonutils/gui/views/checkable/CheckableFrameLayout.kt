package net.maxsmr.commonutils.gui.views.checkable

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

open class CheckableFrameLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ICheckableView {

    override var _isChecked: Boolean = false

    override fun mergeDrawableStates(baseState: IntArray, additionalState: IntArray) {
        View.mergeDrawableStates(baseState, additionalState)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray =
            mergeCheckedDrawableState(super.onCreateDrawableState(extraSpace + 1))
}