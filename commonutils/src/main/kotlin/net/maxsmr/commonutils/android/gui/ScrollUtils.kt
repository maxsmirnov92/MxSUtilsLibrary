package net.maxsmr.commonutils.android.gui

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import net.maxsmr.commonutils.android.gui.RecyclerScrollState.*
import net.maxsmr.commonutils.android.gui.ScrollState.*
import net.maxsmr.commonutils.android.gui.ScrollState.UNKNOWN

const val SCROLL_ANIMATION_DURATION = 400L
const val ALL_STATES = 10 // Все состояния

/**
 * Запрашиваем проскрол для отображения вью
 * @return Whether any parent scrolled.
 */
fun View.requestScrollOnScreen() = post {
    val rect = Rect(0, 0, width, height)
    requestRectangleOnScreen(rect, false)
}

/**
 * Определяет возможность скролла у [RecyclerView] с [LinearLayoutManager]
 * @param isFromStart смотреть от начала
 */
@JvmOverloads
fun RecyclerView.isScrollable(isFromStart: Boolean = false): Boolean? {
    val layoutManager = this.layoutManager as? LinearLayoutManager ?: return null
    val adapter = this.adapter ?: return null
    return layoutManager.findLastCompletelyVisibleItemPosition() < adapter.itemCount - 1
            && (isFromStart.not() || layoutManager.findFirstCompletelyVisibleItemPosition() == 0)
}

@JvmOverloads
fun RecyclerView.setOnScrollChangesListener(
        layoutManager: LinearLayoutManager,
        controlState: Int = ALL_STATES,
        listener: ((RecyclerScrollState) -> Unit)
) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {

        var previousFirstVisiblePosition: Int? = null

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            val currentPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
            if (previousFirstVisiblePosition == null) {
                previousFirstVisiblePosition = currentPosition
            }
            previousFirstVisiblePosition?.let {
                if (controlState == ALL_STATES || newState == controlState) {
                    val state = when {
                        currentPosition == 0 -> FIRST
                        layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.itemCount - 1 -> LAST
                        currentPosition > it -> NEXT
                        currentPosition < it -> PREVIOUS
                        else -> RecyclerScrollState.UNKNOWN
                    }
                    listener.invoke(state)
                }
            }
            previousFirstVisiblePosition = currentPosition
        }
    })
}

@TargetApi(Build.VERSION_CODES.M)
fun ScrollView.setOnScrollChangesListener(listener: ((ScrollState) -> Unit)) {
    setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
        listener.invoke(detectScrollChangesByParams(scrollX, scrollY, oldScrollX, oldScrollY))
    }
}

@TargetApi(Build.VERSION_CODES.M)
fun NestedScrollView.setOnScrollChangesListener(listener: ((ScrollState) -> Unit)) {
    setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
        listener.invoke(detectScrollChangesByParams(scrollX, scrollY, oldScrollX, oldScrollY))
    }
}

@JvmOverloads
fun ScrollView.scrollToView(
        activity: Activity?,
        target: View,
        isVertically: Boolean,
        smoothScroll: Boolean = true
) {
    val x: Int
    val y: Int
    val coords = target.getBoundsByParent(this)
    if (isVertically) {
        x = 0
        y = coords.top
    } else {
        x = coords.left
        y = 0
    }
    scrollTo(activity, x, y, smoothScroll)
}

/**
 * Скролл в указанную позицию (x, y) [ScrollView]
 */
@JvmOverloads
fun ScrollView.scrollTo(
        activity: Activity?,
        x: Int,
        y: Int,
        smoothScroll: Boolean = true,
        changeFocus: Boolean = false
) {
    if (changeFocus) {
        // если не очистить текущий фокус,
        // может не сработать
        activity.clearFocus()
        fullScroll(View.FOCUS_DOWN)
    }
    if (smoothScroll) {
        smoothScrollTo(x, y)
    } else {
        scrollTo(x, y)
    }
    if (changeFocus) {
        parent.requestChildFocus(this, this)
    }
}

/**
 * Скролл в указанную позицию [RecyclerView]
 */
@JvmOverloads
fun RecyclerView.scrollTo(position: Int, smoothScroll: Boolean = true) {
    if (position != RecyclerView.NO_POSITION) {
        if (smoothScroll) {
            smoothScrollToPosition(position)
        } else {
            scrollToPosition(position)
        }
    }
}

/**
 * При неполностью раскрытом состоянии [AppBarLayout]
 * прокрутить на указанный [offset] с анимацией или без
 */
@JvmOverloads
fun AppBarLayout.scrollByOffset(offset: Int, animationDuration: Long = SCROLL_ANIMATION_DURATION) {
    val params = layoutParams as CoordinatorLayout.LayoutParams
    val behavior = params.behavior as AppBarLayout.Behavior?
    behavior?.let {
        val offsetLambda: ((Int) -> Unit) = { offset ->
            behavior.topAndBottomOffset = offset
            requestLayout()
        }
        if (animationDuration > 0) {
            val valueAnimator = ValueAnimator.ofInt()
            valueAnimator.interpolator = DecelerateInterpolator()
            valueAnimator.addUpdateListener { animation ->
                offsetLambda(animation.animatedValue as Int)
            }
            valueAnimator.setIntValues(0, offset)
            valueAnimator.duration = animationDuration
            valueAnimator.start()
        } else {
            offsetLambda(offset)
        }
    }
}

private fun ViewGroup.detectScrollChangesByParams(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int): ScrollState =
        when {
            scrollX == (getChildAt(0).measuredWidth - measuredWidth) && scrollX != oldScrollX -> END
            scrollX > oldScrollX -> RIGHT
            scrollX < oldScrollX -> LEFT
            scrollX == 0 && oldScrollX != 0 -> START
            scrollY == (getChildAt(0).measuredHeight - measuredHeight) && scrollY != oldScrollY -> BOTTOM
            scrollY < oldScrollY -> UP
            scrollY > oldScrollY -> DOWN
            scrollY == 0 && oldScrollY != 0 -> TOP
            else -> UNKNOWN
        }

enum class ScrollState {
    // горизонтальный:
    LEFT,
    RIGHT,
    START,
    END,
    // вертикальный:
    DOWN,
    UP,
    TOP,
    BOTTOM,
    UNKNOWN
}

enum class RecyclerScrollState {
    FIRST, LAST, NEXT, PREVIOUS, UNKNOWN
}