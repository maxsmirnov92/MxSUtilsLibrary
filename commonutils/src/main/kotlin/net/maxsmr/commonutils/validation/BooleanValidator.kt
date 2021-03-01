package net.maxsmr.commonutils.validation

import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.live.setValueIfNew

/**
 * [BaseValidator] с записью true/false результата валидации в [hasErrorData]
 */
open class BooleanValidator<T>(
        val hasErrorData: MutableLiveData<Boolean>,
        private val isDistinct: Boolean = true,
        validationFunc: (T) -> Boolean
) : BaseValidator<T>(validationFunc) {

    override var hasError: Boolean
        get() = hasErrorData.value == true
        set(value) {
            if (isDistinct) {
                hasErrorData.setValueIfNew(value)
            } else {
                hasErrorData.value = value
            }
        }
}