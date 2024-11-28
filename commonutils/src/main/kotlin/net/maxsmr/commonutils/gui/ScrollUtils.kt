package net.maxsmr.commonutils.gui

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.maxsmr.commonutils.gui.RecyclerScrollState.*
import net.maxsmr.commonutils.gui.ScrollState.*
import net.maxsmr.commonutils.gui.ScrollState.UNKNOWN

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
fun RecyclerView.addOnScrollStateListener(
    layoutManager: LinearLayoutManager,
    controlState: Int = ALL_STATES,
    stateListener: ((RecyclerScrollState, Int) -> Unit)
): RecyclerView.OnScrollListener {
    val listener = object : RecyclerView.OnScrollListener() {

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
                    stateListener.invoke(state, currentPosition)
                }
            }
            previousFirstVisiblePosition = currentPosition
        }
    }
    addOnScrollListener(listener)
    return listener
}

fun RecyclerView.addFloatingActionButtonScrollListener(
    fab: FloatingActionButton,
    showDelay: Long = 0,
    shouldHideFunc: ((Int) -> Boolean)? = null,
): RecyclerView.OnScrollListener {
    val listener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (showDelay <= 0) {
                    fab.show()
                } else {
                    fab.postDelayed({
                        fab.show()
                    }, showDelay)
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (shouldHideFunc?.invoke(dy) != false && (dy > 0 || dy < 0)) {
                fab.hide()
            }
        }
    }
    addOnScrollListener(listener)
    return listener
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

fun ViewGroup.calculateCoordsForScroll(
    target: View,
    isVertically: Boolean,
    alignToBottomOrRight: Boolean = false,
): Point? {
    val coords = target.getOffsetByParent(this) ?: return null

    val x: Int
    val y: Int

    if (isVertically) {
        if (alignToBottomOrRight) {
            // Учитываем высоту NestedScrollView и высоту контента
            val scrollViewHeight = height
            val contentHeight = getChildAt(0)?.height ?: 0
            val targetBottom = coords.bottom

            // Рассчитываем позицию, чтобы target был внизу
            y = (targetBottom - scrollViewHeight).coerceAtMost(contentHeight - scrollViewHeight)
        } else {
            y = coords.top
        }
        x = 0
    } else {
        if (alignToBottomOrRight) {
            // Учитываем ширину NestedScrollView и ширину контента
            val scrollViewWidth = width
            val contentWidth = getChildAt(0)?.width ?: 0
            val targetRight = coords.right

            // Рассчитываем позицию, чтобы target был справа
            x = (targetRight - scrollViewWidth).coerceAtMost(contentWidth - scrollViewWidth)
        } else {
            x = coords.left
        }
        y = 0
    }
    return Point(x, y)
}

@JvmOverloads
fun ViewGroup.scrollToView(
    target: View,
    isVertically: Boolean,
    alignToBottomOrRight: Boolean = false,
    smoothScroll: Boolean = true,
    changeFocus: Boolean = false,
    activity: Activity? = null,
) {
    val coords = calculateCoordsForScroll(target, isVertically, alignToBottomOrRight) ?: return
    scrollTo(coords.x, coords.y, smoothScroll, changeFocus, activity)
}

/**
 * Скролл в указанную позицию (x, y) [ScrollView]
 */
@JvmOverloads
fun ViewGroup.scrollTo(
    x: Int,
    y: Int,
    smoothScroll: Boolean = true,
    changeFocus: Boolean = false,
    activity: Activity? = null,
) {
    if (changeFocus) {
        // если не очистить текущий фокус,
        // может не сработать
        activity?.currentFocus?.clearFocus()
        when (this) {
            is ScrollView -> {
                fullScroll(View.FOCUS_DOWN)
            }
            is HorizontalScrollView -> {
                fullScroll(View.FOCUS_DOWN)
            }
            is NestedScrollView -> {
                fullScroll(View.FOCUS_DOWN)
            }
        }

    }
    if (smoothScroll) {
        when (this) {
            is ScrollView -> {
                smoothScrollTo(x, y)
            }
            is HorizontalScrollView -> {
                smoothScrollTo(x, y)
            }
            is NestedScrollView -> {
                smoothScrollTo(x, y)
            }
        }
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
        scrollX == 0 -> START
        scrollY == (getChildAt(0).measuredHeight - measuredHeight) && scrollY != oldScrollY -> BOTTOM
        scrollY < oldScrollY -> UP
        scrollY > oldScrollY -> DOWN
        scrollY == 0 -> TOP
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