package net.maxsmr.commonutils.gui.listeners

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import kotlin.math.abs

abstract class AppBarStateChangeListener : OnOffsetChangedListener {

    var currentState = State.IDLE
        private set(value) {
            if (value != field) {
                field = value
                onStateChanged(value)
            }
        }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        currentState = when {
            i == 0 -> {
                State.EXPANDED
            }
            abs(i) >= appBarLayout.totalScrollRange -> {
                State.COLLAPSED
            }
            else -> {
                State.IDLE
            }
        }
    }

    abstract fun onStateChanged(state: State)

    enum class State {
        EXPANDED, COLLAPSED, IDLE
    }
}