package net.maxsmr.commonutils.live.event

import androidx.annotation.MainThread

/**
 * Ипользуйте в составе LiveData<VmEvent<T>> для оповещения View об одноразовых событиях из VM.
 * Например, если нужно заэмитить эвент, по получению которого View должна отобразить диалог/тост итд.
 * Если ViewModel расшаривается между фрагментами, и более 1 фрагмента подписано на LiveData<VmEvent<T>>,
 * эвент получит только один из них.
 *
 * Альтернативы:
 * 1. Если использовать LiveData без обертки VmEvent - View будет реагировать на него не 1 раз, а после
 * каждой смены конфигурации.
 * 1. Если использовать [ru.railways.core.android.utils.rx.LiveSubject] - то View получит эвент в том случае,
 * если находится в состоянии RESUMED. Или пользовать freezeSelector вместо фильтра по состоянию.
 *
 * @see SharedVmEvent
 */
class VmEvent<T>(value: T) {

    private var value: T? = value

    @JvmOverloads
    @MainThread
    fun get(consume: Boolean = true): T? {
        val res = value ?: return null
        if (consume) value = null
        return res
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VmEvent<*>) return false

        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }
}