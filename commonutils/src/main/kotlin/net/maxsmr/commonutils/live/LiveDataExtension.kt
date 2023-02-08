package net.maxsmr.commonutils.live

import android.os.CountDownTimer
import android.os.Handler
import android.widget.TextView
import androidx.lifecycle.*
import net.maxsmr.commonutils.format.getFormattedText
import net.maxsmr.commonutils.gui.setTextChecked
import net.maxsmr.commonutils.gui.setTextDistinctFormatted
import net.maxsmr.commonutils.live.wrappers.NotifyCheckMutableLiveData
import net.maxsmr.commonutils.states.ILoadState
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.states.getOrCreate
import net.maxsmr.commonutils.validation.BaseValidator
import ru.tinkoff.decoro.Mask
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

// region Field

/**
 * Обозревание форматированного или неформатированного (второй вариант более правильный)
 * из [MutableLiveData] с целью выставления в целевую [view] форматированного значения методом refreshMask
 * @param maskWatcher уже привязанный к [view]
 */
@JvmOverloads
fun MutableLiveData<String>.observeFromTextFormatted(
        view: TextView,
        owner: LifecycleOwner,
        maskWatcher: MaskFormatWatcher,
        asString: Boolean = true,
        onChanged: ((String?) -> Unit)? = null
) {
    observe(owner) {
        onChanged?.invoke(it)
        view.setTextDistinctFormatted(it, maskWatcher, asString)
    }
}

@JvmOverloads
fun MutableLiveData<String>.observeFromTextFormatted(
        view: TextView,
        owner: LifecycleOwner,
        mask: Mask,
        distinct: Boolean = true,
        asString: Boolean = true,
        onChanged: ((String?) -> Unit)? = null
) {
    observeFromText(view, owner, distinct, asString) {
        onChanged?.invoke(it)
        getFormattedText(mask, it)
    }
}

/**
 * Обозревание форматированного или неформатированного (второй вариант более правильный)
 * из [MutableLiveData] с целью выставления в целевую [view] форматированного значения через [formatFunc]
 */
fun MutableLiveData<String>.observeFromText(
        view: TextView,
        owner: LifecycleOwner,
        distinct: Boolean = true,
        asString: Boolean = true,
        formatFunc: ((String) -> CharSequence?)? = null
) {
    observeFrom(view, owner, distinct, asString) {
        formatFunc?.invoke(it) ?: it
    }
}

@JvmOverloads
fun <D> MutableLiveData<D>.observeFrom(
        view: TextView,
        owner: LifecycleOwner,
        distinct: Boolean = true,
        asString: Boolean = true,
        formatFunc: (D) -> CharSequence?
) {
    observe(owner) {
        view.setTextChecked(formatFunc(it), distinct, asString)
    }
}

// endregion

/**
 * @return [LiveData] с изменёнными текущими данными из исходного [T] с использованием [changeFunc]
 */
fun <T> LiveData<T>.observeWithChange(changeFunc: (T) -> T): LiveData<T> {
    val result = MediatorLiveData<T>()
    result.addSource(this) {
        result.postValue(changeFunc(it))
    }
    return result
}

/**
 * @return [LiveData] с текущими данными другого типа из исходного [T] с использованием [changeFunc]
 */
fun <T, X> LiveData<T>.observeWithTypeChange(changeFunc: (T) -> X): LiveData<X> {
    val result = MediatorLiveData<X>()
    result.addSource(this) {
        result.postValue(changeFunc(it))
    }
    return result
}

/**
 * Однократно получить данные подписчиком и после этого удалить подписку
 */
fun <T> LiveData<T>.observeOnce(observer: Observer<T>) {
    observeForever(OnceObserver<T>(this, observer))
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, OnceObserver<T>(this, observer))
}

/**
 * эмиссирует только первое значение,
 * следующие объекты игнорирует
 */
fun <T> LiveData<T>.once(): LiveData<T> {
    val result = MediatorLiveData<T>()
    result.addSource(this) {
        result.removeSource(this)
        result.postValue(it)
    }
    return result
}

/**
 * Реализует преобразование [Transformations.map] над LiveData в стиле цепочек RxJava
 */
fun <X, Y> LiveData<X>.map(body: (X) -> Y): LiveData<Y> {
    return Transformations.map(this, body)
}

