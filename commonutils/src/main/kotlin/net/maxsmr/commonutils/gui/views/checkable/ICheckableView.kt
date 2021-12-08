package net.maxsmr.commonutils.gui.views.checkable

import android.widget.Checkable

/**
 * This is a simple wrapper for any layout that implements the [Checkable]
 * interface by keeping an internal 'checked' state flag.
 *
 * This can be used as the root view for a custom list item layout for
 * [android.widget.AbsListView] elements with a setChoiceMode set.
 */
interface ICheckableView : Checkable {

    var internalChecked: Boolean

    fun refreshDrawableState()

    fun mergeDrawableStates(baseState: IntArray, additionalState: IntArray)

    override fun isChecked(): Boolean = internalChecked

    override fun setChecked(b: Boolean) {
        if (b != internalChecked) {
            internalChecked = b
            refreshDrawableState()
        }
    }

    override fun toggle() {
        isChecked = !isChecked
    }

    fun mergeWithCheckedDrawableState(states: IntArray): IntArray {
        if (isChecked) {
            mergeDrawableStates(states, CHECKED_STATE_ARRAY)
        }
        return states
    }

    companion object {

        private val CHECKED_STATE_ARRAY = intArrayOf(android.R.attr.state_checked)
    }
}