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
fun requestScrollOnScreen(view: View) =
        view.post {
            val rect = Rect(0, 0, view.width, view.height)
            view.requestRectangleOnScreen(rect, false)
        }

/**
 * Определяет возможность скролла у [RecyclerView] с [LinearLayoutManager]
 * @param isFromStart смотреть от начала
 */
fun isScrollable(view: RecyclerView, isFromStart: Boolean = false): Boolean? {
    val layoutManager = view.layoutManager as? LinearLayoutManager ?: return null
    val adapter = view.adapter ?: return null
    return layoutManager.findLastCompletelyVisibleItemPosition() < adapter.itemCount - 1
            && (isFromStart.not() || layoutManager.findFirstCompletelyVisibleItemPosition() == 0)
}

fun setOnScrollChangesListener(
        view: RecyclerView,
        layoutManager: LinearLayoutManager,
        controlState: Int = ALL_STATES,
        listener: ((RecyclerScrollState) -> Unit)
) {
    view.addOnScrollListener(object : RecyclerView.OnScrollListener() {

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
fun setOnScrollChangesListener(view: ScrollView, listener: ((ScrollState) -> Unit)) {
    view.setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
        listener.invoke(detectScrollChangesByParams(view, scrollX, scrollY, oldScrollX, oldScrollY))
    }
}

@TargetApi(Build.VERSION_CODES.M)
fun setOnScrollChangesListener(view: NestedScrollView, listener: ((ScrollState) -> Unit)) {
    view.setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
        listener.invoke(detectScrollChangesByParams(view, scrollX, scrollY, oldScrollX, oldScrollY))
    }
}

/**
 * Скролл в указанную позицию (x, y) [ScrollView]
 */
fun scrollTo(
        view: ScrollView,
        activity: Activity?,
        x: Int,
        y: Int,
        smoothScroll: Boolean = true
) {
    // если не очистить текущий фокус,
    // может не сработать
    clearFocus(activity)
    view.fullScroll(View.FOCUS_DOWN)
    if (smoothScroll) {
        view.smoothScrollTo(x, y)
    } else {
        view.scrollTo(x, y)
    }
    view.parent.requestChildFocus(view, view)
}

/**
 * Скролл в указанную позицию [RecyclerView]
 */
fun scrollTo(
        view: RecyclerView,
        position: Int,
        smoothScroll: Boolean = true
) {
    if (position != RecyclerView.NO_POSITION) {
        if (smoothScroll) {
            view.smoothScrollToPosition(position)
        } else {
            view.scrollToPosition(position)
        }
    }
}

/**
 * При неполностью раскрытом состоянии [AppBarLayout]
 * прокрутить на указанный [offset] с анимацией или без
 */
fun scrollByOffset(layout: AppBarLayout, offset: Int, animationDuration: Long = SCROLL_ANIMATION_DURATION) {
    val params = layout.layoutParams as CoordinatorLayout.LayoutParams
    val behavior = params.behavior as AppBarLayout.Behavior?
    behavior?.let {
        val offsetLambda: ((Int) -> Unit) = { offset ->
            behavior.topAndBottomOffset = offset
            layout.requestLayout()
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

private fun detectScrollChangesByParams(v: ViewGroup, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int): ScrollState =
        when {
            scrollX == (v.getChildAt(0).measuredWidth - v.measuredWidth) && scrollX != oldScrollX -> END
            scrollX > oldScrollX -> RIGHT
            scrollX < oldScrollX -> LEFT
            scrollX == 0 && oldScrollX != 0 -> START
            scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight) && scrollY != oldScrollY -> BOTTOM
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