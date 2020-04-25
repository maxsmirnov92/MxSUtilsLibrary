package net.maxsmr.commonutils.android.gui.fragments.dialogs.holder

import android.text.TextUtils
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import org.jetbrains.annotations.Nullable
import ru.railways.core.common.utils.rx.LiveSubject

val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("DialogFragmentsHolder")

/**
 * Class for showing/hiding [DialogFragment] via specified [FragmentManager], with storing its state info
 * (if it's [TypedDialogFragment] - also handling dismiss events)
 * @param allowedTags fragment tags, which this Holder should handle, include after restore
 * (will not show or apply after restore dialog if new tag is not in this set)
 * or empty - if allow any tag
 */
@MainThread
open class DialogFragmentsHolder(val allowedTags: Set<String> = emptySet()) : LifecycleObserver {

    init {
        checkTags()
    }

    val triggerListenerSubject = LiveSubject<DialogFragment>(LiveSubject.SubjectType.BEHAVIOUR)

    val showingFragments: List<DialogFragment> get() = activeFragments.filter { it.isAdded }

    val showingFragmentsCount: Int get() = showingFragments.size

    val isAnyFragmentShowing: Boolean get() = showingFragmentsCount > 0

    val isCommitAllowed
        get() = with(ownerCurrentState) {
            this != Lifecycle.Event.ON_STOP || this != Lifecycle.Event.ON_DESTROY
        }

    /**
     * Active DialogFragments, added to specified [FragmentManager]
     * note that Set may be not actual (for example, if removing/adding directly to manager in other place)
     * so invalidate manually by [hide]
     */
    protected val activeFragments: MutableSet<DialogFragment> = mutableSetOf()

    /**
     * Fragments to show when commits will be allowed (or show rule will be changed)
     * remembering tags because instance may not contain target tag
     * Boolean - should reshow
     */
    protected val targetFragmentsToShow: MutableMap<String, Pair<DialogFragment, Boolean>> = mutableMapOf()

    /**
     * Fragments tags to hide when commits will be allowed
     */
    protected val targetFragmentsToHide: MutableSet<String> = mutableSetOf()

    /**
     * Allow showing single or multiple [TypedDialogFragment] instances at time
     */
    var showRule: ShowRule = ShowRule.MULTI
        set(value) {
            if (field != value) {
                field = value
                if (value == ShowRule.SINGLE) {
                    val copyList: List<DialogFragment> = activeFragments.toList()
                    val it: Iterator<DialogFragment> = copyList.iterator()
                    while (it.hasNext() && showingFragmentsCount > 1) {
                        val tag: String? = it.next().getTag()
                        if (tag != null && tag.isNotEmpty()) {
                            val hideResult: Pair<Boolean?, DialogFragment?> = hide(tag)
                            with(hideResult.first) {
                                val hideFragment = hideResult.second
                                if (shouldStoreRejectedFragments && this != null && this && hideFragment != null) {
                                    targetFragmentsToShow[tag] = Pair(hideFragment, false)
                                }
                            }
                        }
                    }
                } else {
                    handleTargetFragmentsToShow()
                }
            }
        }

    /**
     * Store rejected by [ShowRule.SINGLE] fragments in scheduled for showing when
     * mode will be changed to [ShowRule.MULTI]
     */
    var shouldStoreRejectedFragments = true
        set(value) {
            if (field != value) {
                field = value
                if (value && showRule == ShowRule.MULTI) {
                    // previously rejected fragments (because of onStop/onDestroy) now should be handled here
                    handleTargetFragmentsToShow()
                }
            }
        }

    protected var currentOwner: LifecycleOwner? = null

    private var ownerCurrentState = Lifecycle.Event.ON_ANY
        private set(value) {
            if (field != value) {
                field = value
                if (isCommitAllowed) {
                    if (fragmentManager != null) {
                        handleTargetFragmentsToHide()
                        handleTargetFragmentsToShow()
                    }
                }
            }
        }

    private var fragmentManager: FragmentManager? = null
        set(value) {
            field = value
            if (value != null) {
                restoreDialogsFromFragmentManager()
                handleTargetFragmentsToHide()
                handleTargetFragmentsToShow()
            } else {
                if (ownerCurrentState != Lifecycle.Event.ON_DESTROY) {
                    // if it's happening onDestroy - don't hide and remember those tags
                    hideActive()
                } else {
                    activeFragments.clear()
                }
            }
        }