fun <X, Y> LiveData<X>.mapNotNull(body: (X) -> Y?): MutableLiveData<Y> {
    val result = MediatorLiveData<Y>()
    result.addSource(this) { x ->
        body(x)?.let {
            result.value = it
        }
    }
    return result
}

/**
 * Реализует преобразование [Transformations.switchMap] над LiveData в стиле цепочек RxJava
 */
fun <X, Y> LiveData<X>.switchMap(body: (X) -> LiveData<Y>): LiveData<Y> {
    return Transformations.switchMap(this, body)
}

/**
 * С гарантиями того, что возможные нульные данные
 * из [LiveData] вызова [body]
 * не попадут в результирующую
 */
fun <X, Y> LiveData<X>.switchMapNotNull(body: (X) -> LiveData<Y?>): LiveData<Y> {
    val result = MediatorLiveData<Y>()
    result.addSource(this, object : Observer<X> {
        var source: LiveData<Y?>? = null
        override fun onChanged(x: X) {
            val newLiveData: LiveData<Y?> = body(x)
            if (source === newLiveData) {
                return
            }
            source?.let {
                result.removeSource(it)
            }
            source = newLiveData
            result.addSource(newLiveData) { y ->
                y?.let {
                    result.setValue(y)
                }
            }
        }
    })
    return result
}

fun <T> LiveData<T>.distinct(preventNull: Boolean = false): LiveData<T> =
    distinct(preventNull) { it }

/**
 * Реализует преобразование `AppTransformations.distinct` над LiveData в стиле цепочек RxJava
 */
fun <T, K> LiveData<T>.distinct(
    preventNull: Boolean = false,
    mapper: (T) -> K,
): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()
    distinctLiveData.addSource(this, object : Observer<T> {

        private var initialized = false
        private var lastObj: K? = null

        override fun onChanged(obj: T) {
            val curr = mapper(obj)
            if (!initialized) {
                initialized = true
                lastObj = curr
                distinctLiveData.postValue(obj)
            } else if ((!preventNull || curr != null) && curr != lastObj) {
                lastObj = curr
                distinctLiveData.postValue(obj)
            }
        }
    })
    return distinctLiveData
}

/**
 * Реализует фильтрацию данных LiveData в стиле цепочек RxJava
 */
fun <X> LiveData<X>.filter(condition: (X?) -> Boolean): LiveData<X> {
    val result = MediatorLiveData<X>()
    result.addSource(this) { x -> if (condition(x)) result.value = x }
    return result
}

/**
 * Реализует фильтрацию данных LiveData в стиле цепочек RxJava
 */
fun <X> LiveData<X>.filterNotNull(): LiveData<X> {
    val result = MediatorLiveData<X>()
    result.addSource(this) { x -> if (x != null) result.value = x }
    return result
}

/**
 * Позволяет выполнить действие [action] при каждой эмиссии, не изменяя данные в цепочке
 */
fun <X> LiveData<X>.doOnNext(action: (X?) -> Unit): LiveData<X> {
    val result = MediatorLiveData<X>()
    result.addSource(this) { x ->
        action(x)
        result.value = x
    }
    return result
}

/**
 * Пропускает первые [count] элементов в цепочке
 */
fun <X> LiveData<X>.skip(count: Int): LiveData<X> {
    var counter = 0
    return this
            .doOnNext { counter++ }
            .filter { counter > count }
}

/**
 * Пропускает все первые элементы, не удовлетворяющие [predicate]. После первого элемента,
 * удовлетворяющего [predicate], фильтрация перестает выполняться.
 */
fun <X> LiveData<X>.skipUntil(predicate: (X) -> Boolean): LiveData<X> {
    val m = MediatorLiveData<X>()
    var success = false
    m.addSource(this) {
        if (success) {
            m.postValue(it)
        } else if (predicate(it)) {
            success = true
            m.postValue(it)
        }
    }
    return m
}

/**
 * Анадлог just из rx. Эмиссия одного объекта
 */
fun <T> T.just(): LiveData<T> {
    val result = MutableLiveData<T>()
    result.postValue(this)
    return result
}

/**
 * возвращает liveData, которая эммитирует последнее значение через каждые [intervalMillis] миллисекунд
 */
fun <T> LiveData<T>.updateEvery(intervalMillis: Long): LiveData<T> {
    val liveData = UpdatingLiveData<T>(intervalMillis)
    liveData.addSource(this) {
        liveData.value = it
    }
    return liveData
}

