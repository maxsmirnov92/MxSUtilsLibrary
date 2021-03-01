package net.maxsmr.commonutils.gui.fragments.dialogs.holder

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
import io.reactivex.*
import io.reactivex.functions.Predicate
import io.reactivex.subjects.BehaviorSubject
import net.maxsmr.commonutils.gui.actions.EmptyAction
import net.maxsmr.commonutils.gui.actions.TypedAction
import net.maxsmr.commonutils.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.logException
import net.maxsmr.commonutils.rx.live.*
import org.jetbrains.annotations.Nullable

val logger: BaseLogger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("DialogFragmentsHolder")

private val RESTRICTED_STATES = setOf(Lifecycle.Event.ON_ANY, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY)

/**
 * Class for showing/hiding [DialogFragment] via specified [FragmentManager], with storing its state info
 * (if it's [TypedDialogFragment] - also handling dismiss events)
 * @param allowedTags fragment tags, which this Holder should handle, include after restore
 * (will not show or apply after restore dialog if new tag is not in this set)
 * or empty - if allow any tag
 *
 * Note: use methods returning Observable, Maybe, Single, Completable to subscribe on ViewModel / Presenter; and use Live-wrappers to subscibe on views
 */
@MainThread
open class DialogFragmentsHolder(val allowedTags: Set<String> = emptySet()) : LifecycleObserver {

    init {
        checkTags()
    }

