package net.maxsmr.commonutils.flow.field

import android.content.Context
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import java.io.Serializable
import java.util.Locale


@Suppress("unused", "MemberVisibilityCanBePrivate")
@MainThread
class Field<T> private constructor(
    persistable: Persistable?,
    val valueFlow: StateFlow<T>,
    private val _valueFlow: MutableSharedFlow<T>,
    private val scope: CoroutineScope,
    private val setValueFunction: (T) -> Unit,
    private val getValueFunction: () -> T,
) {

    /**
     * Признак того, что поле является обязательным для заполнения
     */
    val requiredFlow by lazy {
        _requiredFlow.asStateFlow()
    }

    val required: Boolean get() = requiredFlow.value

    val isEmptyFlow: StateFlow<Boolean> by lazy {
        _valueFlow
            .map { validateEmpty() }
            .stateIn(scope, SharingStarted.Eagerly, false)
    }

    /**
     * Возвращает признак отсутствия данных в поле сейчас
     */
    val isEmpty: Boolean get() = isEmptyFlow.value

    /**
     * Поле с текущим значением по ошибке
     */
    val errorFlow: StateFlow<TextMessage?> by lazy {
        _errorFlow.asStateFlow()
    }

    val error: TextMessage? get() = errorFlow.value

    /**
     * Возвращает true, если ошибка выставлялась ранее, иначе false
     */
    val hasError: Boolean get() = error != null

    /**
     * Observable вариант [hint]. Текст подсказки может меняться, если поле обязательное
     */
    val hintFlow: StateFlow<Hint?> by lazy {
        isEmptyFlow.map {
            val message = hintMessage ?: return@map null
            return@map if (!requiredFlow.value || !withAsterisk) {
                Hint(message, false, withCaps, requiredDescriptionResId)
            } else {
                Hint(message, it, withCaps, requiredDescriptionResId)
            }
        }.stateIn(scope, SharingStarted.Eagerly, null)
    }

    /**
     * Возвращает текущую подсказку поля, либо null, если подсказка не была задана при создании поля
     */
    val hint: Hint? get() = hintFlow.value

    val hasHint: Boolean get() = hint != null

    private val _requiredFlow = MutableStateFlow(false)

    private val _errorFlow = MutableStateFlow<TextMessage?>(null)

    var value: T
        get() = getValueFunction()
        set(value) {
            setValueFunction(value)
        }

    var validators: Array<out Validator<T>> = emptyArray()
        set(value) {
            field = value
            recharge()
        }

    private lateinit var emptyPredicate: (T) -> Boolean
    private var emptyMessage: TextMessage? = null

    private var hintMessage: TextMessage? = null

    @StringRes
    private var requiredDescriptionResId: Int = 0
    private var withAsterisk: Boolean = true
    private var withCaps: Boolean = true

    init {
        if (persistable != null) {
            scope.launch {
                valueFlow.collectLatest {
                    persistable.handle[persistable.key] = it
                }
            }
        }
    }

    /**
     * @return true, если проверка по всем валидаторам прошла
     */
    fun validateAndSet(): Boolean {
        val result = validate()
        _errorFlow.value = result
        return result == null
    }

    /**
     * Валидация в зав-ти от обязательности данного поля
     * @param ifEmpty при true необязательное поле будет валидироваться если непустое
     */
    @JvmOverloads
    fun validateAndSetByRequired(ifEmpty: Boolean = true): Boolean {
        return if (!requiredFlow.value && (!ifEmpty || isEmpty)) {
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
        _errorFlow.value = null
    }

    fun recharge() {
        // повторно отправляем текущее значение
        _valueFlow.tryEmit(valueFlow.value)
    }

    fun setNonRequired() {
        setRequired(false, null)
    }

    fun setRequired(
        required: Boolean,
        @StringRes emptyMessageResId: Int,
    ) {
        setRequired(required, TextMessage(emptyMessageResId))
    }

    fun setRequired(
        required: Boolean,
        emptyMessage: TextMessage?,
    ) {
        _requiredFlow.value = required
        this.emptyMessage = emptyMessage
        recharge()
    }

    private fun validateEmpty(): Boolean {
        val field = value
        return field == null || emptyPredicate(field)
    }

    @JvmOverloads
    fun setHint(
        @StringRes hintRes: Int,
        @StringRes requiredDescriptionResId: Int? = null,
        withAsterisk: Boolean = true,
        withCaps: Boolean = false
    ) = setHint(
        TextMessage(hintRes),
        requiredDescriptionResId,
        withAsterisk,
        withCaps
    )

    @JvmOverloads
    fun setHint(
        hint: TextMessage,
        @StringRes requiredDescriptionResId: Int? = null,
        withAsterisk: Boolean = true,
        withCaps: Boolean = false
    ) = apply {
        this.hintMessage = hint
        this.requiredDescriptionResId = requiredDescriptionResId ?: 0
        this.withAsterisk = withAsterisk
        this.withCaps = withCaps
        recharge()
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
    class Hint internal constructor(
        private val hint: TextMessage,
        private val withAsterisk: Boolean,
        private val withCaps: Boolean,
        @StringRes private val requiredDescriptionResId: Int
    ) : Serializable {

        /**
         * Возвращает текстовку текущей подсказки поля
         */
        fun get(context: Context, formatHint: ((String) -> String)? = null): String {
            var hint = if (!withCaps) {
                hint.get(context).toString()
            } else {
                hint.get(context).toString().uppercase(Locale.getDefault())
            }
            hint = formatHint?.invoke(hint) ?: hint
            return if (withAsterisk) {
                "$hint *"
            } else {
                hint
            }
        }

        /**
         * Возвращает описание поля. При наличии "*" заменяет ее на стандартную текстовку про обязательность ввода
         */
        fun contentDescription(context: Context): CharSequence? = if (withAsterisk) {
            get(context).getReplacedAsteriskContentDescription(context)
        } else {
            get(context)
        }

        fun CharSequence?.getReplacedAsteriskContentDescription(context: Context): CharSequence? {
            this ?: return null
            requiredDescriptionResId.takeIf { it != 0 } ?: return null
            if (!this.contains("*")) return this
            return this.toString()
                .replace("*", " ${context.getString(requiredDescriptionResId)}")
        }
    }

    /**
     * Строитель данных поля.
     *
     * Обязательные к вызову методы: [emptyIf]
     */
    open class Builder<T>(
        protected open val initialValue: T,
        private val scope: CoroutineScope,
    ) {

        protected var required: Boolean = false
            private set
        protected var emptyPredicate: ((T) -> Boolean)? = null
            private set
        protected var emptyMessage: TextMessage? = null
            private set
        protected var validators: Array<out Validator<T>> = emptyArray()
            private set
        protected var hint: TextMessage? = null
            private set
        protected var withAsterisk: Boolean = true
            private set
        protected var withCaps: Boolean = true
            private set
        private var persistable: Persistable? = null

        @StringRes
        protected var requiredDescriptionResId: Int? = null
            private set

        /**
         * Устанавливает функцию, определяющую факт отсутствия значения в поле.
         *
         * @param predicate возвращает true, если поле пустое, иначе false
         */
        fun emptyIf(predicate: (T) -> Boolean) = apply {
            this.emptyPredicate = predicate
        }

        /**
         * Устанавливает признак того, что поле обязательное (по умолчанию - не обязательное).
         *
         * @param emptyMessageRes сообщение о незаполненности поля
         */
        fun required(toggle: Boolean, @StringRes emptyMessageRes: Int) = apply {
            required(toggle, TextMessage(emptyMessageRes))
        }

        /**
         * Устанавливает признак того, что поле обязательное (по умолчанию - не обязательное).
         *
         * @param emptyMessage сообщение о незаполненности поля
         */
        fun required(required: Boolean, emptyMessage: TextMessage? = null) = apply {
            this.required = required
            this.emptyMessage = emptyMessage
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

        @JvmOverloads
        fun hint(
            @StringRes hintRes: Int,
            @StringRes requiredDescriptionResId: Int? = null,
            withAsterisk: Boolean = true,
            withCaps: Boolean = true
        ) = hint(
            TextMessage(hintRes),
            requiredDescriptionResId,
            withAsterisk,
            withCaps
        )

        /**
         * Устанавливает текстовку подсказки поля
         *
         * @param hint текстовка подсказки поля
         * @param withAsterisk true, если для пустого **обязательного** поля надо добавлять '*' в подсказку
         * @param requiredDescriptionResId необязательная строка для формирования корректного contentDescription
         */
        @JvmOverloads
        fun hint(
            hint: TextMessage,
            @StringRes requiredDescriptionResId: Int? = null,
            withAsterisk: Boolean = true,
            withCaps: Boolean = true
        ) = apply {
            this.hint = hint
            this.requiredDescriptionResId = requiredDescriptionResId
            this.withAsterisk = withAsterisk
            this.withCaps = withCaps
        }

        /**
         * Позволяет восстановить данные поля после смерти процесса приложения.
         * ВАЖНО! Для сохранения значения тип поля должен быть примитивом, Serializable или Parcelable.
         *
         * @param handle SavedStateHandle фрагмента или активити, куда сохраняется состояние поля для последующего восстановления
         * @param key ключ для сохранения данных поля
         */
        fun persist(handle: SavedStateHandle, key: String) = apply {
            persistable = Persistable(handle, key)
        }

        fun build(): Field<T> {
            val emptyIf = emptyPredicate
                ?: throw IllegalStateException("emptyIf function must be called on Field.Builder")
            val field = createField()
            field._requiredFlow.value = required
            field.emptyPredicate = emptyIf
            field.emptyMessage = emptyMessage
            field.validators = validators
            field.hintMessage = hint
            field.requiredDescriptionResId = requiredDescriptionResId ?: 0
            field.withAsterisk = withAsterisk
            field.withCaps = withCaps
            return field
        }

        private fun createField(): Field<T> {
            val sharedFieldValue = fieldValue()
            val stateFieldValue = sharedFieldValue.stateIn(
                scope,
                SharingStarted.Eagerly,
                initialValue
            )
            return Field(
                persistable,
                stateFieldValue,
                sharedFieldValue,
                scope,
                valueSetter(sharedFieldValue),
                valueGetter(stateFieldValue)
            )
        }

        protected open fun fieldValue(): MutableSharedFlow<T> {
            val p = persistable
            val currentValue = p?.handle?.get<T>(p.key) ?: initialValue
            return MutableSharedFlow<T>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).also {
                it.tryEmit(currentValue)
            }
        }

        protected open fun valueGetter(fieldValue: StateFlow<T>): () -> T = {
            fieldValue.value
        }

        protected open fun valueSetter(
            fieldValue: MutableSharedFlow<T>,
        ): (T) -> Unit = {
            it.checkPersistable()
            fieldValue.tryEmit(it)
        }

        protected fun T.checkPersistable() {
            if (this == null || persistable == null) return
            //Проверка для того, чтобы краш был сразу при первой попытке установки nonSerializable или nonParcelable
            //значения поля при использовании persist, а не при попытке сохранения в Bundle (которая отлавливается не всегда)
            check(this is Serializable || this is Parcelable) {
                "Attempt to persist non serializable or parcelable object: \"${this.let { it::class.java.simpleName }}\"."
            }
        }
    }

    private class Persistable(
        val handle: SavedStateHandle,
        val key: String
    )
}