fun <X, Y> LiveData<X>.updateEveryWithTransform(
    paramsFunc: (X) -> Pair<Long, Long>?,
    transformFunc: (X?, Long, Boolean) -> Y,
): LiveData<Y> = UpdatingTransformLiveData(this, paramsFunc, transformFunc)

/**
 * Присваивает [LiveData] новое значение, только если оно изменилось
 */
fun <T> MutableLiveData<T>.setValueIfNew(newValue: T?, eagerNotify: Boolean = false) {
    if (eagerNotify || this.value != newValue) value = newValue
}

fun <T> MutableLiveData<T>.postValueIfNew(newValue: T?, eagerNotify: Boolean = false) {
    if (eagerNotify || this.value != newValue) postValue(newValue)
}

/**
 * Присваивает [NotifyCheckMutableLiveData] новое значение, только если оно изменилось
 */
fun <T> NotifyCheckMutableLiveData<T>.setValueIfNewNotify(newValue: T?, eagerNotify: Boolean = false, shouldNotify: Boolean = true) {
    if (eagerNotify || this.value != newValue) setValue(newValue, shouldNotify)
}

fun <T> NotifyCheckMutableLiveData<T>.postValueIfNewNotify(newValue: T?, eagerNotify: Boolean = false, shouldNotify: Boolean = true) {
    if (eagerNotify || this.value != newValue) postValue(newValue, shouldNotify)
}

/**
 * Переприсвает текущее значение LiveData, вызывая ее "перезарядку"
 */
fun <T> MutableLiveData<T>.recharge() {
    value = value
}

fun <T> MutableLiveData<T>.postRecharge() {
    postValue(value)
}

/**
 * Реализует аналог операции distinct из RxJava
 *
 * @param preventNull - если старое значение not null, а новое null, предотвращает эмиссию нового значения
 */


/**
 * Возвращает LiveData со списком, сформированным путем объединения всех элементов из исходных списков
 *
 * @param data - исходные списки для объединения
 */
fun <T> concat(vararg data: LiveData<List<T>>): LiveData<List<T>> {
    val concatLiveData = MediatorLiveData<List<T>>()
    val merger = object : Observer<List<T>> {
        // Предотвращает многократное срабатывание при начальном формировании общего списка
        private var initialized = 0

        override fun onChanged(obj: List<T>?) {
            if (++initialized >= data.size) {
                concatLiveData.postValue(data.flatMap { it.value ?: emptyList() })
            }
        }
    }

    data.forEach { concatLiveData.addSource(it, merger) }
    return concatLiveData
}

fun <X, Y, Z> zip(x: LiveData<X>, z: LiveData<Z>, merge: (x: X?, z: Z?) -> Y?): LiveData<Y> {
    val mergeLiveData = MediatorLiveData<Y>()
    mergeLiveData.addSource(x) { xValue ->
        mergeLiveData.value = merge(xValue, z.value)
    }
    mergeLiveData.addSource(z) { zValue ->
        mergeLiveData.value = merge(x.value, zValue)
    }
    return mergeLiveData
}

fun <W, X, Y, Z> zip(w: LiveData<W>, x: LiveData<X>, z: LiveData<Z>, merge: (w: W?, x: X?, z: Z?) -> Y): LiveData<Y> {
    val mergeLiveData = MediatorLiveData<Y>()
    mergeLiveData.addSource(w) { wValue ->
        mergeLiveData.value = merge(wValue, x.value, z.value)
    }
    mergeLiveData.addSource(x) { xValue ->
        mergeLiveData.value = merge(w.value, xValue, z.value)
    }
    mergeLiveData.addSource(z) { zValue ->
        mergeLiveData.value = merge(w.value, x.value, zValue)
    }
    return mergeLiveData
}

/**
 * сливает результаты нескольких livedata в один массив
 * модно использовать например в случае вызова нескольких запросов параллельно
 */
fun <X, Z> zip(xArray: List<LiveData<X>>, merge: (xList: List<X?>) -> Z): LiveData<Z> {
    val mergeLiveData = MediatorLiveData<Z>()
    xArray.forEachIndexed { index1, liveData1 ->
        mergeLiveData.addSource(liveData1) { xResult ->
            val result = xArray.mapIndexed { index2, liveData2 -> if (index1 == index2) xResult else liveData2.value }
            mergeLiveData.postValue(merge(result))
        }
    }
    return mergeLiveData
}