    private val showDialogSubject: BehaviorSubject<DialogFragment> = BehaviorSubject.create()

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
                            val hideResult: Pair<HideResult, DialogFragment?> = hide(tag)
                            with(hideResult.first) {
                                val hideFragment = hideResult.second
                                if (shouldStoreRejectedFragments && this.isSuccess() && hideFragment != null) {
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
            if (field != value) {
                field = value
                if (value != null) {
                    restoreDialogsFromFragmentManager()
                    handleTargetFragmentsToHide()
                    handleTargetFragmentsToShow()
                } else {
                    if (lastLifecycleEvent != Lifecycle.Event.ON_DESTROY) {
                        hideActive()
                    } else {
                        // if it's happening onDestroy - don't hide and remember those tags
                        activeFragments.clear()
                    }
                }
            }
        }

    @JvmOverloads
    fun <T, D : DialogFragment> eventsObservable(
            tag: String? = null,
            dialogClass: Class<D>,
            eventsFilter: Predicate<T>? = null,
            eventsMapper: (D) -> Observable<T>
    ): Observable<T> =
            showDialogEvents(tag, dialogClass)
                    .switchMap { eventsMapper(it) } // целевой observable из диалога
                    .filter {
                        // данные, выбрасываемые из него, удовлетворяют условию
                        eventsFilter?.test(it) ?: true
                    }

    /**
     * @param tag тэг, с которым был стартован диалог, может быть пустым
     * @param eventsMapper функция, извлекающая из диалога указанного типа требуемый Observable, эмитящий эвенты
     * @return [LiveObservable] с конкретным типом эвента с автоматической отпиской в onDestroy
     */
    @JvmOverloads
    fun <T, D : DialogFragment> eventsLiveObservable(
            tag: String? = null,
            dialogClass: Class<D>,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            eventsFilter: Predicate<T>? = null,
            eventsMapper: (D) -> Observable<T>
    ): LiveObservable<T> =
            eventsObservable(tag, dialogClass, eventsFilter, eventsMapper)
                    .toLive(observingState = observingState)

    @JvmOverloads
    fun <T, D : DialogFragment> eventsSingle(
            tag: String? = null,
            dialogClass: Class<D>,
            eventsFilter: Predicate<T>? = null,
            eventsMapper: (D) -> Single<T>
    ): Single<T> =
            Single.fromObservable(showDialogEvents(tag, dialogClass)
                    .switchMap { dialog ->
                        eventsMapper(dialog).toObservable()
                    }
                    .filter {
                        eventsFilter?.test(it) ?: true
                    })

    // FIXME not working
    @JvmOverloads
    fun <T, D : DialogFragment> eventsLiveSingle(
            tag: String? = null,
            dialogClass: Class<D>,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            eventsFilter: Predicate<T>? = null,
            eventsMapper: (D) -> Single<T>
    ): LiveSingle<T> =
            eventsSingle(tag, dialogClass, eventsFilter, eventsMapper)
                    .toLive(observingState = observingState)

    @JvmOverloads
    fun <T, D : DialogFragment> eventsMaybe(
            tag: String? = null,
            dialogClass: Class<D>,
            eventsFilter: Predicate<T>? = null,
            eventsMapper: (D) -> Maybe<T>
    ): Maybe<T> =
            showDialogEvents(tag, dialogClass)
                    .switchMap { dialog ->
                        eventsMapper(dialog).toObservable()
                    }
                    .filter {
                        eventsFilter?.test(it) ?: true
                    }
                    .firstElement()

    @JvmOverloads
    fun <T, D : DialogFragment> eventsLiveMaybe(
            tag: String? = null,
            dialogClass: Class<D>,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            eventsFilter: Predicate<T>? = null,
            eventsMapper: (D) -> Maybe<T>
    ): LiveMaybe<T> =
            eventsMaybe(tag, dialogClass, eventsFilter, eventsMapper)
                    .toLive(observingState = observingState)

    @JvmOverloads
    fun <D : DialogFragment> eventsCompletable(
            tag: String? = null,
            dialogClass: Class<D>,
            eventsMapper: (D) -> Completable
    ): Completable {
        return showDialogEvents(tag, dialogClass)
                .switchMap { dialog ->
                    eventsMapper(dialog).andThen(Observable.just(EmptyAction))
                }
                .ignoreElements()
    }

    // FIXME not working
    @JvmOverloads
    fun <D : DialogFragment> eventsLiveCompletable(
            tag: String? = null,
            dialogClass: Class<D>,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            eventsMapper: (D) -> Completable
    ): LiveCompletable {
        return eventsCompletable(tag, dialogClass, eventsMapper)
                .toLive(observingState = observingState)
    }

    // FIXME not working
    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun <D : Dialog> createdLiveEventsOnce(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<D>> = TypedDialogFragment::class.java as Class<TypedDialogFragment<D>>,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            eventsFilter: Predicate<TypedAction<D>>? = null
    ): LiveSingle<TypedAction<D>> =
            eventsLiveSingle(
                    tag,
                    clazz,
                    observingState,
                    eventsFilter
            ) {
                it.createdSingle()
            }

    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun <D : Dialog> createdLiveEvents(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<D>> = TypedDialogFragment::class.java as Class<TypedDialogFragment<D>>,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            eventsFilter: Predicate<TypedAction<D>>? = null
    ): LiveObservable<TypedAction<D>> =
            eventsLiveObservable(
                    tag,
                    clazz,
                    observingState,
                    eventsFilter
            ) {
                it.createdSingle().toObservable()
            }

    fun buttonClickLiveEvents(
            tag: String? = null,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            buttons: Collection<Int> = listOf()
    ): LiveObservable<TypedAction<Int>> = buttonClickLiveEvents(tag, TypedDialogFragment::class.java, observingState, buttons)

    /**
     * Подписка на клики по кнопкам диалога
     *
     * @param tag тэг, с которым был стартован диалог
     * @param buttons типы кнопок, за которыми наблюдаем (см. [android.content.DialogInterface]), или
     * пустой список, если наблюдаем за всеми
     */
    @JvmOverloads
    fun buttonClickLiveEvents(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<*>> = TypedDialogFragment::class.java,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            buttons: Collection<Int> = listOf()
    ): LiveObservable<TypedAction<Int>> =
            eventsLiveObservable(
                    tag,
                    clazz,
                    observingState,
                    {
                        buttons.isEmpty() || it.value in buttons
                    }
            ) {
                it.buttonClickObservable()
            }

    @JvmOverloads
    fun keyActionLiveEvents(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<*>> = TypedDialogFragment::class.java,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            vararg keyCodes: Int
    ): LiveObservable<TypedDialogFragment.KeyAction> =
            keyActionLiveEvents(tag, clazz, observingState, { keyCodes.isEmpty() || it.keyCode in keyCodes })

    @JvmOverloads
    fun keyActionLiveEvents(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<*>> = TypedDialogFragment::class.java,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED,
            keyEventFilter: Predicate<TypedDialogFragment.KeyAction>
    ): LiveObservable<TypedDialogFragment.KeyAction> =
            eventsLiveObservable(
                    tag,
                    clazz,
                    observingState,
                    keyEventFilter
            ) {
                it.keyActionObservable()
            }

    // FIXME not working
    @JvmOverloads
    fun dismissLiveEventsOnce(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<*>> = TypedDialogFragment::class.java,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED
    ): LiveCompletable =
            eventsLiveCompletable(
                    tag,
                    clazz,
                    observingState
            ) {
                it.dismissCompletable()
            }

    @JvmOverloads
    fun dismissLiveEvents(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<*>> = TypedDialogFragment::class.java,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED
    ): LiveObservable<EmptyAction> =
            eventsLiveObservable(
                    tag,
                    clazz,
                    observingState
            ) { fragment ->
                fragment.dismissCompletable().andThen(Observable.just(EmptyAction))
                // то же самое по смыслу:
//                Observable.create<EmptyAction> {
//                    fragment.dismissCompletable().doOnComplete {
//                                it.onNext(EMPTY_ACTION)
//                            }
//                            .subscribe()
//                }
            }

    // FIXME not working
    @JvmOverloads
    fun cancelEventsOnce(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<*>> = TypedDialogFragment::class.java,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED
    ): LiveCompletable =
            eventsLiveCompletable(
                    tag,
                    clazz,
                    observingState
            ) {
                it.cancelCompletable()
            }

    @JvmOverloads
    fun cancelEvents(
            tag: String? = null,
            clazz: Class<TypedDialogFragment<*>> = TypedDialogFragment::class.java,
            observingState: Lifecycle.State? = Lifecycle.State.STARTED
    ): LiveObservable<EmptyAction> =
            eventsLiveObservable(
                    tag,
                    clazz,
                    observingState
            ) {
                it.cancelCompletable().andThen(Observable.just(EmptyAction))
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

    fun showNoResult(tag: String, fragment: DialogFragment, reshow: Boolean = true): Boolean =
            show(tag, fragment, reshow).isSuccess()

    /**
     * @param tag      new tag for adding for specified fragment
     * @param fragment created instance
     * @param reshow   if fragment for specified tag is already showing, it will be re-showed
     * @return true if successfully showed, false - otherwise
     */
    fun show(tag: String, fragment: DialogFragment, reshow: Boolean = true): ShowResult {
        logger.d("show: tag=$tag, fragment=$fragment, reshow=$reshow")

        val isFragmentShowing = isFragmentShowing(tag)
        val fragmentManager = fragmentManager
        checkNotNull(fragmentManager) { "FragmentManager is not specified" }
        checkTag(tag)

        if (isFragmentShowing && !reshow) {
            return ShowResult.AlreadyShowed
        }
        if (isFragmentShowing && !hide(tag).first.isSuccess()) {
            return ShowResult.Failed(ShowResult.Failed.Reason.HIDE)
        }
        if (showRule == ShowRule.SINGLE && isAnyFragmentShowing) {
            logger.w("Not adding fragment for tag '$tag', because show rule is '" + ShowRule.SINGLE.name
                    + "' and some dialogs are showing")
            if (shouldStoreRejectedFragments) {
                logger.w("Saving fragment for tag '$tag' to show it later...")
                targetFragmentsToShow[tag] = Pair(fragment, reshow)
            }
            return ShowResult.Failed(ShowResult.Failed.Reason.SHOW_RULE)
        }
        if (isCommitAllowed) {
            try {
                fragment.show(fragmentManager, tag)
            } catch (e: Exception) {
                logException(logger, e, "show")
                targetFragmentsToShow[tag] = Pair(fragment, reshow)
                return ShowResult.Failed(ShowResult.Failed.Reason.EXCEPTION)
            }
            activeFragments.add(fragment)
            targetFragmentsToShow.remove(tag)
            onSetEventListener(fragment)
            return if (isFragmentShowing) ShowResult.Reshowed else ShowResult.Showed
        }
        logger.w("Transaction commits are not allowed, schedule showing...")
        targetFragmentsToShow[tag] = Pair(fragment, reshow)
        return ShowResult.Failed(ShowResult.Failed.Reason.NOT_ALLOWED)
    }

    fun hideNoResult(tag: String?): Pair<Boolean, DialogFragment?> =
            with(hide(tag)) {
                Pair(this.first.isSuccess(), this.second)
            }


    /**
     * @return Pair:
     * - true if fragment for specified tag was successfully hided or already not showing,
     * false - otherwise (also when showing was scheduled)
     * - [TypedDialogFragment] instance non-null if was added to [FragmentManager] before, false otherwise
     */
    fun hide(tag: String?): Pair<HideResult, DialogFragment?> {
        logger.d("hide: tag=$tag")

        var result: HideResult = HideResult.FAILED
        var fragment: DialogFragment? = null

        if (tag != null) {
            fragment = findShowingFragmentByTag(tag)
            if (fragment != null) {
                if (isCommitAllowed) {
                    try {
                        fragment.dismiss()
                        result = HideResult.DISMISSED
                    } catch (e: Exception) {
                        logException(logger, e, "dismiss")
                        targetFragmentsToHide.add(tag)
                    }
                } else {
                    logger.w("Transaction commits are not allowed, schedule hiding")
                    targetFragmentsToHide.add(tag)
                }
            } else {
                result = HideResult.ALREADY_DISMISSED
            }
            if (result.isSuccess()) {
                val it = activeFragments.iterator()
                while (it.hasNext()) {
                    val f = it.next()
                    if (tag == f.tag) {
                        it.remove()
                    }
                }
                if (result == HideResult.DISMISSED) {
                    targetFragmentsToHide.remove(tag)
                }
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
        detachCurrentOwner()
    }

    @CallSuper
    protected open fun attachOwner(owner: LifecycleOwner, fragmentManager: FragmentManager) {
        if (currentOwner != owner) {
            detachCurrentOwner()
            owner.lifecycle.addObserver(this)
            this.currentOwner = owner
        }
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
                // должен прилетать в любых стейтах, чтобы отслеживать здесь
                dismissLiveEvents(forFragment.tag, observingState = null).subscribe(this, emitOnce = true) {
                    onDialogDismiss(forFragment)
                }
            }
            onSetOtherEventListener(forFragment, this)
        }
        showDialogSubject.onNext(forFragment)
    }

    private fun <D : DialogFragment> showDialogEvents(
            tag: String? = null,
            dialogClass: Class<D>
    ): Observable<D> =
            showDialogSubject
                    .ofType(dialogClass) // отфильтровали по целевому типу диалога
                    .filter { (tag.isNullOrEmpty() || it.tag == tag) } // при пустом тэге - все; или если совпадает

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

    sealed class ShowResult {

        object Showed : ShowResult()
        object Reshowed : ShowResult()
        object AlreadyShowed : ShowResult()

        data class Failed(val reason: Reason) : ShowResult() {

            enum class Reason {
                /**
                 * failed dismiss
                 */
                HIDE,

                /**
                 * due to [ShowRule.SINGLE]
                 */
                SHOW_RULE,

                /**
                 * exception
                 */
                EXCEPTION,

                /**
                 * transactions are not allowed
                 */
                NOT_ALLOWED
            }
        }

        fun isSuccess() = this !is Failed
    }

    enum class HideResult {
        DISMISSED,
        ALREADY_DISMISSED,
        FAILED;

        fun isSuccess() = this != FAILED
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