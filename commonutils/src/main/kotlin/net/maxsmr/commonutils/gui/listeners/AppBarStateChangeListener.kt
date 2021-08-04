package net.maxsmr.commonutils.gui.listeners

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener

abstract class AppBarStateChangeListener : OnOffsetChangedListener {

    var currentState = State.IDLE
        private set

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        currentState = when {
            i == 0 -> {
                if (currentState != State.EXPANDED) {
                    onStateChanged(appBarLayout, State.EXPANDED)
                }
                State.EXPANDED
            }
            Math.abs(i) >= appBarLayout.totalScrollRange -> {
                if (currentState != State.COLLAPSED) {
                    onStateChanged(appBarLayout, State.COLLAPSED)
                }
                State.COLLAPSED
            }
            else -> {
                if (currentState != State.IDLE) {
                    onStateChanged(appBarLayout, State.IDLE)
                }
                State.IDLE
            }
        }
    }

    abstract fun onStateChanged(appBarLayout: AppBarLayout, state: State)

    enum class State {
        EXPANDED, COLLAPSED, IDLE
    }
}