fun <X, Y, Z> zipIf(x: LiveData<X>, z: LiveData<Z>, merge: (x: X?, z: Z?) -> Y, condition: (x: X?, z: Z?) -> Boolean): LiveData<Y> {
    val mergeLiveData = MediatorLiveData<Y>()
    mergeLiveData.addSource(x) { xValue ->
        mergeLiveData.value = if (condition(xValue, z.value)) {
            merge(xValue, z.value)
        } else {
            null
        }
    }
    mergeLiveData.addSource(z) { zValue ->
        mergeLiveData.value = if (condition(x.value, zValue)) {
            merge(x.value, zValue)
        } else {
            null
        }
    }
    return mergeLiveData
}

fun <X, Y> combineLatest(sources: List<LiveData<out X>>, combine: (List<X?>) -> Y): LiveData<Y> {
    val combineLiveData = MediatorLiveData<Y>()
    sources.forEach { source ->
        combineLiveData.addSource(source) {
            combineLiveData.value = combine(sources.map { it.value })
        }
    }
    return combineLiveData
}

// region LoadState

fun <D> MutableLiveData<ILoadState<D>>.setEmptyLoadState(
        setOrPost: Boolean = false,
        eagerNotify: Boolean = false
) = setLoadState(null, null, null, setOrPost, eagerNotify)

/**
 * Поместить изменённый или тот же [LoadState] в [LiveData]
 * @param eagerNotify требуется ли поместить значение даже при отсутствии изменений
 */
fun <D> MutableLiveData<ILoadState<D>>.setLoadState(
        data: D? = null,
        isLoading: Boolean? = null,
        error: Throwable? = null,
        setOrPost: Boolean = false,
        eagerNotify: Boolean = false,
        shouldNotify: Boolean = true
): ILoadState<D> {
    val state = value
    val result = state.getOrCreate(data, isLoading, error)
    if (eagerNotify || result.second) {
        setOrPost(result.first, setOrPost, shouldNotify)
    }
    return result.first
}

fun <D> MutableLiveData<ILoadState<D>>.preLoad(
        setOrPost: Boolean = false,
        eagerNotify: Boolean = false,
        shouldNotify: Boolean = true,
        createEmptyStateFunc: () -> ILoadState<D> = { LoadState() }
): ILoadState<D> {
    var hasChanged = false
    var state = value
    if (state == null) {
        state = createEmptyStateFunc()
        hasChanged = true
    }
    hasChanged = state.preLoad() || hasChanged
    if (eagerNotify || hasChanged) {
        setOrPost(state, setOrPost, shouldNotify)
    }
    return state
}

fun <D> MutableLiveData<ILoadState<D>>.successLoad(
        data: D,
        setOrPost: Boolean = false,
        eagerNotify: Boolean = false,
        shouldNotify: Boolean = true,
        createEmptyStateFunc: () -> ILoadState<D> = { LoadState() }
): ILoadState<D> {
    var hasChanged = false
    var state = value
    if (state == null) {
        state = createEmptyStateFunc()
        hasChanged = true
    }
    hasChanged = state.successLoad(data) || hasChanged
    if (eagerNotify || hasChanged) {
        setOrPost(state, setOrPost, shouldNotify)
    }
    return state
}

fun <D> MutableLiveData<ILoadState<D>>.errorLoad(
        error: Throwable,
        setOrPost: Boolean = false,
        eagerNotify: Boolean = false,
        shouldNotify: Boolean = true,
        createEmptyStateFunc: () -> ILoadState<D> = { LoadState() }
): ILoadState<D> {
    var hasChanged = false
    var state = value
    if (state == null) {
        state = createEmptyStateFunc()
        hasChanged = true
    }
    hasChanged = state.errorLoad(error) || hasChanged
    if (eagerNotify || hasChanged) {
        setOrPost(state, setOrPost, shouldNotify)
    }
    return state
}

fun <D> MutableLiveData<D>.bindValidate(vararg validators: BaseValidator<D>) {
    observeForever { data ->
        validators.forEach {
            it.validate(data)
        }
    }
}

/**
 * Для случая, когда [validators] принимают не исходный тип [D], а комбинированный [T]
 */
fun <D, T> MutableLiveData<D>.bindValidate(validators: Collection<BaseValidator<T>>, mapFunc: (D) -> T) {
    observeForever { data ->
        validators.forEach {
            it.validate(mapFunc(data))
        }
    }
}

fun MutableLiveData<*>.bindClearError(vararg validators: BaseValidator<*>) {
    observeForever {
        validators.forEach {
            it.clearError()
        }
    }
}