    /**
     * Should call manually from appropriate method of activity/fragment
     */
    @CallSuper
    open fun onCreate(owner: LifecycleOwner, fragmentManager: FragmentManager) {
        attachOwner(owner, fragmentManager)
        ownerCurrentState = Lifecycle.Event.ON_CREATE
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    open fun onResumed() {
        logger.d( "onResumed")
        ownerCurrentState = Lifecycle.Event.ON_RESUME
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun onStop() {
        logger.d( "onStop")
        ownerCurrentState = Lifecycle.Event.ON_STOP
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun onDestroy() {
        logger.d( "onDestroy")
        cleanUp(true)
    }

    fun isFragmentShowing(tag: String): Boolean =
            findShowingFragmentByTag<DialogFragment>(tag) != null

    @SuppressWarnings("unchecked")
    @Nullable
    fun <F : DialogFragment> findShowingFragmentByTag(tag: String?): F? =
            showingFragments.find { tag == it.tag } as F?

    /**
     * @param tag      new tag for adding for specified fragment
     * @param fragment created instance
     * @param reshow   if fragment for specified tag is already showing, it will be re-showed
     * @return true if successfully showed, false - otherwise
     */
    fun show(tag: String, fragment: DialogFragment, reshow: Boolean): Boolean {
        logger.d( "show: tag=$tag, reshow=$reshow")

        val fragmentManager = fragmentManager
        checkNotNull(fragmentManager) { "FragmentManager is not specified" }
        checkTag(tag)

        if (!reshow && isFragmentShowing(tag)) {
            return false
        }
        if (!hide(tag).first) {
            return false
        }
        if (showRule == ShowRule.SINGLE && isAnyFragmentShowing) {
            logger.w( "Not adding fragment for tag '$tag', because show rule is '" + ShowRule.SINGLE.name
                    + "' and some dialogs are showing")
            if (shouldStoreRejectedFragments) {
                logger.w( "Saving fragment for tag '$tag' to show it later...")
                targetFragmentsToShow[tag] = Pair(fragment, reshow)
            }
            return false
        }
        if (isCommitAllowed) {
            onSetEventListener(fragment)
            try {
                fragment.show(fragmentManager, tag)
            } catch (e: Exception) {
                logger.e( "An Exception occurred during show(): " + e.message, e)
                targetFragmentsToShow[tag] = Pair(fragment, reshow)
                return false
            }
            activeFragments.add(fragment)
            targetFragmentsToShow.remove(tag)
            return true
        }
        logger.w( "Transaction commits are not allowed, schedule showing...")
        targetFragmentsToShow[tag] = Pair(fragment, reshow)
        return false
    }

    /**
     * @return Pair:
     * - true if fragment for specified tag was successfully hided or already not showing,
     * false - otherwise (also when showing was scheduled)
     * - [TypedDialogFragment] instance non-null if was added to [FragmentManager] before, false otherwise
     */
    fun hide(tag: String?): Pair<Boolean, DialogFragment?> {
        logger.d( "hide: tag=$tag")

        var result = false
        var fragment: DialogFragment? = null

        if (tag != null) {
            result = true
            fragment = findShowingFragmentByTag(tag)
            var isDismissed = fragment == null
            if (fragment != null) {
                if (isCommitAllowed) {
                    try {
                        fragment.dismiss()
                    } catch (e: Exception) {
                        logger.e( "An Exception occurred during dismiss(): " + e.message, e)
                        targetFragmentsToHide.add(tag)
                        result = false
                    }
                    isDismissed = result
                } else {
                    logger.w( "Transaction commits are not allowed, schedule hiding")
                    targetFragmentsToHide.add(tag)
                    result = false
                }
            }
            if (isDismissed) {
                val it = activeFragments.iterator()
                while (it.hasNext()) {
                    val f = it.next()
                    if (tag == f.tag) {
                        it.remove()
                    }
                }
            }
            if (result) {
                targetFragmentsToHide.remove(tag)
            }
        }
        return Pair(result, fragment)
    }

    fun hideActive() {
        for (f in activeFragments) {
            hide(f.tag)
        }
        activeFragments.clear()
    }

    /**
     * Deattach owner and clear active fragments;
     * this object will stay reusable after that, just call onCreate
     */
    fun cleanUp() {
        cleanUp(false)
    }

    @CallSuper
    protected open fun onDialogDismiss(fragment: DialogFragment) {
        val tag = fragment.tag
        if (tag != null) {
            if (activeFragments.remove(fragment)
                    || targetFragmentsToHide.remove(tag)) {
                if (showRule == ShowRule.SINGLE && targetFragmentsToShow.isNotEmpty()) { // add first scheduled fragment, because of mode
                    val e: Map.Entry<String, Pair<DialogFragment?, Boolean?>> = targetFragmentsToShow.entries.toList()[0]
                    val p: Pair<DialogFragment?, Boolean?> = e.value
                    val first = p.first
                    val second = p.second
                    if (first != null && second != null) {
                        show(e.key, first, second)
                    }
                }
            }
        }
    }

    protected open fun onSetOtherEventListener(forFragment: DialogFragment, owner: LifecycleOwner) {
        // override if other listeners needed
    }

    protected fun restoreDialogsFromFragmentManager() {

        fun applyFragment(fragment: Fragment?) {
            if (fragment is DialogFragment) {
                activeFragments.add(fragment)
                onSetEventListener(fragment)
            }
        }

        val fragmentManager = fragmentManager
        checkNotNull(fragmentManager) { "FragmentManager is not specified" }
        activeFragments.clear()
        if (allowedTags.isNotEmpty()) {
            for (tag in allowedTags) {
                if (!TextUtils.isEmpty(tag)) {
                    applyFragment(fragmentManager.findFragmentByTag(tag))
                }
            }
        } else {
            for (fragment in fragmentManager.fragments) {
                applyFragment(fragment)
            }
        }
    }

    protected fun handleTargetFragmentsToShow() {
        for ((tag, p) in targetFragmentsToShow) {
            val first = p.first
            val second = p.second
            show(tag, first, second)
        }
    }

    protected fun handleTargetFragmentsToHide() {
        for (tag in targetFragmentsToHide) {
            hide(tag)
        }
    }

    protected open fun cleanUp(isDestroyed: Boolean = true) {
        ownerCurrentState = if (isDestroyed) Lifecycle.Event.ON_DESTROY else Lifecycle.Event.ON_ANY
        detachCurrentOwner()
    }

    private fun attachOwner(owner: LifecycleOwner, fragmentManager: FragmentManager) {
        detachCurrentOwner()
        owner.lifecycle.addObserver(this)
        this.currentOwner = owner
        this.fragmentManager = fragmentManager
    }

    private fun detachCurrentOwner() {
        currentOwner?.lifecycle?.removeObserver(this)
        currentOwner = null
        fragmentManager = null
        targetFragmentsToShow.clear()
        targetFragmentsToHide.clear()
    }

    private fun onSetEventListener(forFragment: DialogFragment) {
        with(currentOwner) {
            checkNotNull(this) { "LifecycleOwner is not attached" }
            if (forFragment is TypedDialogFragment<*>) {
                forFragment.dismissSubject.subscribe(this) {
                    onDialogDismiss(forFragment)
                }
            }
            onSetOtherEventListener(forFragment, this)
        }
        triggerListenerSubject.onNext(forFragment)
    }

    private fun checkTags() {
        var checkResult = allowedTags.isEmpty()
        allowedTags.forEach {
            if (it.isNotEmpty()) {
                checkResult = true
            }
        }
        if (!checkResult) {
            throw IllegalArgumentException("Specified tags not contain any valid tag")
        }
    }

    private fun checkTag(tag: String) {
        require(!TextUtils.isEmpty(tag)) { "Tag must be non-empty" }
        if (allowedTags.isEmpty()) return
        require(allowedTags.contains(tag)) { "Tag '$tag' is not declared in holder" }
    }

    enum class ShowRule {

        /**
         * only one fragment in back stack is allowed
         */
        SINGLE,

        /**
         * fragments count is not limited
         */
        MULTI
    }

    companion object {

        fun mergeTags(firstTags: Collection<String>?, secondTags: Collection<String>?): Set<String> {
            val tags = firstTags?.toMutableSet() ?: mutableSetOf()
            if (secondTags != null) {
                tags.addAll(secondTags)
            }
            return tags
        }
    }
}