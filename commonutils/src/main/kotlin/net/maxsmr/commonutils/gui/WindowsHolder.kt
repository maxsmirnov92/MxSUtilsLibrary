package net.maxsmr.commonutils.gui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.MainThread
import net.maxsmr.commonutils.isPreKitkat
import net.maxsmr.commonutils.Predicate
import net.maxsmr.commonutils.text.isEmpty
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException

/**
 * Class for holding added/removed [View] to/from [WindowManager]
 * Top view is first added
 */
@MainThread
open class WindowsHolder(
        protected val viewFactory: ViewFactory,
        context: Context,
        tags: Collection<String>?
) {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(WindowsHolder::class.java)

    protected val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            ?: throw RuntimeException("WindowManager is null")

    /**
     * tags to use in this helper
     */
    protected val tags: MutableList<String> = mutableListOf()

    protected var defaultLayoutParams : ViewGroup.LayoutParams? = null

    /**
     * active views, added to [android.view.WindowManager]
     * note that Set may be not actual (for example, if removing/adding directly to manager in other place)
     * so invalidate manually by [AlertDialogFragmentsHolder.hideAlert]
     */
    protected val activeViews: MutableSet<View> = mutableSetOf()

    init {
        if (tags != null) {
            this.tags.addAll(tags)
        }
    }

    fun isAnyViewAdded(): Boolean =
            getAddedViewsCount() > 0

    fun getAddedViewsCount(): Int = activeViews.size
//        return Predicate.Methods.filter<View>(activeViews) { v -> isAttached(v) }.size

    fun isViewAdded(tag: String): Boolean =
            getAddedViewByTag<View>((tag)) != null

    @Suppress("UNCHECKED_CAST")
    fun <V : View> getAddedViewByTag(tag: String): V? =
            Predicate.Methods.find(activeViews) { v -> /*isAttached(v) &&*/ tag == v.tag } as V?

    @JvmOverloads
    protected open fun addView(tag: String, reAdd: Boolean = true): Boolean {
        val params = defaultLayoutParams
                ?: throw IllegalStateException("Default layout params was not specified")
        return addView(tag, params, reAdd)
    }


    /**
     * @param tag      new tag for adding for specified view
     * @param view created instance
     * @param reAdd   if view for specified tag is already showing, it will be re-showed
     * @return true if successfully showed, false - otherwise (also when showing was scheduled)
     */
    @JvmOverloads
    protected open fun addView(
            tag: String,
            layoutParams: ViewGroup.LayoutParams,
            reAdd: Boolean = true
    ): Boolean {
        checkTag(tag)

        if (!reAdd && isViewAdded(tag)) {
            return false
        }

        if (!removeViewByTag(tag).first) {
            return false
        }

        val view = viewFactory.createViewByTag(tag)
        view.tag = tag

        try {
            windowManager.addView(view, layoutParams)
        } catch (e: Exception) {
            logger.e(formatException(e, "addView"))
            return false
        }

        activeViews.add(view)
        return true
    }

    /**
     * @return Pair:
     * - true if view for specified tag was successfully hided or already not showing,
     *   false - otherwise
     * - [View] instance non-null if was added to [WindowManager] before, false otherwise
     */
    protected open fun removeViewByTag(tag: String): Pair<Boolean, View?> {

        val view = getAddedViewByTag<View>(tag)

        var result = true
        var isRemoved = view == null

        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                logger.e(formatException(e, "removeView"))
                result = false
            }

            isRemoved = result
        }

        if (isRemoved) {
            val it = activeViews.iterator()
            while (it.hasNext()) {
                val v = it.next()
                if (tag == v.tag) {
                    it.remove()
                }
            }
        }

        return Pair(result, view)
    }

    private fun checkTag(tag: String) {
        if (isEmpty(tag)) {
            throw IllegalArgumentException("Tag must be non-empty")
        }
        if (tags.isNotEmpty() && !tags.contains(tag)) {
            throw IllegalArgumentException("Tag '$tag' is not declared in holder")
        }
    }

    interface ViewFactory {

        fun createViewByTag(tag: String) : View
    }

    companion object {

        fun isAttached(v: View?): Boolean {
            var isAttached = false
            if (v != null) {
                isAttached = isPreKitkat() || v.isAttachedToWindow
            }
            return isAttached
        }
    }
}