package net.maxsmr.commonutils.gui.views.checkable

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

open class CheckableLinearLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ICheckableView {

    override var internalChecked: Boolean = false

    override fun mergeDrawableStates(baseState: IntArray, additionalState: IntArray) {
        View.mergeDrawableStates(baseState, additionalState)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray =
            mergeWithCheckedDrawableState(super.onCreateDrawableState(extraSpace + 1))
}