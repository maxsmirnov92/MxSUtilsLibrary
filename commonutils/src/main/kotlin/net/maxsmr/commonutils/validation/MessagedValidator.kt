package net.maxsmr.commonutils.validation

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.live.setValueIfNew

/**
 * [BaseValidator] с записью сообщения об ошибочном результате валидации или его отсутствии в [errorMessageData]
 */
open class MessagedValidator<T>(
        val errorMessageData: MutableLiveData<Int?>,
        @StringRes
        protected val errorMessageResId: Int,
        private val isDistinct: Boolean = true,
        validationFunc: (T) -> Boolean
) : BaseValidator<T>(validationFunc) {

    override var hasError: Boolean
        get() = errorMessageData.value != null
        set(value) {
            val error = if (value) errorMessageResId else null
            if (isDistinct) {
                errorMessageData.setValueIfNew(error)
            } else {
                errorMessageData.value = error
            }
        }
}