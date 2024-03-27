package net.maxsmr.commonutils.live.field

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage
import net.maxsmr.commonutils.live.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.Serializable


@Suppress("unused", "MemberVisibilityCanBePrivate")
class Field<T> private constructor(
    private val _value: MutableLiveData<T>,
    private val setValueFunction: (T?) -> Boolean,
    private val getValueFunction: () -> T?,
) {

    private lateinit var emptyPredicate: (T) -> Boolean
    private var emptyMessage: TextMessage? = null

    /**
     * Признак того, что поле является обязательным для заполнения
     */
    val required: Boolean get() = emptyMessage != null

    val valueLive: LiveData<T> get() = _value

    /**
     * [LiveData] с изменением на предмет пустоты в динамике;
     * Если поле необязательное - обозревать нет необходимости.
     */
    val isEmptyLive: LiveData<Boolean> get() = valueLive.map { validateEmpty() }.distinct()

    /**
     * Возвращает признак отсутствия данных в поле сейчас
     */
    val isEmpty: Boolean get() = isEmptyLive.value ?: false

    private val _error = MutableLiveData<TextMessage?>()

    /**
     * Поле с текущим значением по ошибке
     */
    val errorLive: LiveData<TextMessage?> = _error

    val error: TextMessage? get() = errorLive.value

    /**
     * Возвращает true, если ошибка выставлялась ранее, иначе false
     */
    val hasError: Boolean get() = error != null

    /**
     * Возвращает текущую подсказку поля, либо null, если подсказка не была задана при создании поля
     */
    val hint: Hint?
        get() = hintMessage?.let { Hint(it, required && isEmpty && withAsterisk, requiredStringResId) }

    /**
     * Observable вариант [hint]. Текст подсказки может меняться, если поле обязательное
     */
    val hintLive: LiveData<Hint?>
        get() {
            val message = hintMessage ?: return MutableLiveData(null)
            return if (!required || !withAsterisk) {
                Hint(message, false, requiredStringResId).just()
            } else {
                isEmptyLive.map { Hint(message, it, requiredStringResId) }
            }
        }

    var wasChangedValue: Boolean = false
        private set

    /**
     * Текущее значение поля;
     * Для сеттера нулабельный тип является потенциально ошибочным
     */
    var value: T?
        get() = getValueFunction()
        set(value) {
            if (setValueFunction(value)) {
                wasChangedValue = true
            }
        }


    var validators: Array<out Validator<T>> = emptyArray()
        set(value) {
            field = value
            recharge()
        }

    private var hintMessage: TextMessage? = null

    @StringRes
    private var requiredStringResId: Int = 0
    private var withAsterisk: Boolean = true

    /**
     * @return true, если проверка по всем валидаторам прошла
     */
    fun validateAndSet(): Boolean {
        val result = validate()
        _error.value = result
        return result == null
    }

    /**
     * Валидация в зав-ти от обязательности данного поля
     */
    fun validateAndSetByRequired(): Boolean {
        return if (!required && isEmpty) {
            // при необязательном пустом поле
            // считаем что валидация прошла
            clearError()
            true
        } else {
            // при обязательности -
            // валидация по всем как обычно
            validateAndSet()
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun recharge() {
        _value.recharge()
    }

    private fun validateEmpty(): Boolean {
        val field = value
        if (field == null || emptyPredicate(field)) return true
        return false
    }

    /**
     * Возвращает текущую ошибку поля, формируемую одним из валидаторов, либо null при отсутствии ошибок
     */
    private fun validate(): TextMessage? {
        val field = value
        if (field == null || emptyPredicate(field)) return emptyMessage
        return validators.find { !it.isValid(field) }?.errorMessageProvider?.invoke(field)
    }

    /**
     * Осуществляет валидацию значения поля на предмет наличия ошибки с **конкретной** текстовкой.
     * Валидаторы вызываются, только если поле **не** пустое (см. [emptyPredicate]).
     *
     * @param errorMessageProvider возвращает сообщение об ошибке на случай, если [validPredicate] возвращает false
     * @param validPredicate функция проверки значения поля на наличие ошибки с **конкретной** текстовкой
     */
    open class Validator<in T>(
        val errorMessageProvider: (T) -> TextMessage,
        private val validPredicate: (T) -> Boolean,
    ) {

        fun isValid(value: T): Boolean = validPredicate(value)

        /**
         * Осуществляет валидацию значения поля на предмет **конкретной** ошибки
         *
         * @param errorMessageRes ресурс сообщения об ошибке на случай, если [validPredicate] возвращает false
         * @param validPredicate функция проверки значения поля на наличие ошибки с **конкретной** текстовкой
         */
        constructor(
            @StringRes errorMessageRes: Int,
            validPredicate: (T) -> Boolean,
        ) : this({ TextMessage(errorMessageRes) }, validPredicate)


        companion object {

            /**
             * Возвращает валидатор, осуществляющий проверку поля по регулярному выражению
             *
             * @param errorMessageRes ресурс сообщения об ошибке, если проверка завершается неуспешно
             * @param regExp строковое представление регулярного выражения
             */
            fun regExp(@StringRes errorMessageRes: Int, regExp: String?): Validator<String?> {
                return Validator(errorMessageRes) {
                    it ?: return@Validator true
                    regExp ?: return@Validator true
                    it.matches(regExp.toRegex())
                }
            }
        }
    }

    /**
     * Хранит данные о текущей подсказке поля
     */
    data class Hint(
        private val hint: TextMessage,
        private val withAsterisk: Boolean,
        @StringRes
        private val requiredStringResId: Int
    ) {

        /**
         * Возвращает текстовку текущей подсказки поля
         */
        fun get(context: Context): CharSequence = if (withAsterisk) {
            "${hint.get(context)} *"
        } else {
            hint.get(context)
        }

        /**
         * Возвращает описание поля. При наличии "*" заменяет ее на стандартную текстовку про обязательность ввода
         */
        fun contentDescription(context: Context): CharSequence? = if (withAsterisk) {
            get(context).getReplacedAsteriskText(context)
        } else {
            get(context)
        }

        fun CharSequence?.getReplacedAsteriskText(context: Context): CharSequence? {
            this ?: return null
            if (!this.contains("*")) return this
            return this.toString()
                .replace("*", " ${context.getString(requiredStringResId)}")
        }
    }

    /**
     * Строитель данных поля.
     *
     * Обязательные к вызову методы: [emptyIf]
     */
    open class Builder<T>(
        protected open val initialValue: T,
    ) {

        protected var emptyPredicate: ((T) -> Boolean)? = null
            private set
        protected var emptyMessage: TextMessage? = null
            private set
        protected var validators: Array<out Validator<T>> = emptyArray()
            private set
        protected var handle: SavedStateHandle? = null
            private set
        protected var key: String = ""
            private set
        protected var hint: TextMessage? = null
            private set

        @StringRes
        protected var requiredStringResId: Int? = null
            private set
        protected var withAsterisk: Boolean = true
            private set
        protected var distinctUntilChanged: Boolean = true
            private set

        /**
         * Устанавливает функцию, определяющую факт отстутствия значения в поле.
         *
         * @param predicate возвращает true, если поле пустое, иначе false
         */
        fun emptyIf(predicate: (T) -> Boolean) = apply {
            this.emptyPredicate = predicate
        }

        /**
         * Устанавливает признак того, что поле обязательное (по умолчанию - не обязательное).
         *
         * @param emptyMessage сообщение о незаполненности поля
         */
        fun setRequired(emptyMessage: TextMessage) = apply {
            this.emptyMessage = emptyMessage
        }

        /**
         * Устанавливает признак того, что поле обязательное (по умолчанию - не обязательное).
         *
         * @param emptyMessageRes сообщение о незаполненности поля
         */
        fun setRequired(@StringRes emptyMessageRes: Int) = apply {
            this.emptyMessage = TextMessage(emptyMessageRes)
        }

        /**
         * Устанавливает правила валидации значения поля в виде цепочки валидаторов.
         * Количество валидаторов определяется числом возможных уникальных сообщений об ошибках поля.
         * При выполнении проверки наличия ошибки в поле управление передается последовательно по
         * цепочке валидаторов, т.е. **порядок объявления валидаторов важен**.
         * Валидаторы проверяются, только если поле **не** пустое (см. [emptyIf])
         *
         * @see Validator
         */
        fun validators(vararg validators: Validator<T>) = apply {
            this.validators = validators
        }

        /**
         * Устанавливает текстовку подсказки поля
         *
         * @param hintRes ресурс текстовки подсказки поля
         * @param withAsterisk true, если для пустого **обязательного** поля надо добавлять '*' в подсказку
         */
        @JvmOverloads
        fun hint(
            @StringRes hintRes: Int,
            @StringRes
            requiredStringResId: Int,
            withAsterisk: Boolean = true
        ) = hint(TextMessage(hintRes), requiredStringResId, withAsterisk)

        /**
         * Устанавливает текстовку подсказки поля
         *
         * @param hint текстовка подсказки поля
         * @param withAsterisk true, если для пустого **обязательного** поля надо добавлять '*' в подсказку
         */
        @JvmOverloads
        fun hint(
            hint: TextMessage,
            @StringRes
            requiredStringResId: Int,
            withAsterisk: Boolean = true
        ) = apply {
            this.hint = hint
            this.requiredStringResId = requiredStringResId
            this.withAsterisk = withAsterisk
        }

        /**
         * Устанавливает distinctUntilChanged логику эмита значений поля. По умолчанию true.
         *
         * @param distinctUntilChanged true, если при установке значения поля, эквивалетнтному текущему значению,
         * эмитить его не надо. False, если эмитить одинаковые значения надо
         */
        @JvmOverloads
        fun setDistinctUntilChanged(distinctUntilChanged: Boolean = true) = apply {
            this.distinctUntilChanged = distinctUntilChanged
        }

        /**
         * Позволяет восстановить данные поля после смерти процесса приложения.
         * ВАЖНО! Для сохранения значения тип поля должен быть примитивом, Serializable или Parcelable.
         *
         * @param handle SavedStateHandle фрагмента или активити, куда сохраняется состояние поля для последующего восстановления
         * @param key ключ для сохранения данных поля
         */
        fun persist(handle: SavedStateHandle, key: String) = apply {
            this.handle = handle
            this.key = key
        }

        fun build(): Field<T> {
            val emptyIf = emptyPredicate
                ?: throw IllegalStateException("emptyIf function must be called on Field.Builder")
            val field = createField(distinctUntilChanged)
            field.emptyPredicate = emptyIf
            field.emptyMessage = emptyMessage
            field.validators = validators
            field.hintMessage = hint
            field.requiredStringResId = requiredStringResId ?: 0
            field.withAsterisk = withAsterisk
            return field
        }

        private fun createField(distinctUntilChanged: Boolean): Field<T> {
            val fieldValue = fieldValue()
            return Field(fieldValue, valueSetter(fieldValue, distinctUntilChanged), valueGetter(fieldValue))
        }

        protected open fun fieldValue(): MutableLiveData<T> =
            handle?.getLiveData(key, initialValue) ?: MutableLiveData<T>(initialValue)

        protected open fun valueSetter(fieldValue: MutableLiveData<T>, distinctUntilChanged: Boolean): (T?) -> Boolean = {
            it.checkPersistable()
            if (distinctUntilChanged) {
                fieldValue.setValueIfNew(it)
            } else {
                fieldValue.value = it
                true
            }
        }

        protected fun T?.checkPersistable() {
            if (this == null || handle == null) return
            //Проверка для того, чтобы краш был сразу при первой попытке установки nonSerializable или nonParcelable
            //значения поля при использовании persist, а не при попытке сохранения в Bundle (которая отлавливается не всегда)
            check(this is Serializable || this is Parcelable) {
                "Attempt to persist non serializable or parcelable value."
            }
        }

        protected open fun valueGetter(fieldValue: MutableLiveData<T>): () -> T? = {
            fieldValue.value
        }
    }

    class NonNullStringBuilder @JvmOverloads constructor(
        initialValue: String = EMPTY_STRING,
    ) : Builder<String>(initialValue) {

        override fun valueSetter(fieldValue: MutableLiveData<String>, distinctUntilChanged: Boolean): (String?) -> Boolean = {
            if (distinctUntilChanged) {
                fieldValue.setValueIfNew(it.orEmpty())
            } else {
                fieldValue.value = it.orEmpty()
                true
            }
        }

        override fun valueGetter(fieldValue: MutableLiveData<String>): () -> String? = {
            fieldValue.value.orEmpty()
        }
    }
}