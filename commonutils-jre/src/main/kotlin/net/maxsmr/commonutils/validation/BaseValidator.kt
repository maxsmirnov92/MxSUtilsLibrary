package net.maxsmr.commonutils.validation

/**
 * Валидатор данных с типом [T] посредством [validationFunc]
 */
abstract class BaseValidator<T>(protected val validationFunc: (T) -> Boolean) {

    abstract var hasError: Boolean

    /**
     * @return true при прохождении валидации
     */
    fun validate(data: T): Boolean {
        with(validationFunc(data)) {
            hasError = !this
            return this
        }
    }

    fun clearError() {
        hasError = false
    }
}