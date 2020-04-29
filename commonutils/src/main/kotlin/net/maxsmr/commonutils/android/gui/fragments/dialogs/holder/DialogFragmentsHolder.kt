package net.maxsmr.commonutils.android.gui.fragments.dialogs.holder

import android.app.Dialog
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
import io.reactivex.Observable
import io.reactivex.functions.Predicate
import io.reactivex.subjects.BehaviorSubject
import net.maxsmr.commonutils.android.gui.fragments.actions.EmptyAction
import net.maxsmr.commonutils.android.gui.fragments.actions.TypedAction
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.rx.LiveObservable
import net.maxsmr.commonutils.rx.toLive
import org.jetbrains.annotations.Nullable

val logger: BaseLogger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("DialogFragmentsHolder")

private val RESTRICTED_STATES = setOf(Lifecycle.Event.ON_ANY, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY)

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

    private val listenerSubject: BehaviorSubject<DialogFragment> = BehaviorSubject.create()

    val showingFragments: List<DialogFragment> get() = activeFragments.filter { it.isAdded }

    val showingFragmentsCount: Int get() = showingFragments.size

    val isAnyFragmentShowing: Boolean get() = showingFragmentsCount > 0

    val isCommitAllowed get() = !RESTRICTED_STATES.contains(lastLifecycleEvent)

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

    private var lastLifecycleEvent = Lifecycle.Event.ON_ANY
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

    private var currentOwner: LifecycleOwner? = null

    private var fragmentManager: FragmentManager? = null
        set(value) {
            field = value
            if (value != null) {
                restoreDialogsFromFragmentManager()
                handleTargetFragmentsToHide()
                handleTargetFragmentsToShow()
            } else {
                if (lastLifecycleEvent != Lifecycle.Event.ON_DESTROY) {
                    // if it's happening onDestroy - don't hide and remember those tags
                    hideActive()
                } else {
                    activeFragments.clear()
                }
            }
        }


    /**
     * @param tag тэг, с которым был стартован диалог, может быть пустым
     * @param eventsMapper функция, извлекающая из диалога указанного типа требуемый Observable, эмитящий эвенты
     * @return [LiveObservable] с конкретным типом эвента с автоматической отпиской в onDestroy
     */
    fun <T, D : DialogFragment> events(
            tag: String? = null,
            dialogClass: Class<D>,
            eventsFilter: Predicate<T>? = null,
            eventsMapper: (D) -> Observable<T>
    ): LiveObservable<T> =
            listenerSubject
                    .ofType(dialogClass) // отфильтровали по целевому типу диалога
                    .filter { (tag.isNullOrEmpty() || it.tag == tag) }
                    .switchMap { eventsMapper(it) } // целевой observable из диалога
                    .filter {
                        // данные, выбрасываемые из него, удовлетворяют условию
                        eventsFilter?.test(it) ?: true
                    }
                    .toLive()

    @Suppress("UNCHECKED_CAST")
    fun <D : Dialog> createdEvents(tag: String? = null): LiveObservable<TypedAction<D>> =
            events(
                    tag,
                    TypedDialogFragment::class.java as Class<TypedDialogFragment<D>>
            ) {
                it.createdObservable()
            }

    /**
     * Подписка на клики по кнопкам диалога
     *
     * @param tag тэг, с которым был стартован диалог
     * @param buttons типы кнопок, за которыми наблюдаем (см. [android.content.DialogInterface]), или
     * пустой список, если наблюдаем за всеми
     */
    fun buttonClickEvents(tag: String? = null, vararg buttons: Int): LiveObservable<TypedAction<Int>> =
            events(
                    tag,
                    TypedDialogFragment::class.java,
                    Predicate {
                        buttons.isEmpty() || it.value in buttons
                    }
            ) {
                it.buttonClickObservable()
            }

    fun keyActionEvents(
            tag: String? = null, vararg keyCodes: Int
    ): LiveObservable<TypedDialogFragment.KeyAction> =
            keyActionEvents(tag, Predicate { keyCodes.isEmpty() || it.keyCode in keyCodes })

    fun keyActionEvents(
            tag: String? = null, keyEventFilter: Predicate<TypedDialogFragment.KeyAction>
    ): LiveObservable<TypedDialogFragment.KeyAction> =
            events(
                    tag,
                    TypedDialogFragment::class.java,
                    keyEventFilter
            ) {
                it.keyActionObservable()
            }

    fun dismissEvents(tag: String? = null): LiveObservable<EmptyAction> =
            events(
                    tag,
                    TypedDialogFragment::class.java
            ) {
                it.dismissObservable()
            }

    fun cancelEvents(tag: String? = null): LiveObservable<EmptyAction> =
            events(
                    tag,
                    TypedDialogFragment::class.java
            ) {
                it.cancelObservable()
            }

    /**
     * Should call manually from appropriate method of activity/fragment
     */
    @CallSuper
    open fun init(owner: LifecycleOwner, fragmentManager: FragmentManager) {
        logger.d("init")
        lastLifecycleEvent = Lifecycle.Event.ON_CREATE
        attachOwner(owner, fragmentManager)
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    open fun onResumed() {
        logger.d("onResumed")
        lastLifecycleEvent = Lifecycle.Event.ON_RESUME
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun onStop() {
        logger.d("onStop")
        lastLifecycleEvent = Lifecycle.Event.ON_STOP
    }

    @CallSuper
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun onDestroy() {
        logger.d("onDestroy")
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
    fun show(tag: String, fragment: DialogFragment, reshow: Boolean = true): Boolean {
        logger.d("show: tag=$tag, fragment=$fragment, reshow=$reshow")

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
            try {
                fragment.show(fragmentManager, tag)
            } catch (e: Exception) {
                logger.e( "An Exception occurred during show(): " + e.message, e)
                targetFragmentsToShow[tag] = Pair(fragment, reshow)
                return false
            }
            activeFragments.add(fragment)
            targetFragmentsToShow.remove(tag)
            onSetEventListener(fragment)
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
        logger.d("hide: tag=$tag")

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
        lastLifecycleEvent = if (isDestroyed) Lifecycle.Event.ON_DESTROY else Lifecycle.Event.ON_ANY
        detachOwner()
    }

    private fun attachOwner(owner: LifecycleOwner, fragmentManager: FragmentManager) {
        detachOwner()
        owner.lifecycle.addObserver(this)
        this.currentOwner = owner
        this.fragmentManager = fragmentManager
    }

    private fun detachOwner() {
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
                dismissEvents(forFragment.tag).subscribe(this) {
                    onDialogDismiss(forFragment)
                }
            }
            onSetOtherEventListener(forFragment, this)
        }
        listenerSubject.onNext(forFragment)
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