private fun <D> MutableLiveData<ILoadState<D>>.setOrPost(
        state: ILoadState<D>,
        setOrPost: Boolean,
        shouldNotify: Boolean
) {
    if (this is NotifyCheckMutableLiveData<ILoadState<D>>) {
        if (setOrPost) {
            setValue(state, shouldNotify)
        } else {
            postValue(state, shouldNotify)
        }
    } else {
        if (setOrPost) {
            setValue(state)
        } else {
            postValue(state)
        }
    }
}

// endregion

private class OnceObserver<T>(val liveData: LiveData<T>, val observer: Observer<T>) : Observer<T> {
    override fun onChanged(data: T?) {
        observer.onChanged(data)
        liveData.removeObserver(this)
    }
}

private class UpdatingLiveData<X>(private val intervalMillis: Long) : MediatorLiveData<X>() {
    private val handler = Handler()
    private val updateRunnable = Runnable {
        update()
    }

    private fun update() {
        value = value
        handler.postDelayed(updateRunnable, intervalMillis)
    }

    override fun onActive() {
        super.onActive()
        update()
    }

    override fun onInactive() {
        super.onInactive()
        handler.removeCallbacks(updateRunnable)
    }
}

/**
 * @param sourceLiveData исходная [LiveData] с данными для преобразования
 * @param paramsFunc лямбда, дающая в зав-ти от текущих данных [X]
 * целевое время для отсчёта (0 - без таймера, выставление в эту LD сразу) + интервал
 * @param transformFunc функция преобразования исходных [X] в [Y]
 * [X] - текущее значение в [sourceLiveData]
 * [Long] - оставшиеся миллисекунды до конца (0, если не запускался) при старте таймера
 * [Long] - оставшиеся миллисекунды до конца (0, если не запускался) текущие
 * [Boolean] - был ли в этот раз запущен таймер
 */
private class UpdatingTransformLiveData<X, Y>(
    private val sourceLiveData: LiveData<X>,
    private val paramsFunc: (X) -> Pair<Long, Long>?,
    private val transformFunc: (X?, Long, Boolean) -> Y,
) : MediatorLiveData<Y>() {

    private var observer: UpdatingObserver? = null

    private var timer: CountDownTimer? = null

    override fun onActive() {
        super.onActive()
        // кол-во подписчиков 0 -> 1
        with(UpdatingObserver()) {
            sourceLiveData.observeForever(this)
            observer = this
        }
        // таймер с отложенными значениями запускать не надо
        // и запоминать их заранее тоже - реагируем на значение в UpdatingObserver по месту
    }

    override fun onInactive() {
        super.onInactive()
        // кол-во подписчиков стало 0
        dispose()
    }

    fun dispose() {
        disposeTimer()
        observer?.let {
            sourceLiveData.removeObserver(it)
            observer = null
        }
    }

    private fun update(millisUntilFinished: Long, timerWasStarted: Boolean) {
        this.value = transformFunc(sourceLiveData.value,
            millisUntilFinished.takeIf { it >= 0 } ?: 0,
            timerWasStarted)
    }

    private fun startTimer(params: Pair<Long, Long>) {
        with(Timer(params.first, params.second)) {
            timer = this
            start()
        }
    }

    private fun disposeTimer() {
        timer?.let {
            it.cancel()
            timer = null
        }
    }

    inner class UpdatingObserver : Observer<X> {

        override fun onChanged(t: X) {
            // при каждом изменении исходного значения
            // прекращаем текущий счёт, если был начат
            disposeTimer()
            paramsFunc(t)?.takeIf { it.first > 0 && it.second > 0 }?.let { params ->
                if (hasActiveObservers()) {
                    // и запускаем с новыми параметрами, при наличии активных слушателей
                    startTimer(params)
                }
            } ?: update(0, false)
            // отсчёт не требуется, просто рефрешим
        }
    }

    /**
     * Цель таймера - досчитать до указанного значения,
     * меняя за каждый интервал значение в целевой [MediatorLiveData]
     * с использованием предоставленной [transformFunc]
     */
    inner class Timer(
        millisInFuture: Long,
        intervalMillis: Long,
    ) : CountDownTimer(millisInFuture, intervalMillis) {

        override fun onTick(millisUntilFinished: Long) {
            update(millisUntilFinished, true)
        }

        override fun onFinish() {
            update(0,true)
            timer = null
        }
    }
}