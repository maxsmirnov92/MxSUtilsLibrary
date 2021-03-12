package net.maxsmr.commonutils.gui.actions.message.text

import android.content.Context
import androidx.annotation.StringRes
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.Serializable

/**
 * Текстовое сообщение, может быть инстанциировано либо строкой, либо идентификатором ресурса.
 * Для получения сообщения используется метод [get]. При получении сообщения приоритет у [message].
 * Используется в случаях, когда надо сформировать сообщение, которое может содержать строковые ресурсы,
 * и при этом недоступен контекст, с которым это сообщение будет показано (контекст приложения использовать нельзя,
 * т.к. язык приложения может отличаться от системного).
 */
open class TextMessage @JvmOverloads constructor(
        private val message: CharSequence?,
        @StringRes private val messageRes: Int?,
        private vararg val args: Any = emptyArray()// примитивы, сериализуемые объекты Date, Calendar и тп
) {

    private var lambda: ((Context) -> CharSequence)? = null

    constructor(message: CharSequence) : this(message, null)

    @JvmOverloads
    constructor(@StringRes messageRes: Int, vararg args: Any = emptyArray()) : this(null, messageRes, args)

    constructor(lambda: (Context) -> CharSequence) : this(null, null) {
        this.lambda = lambda
    }

    open fun get(context: Context): CharSequence =
            with(lambda) {
                when {
                    this != null -> this.invoke(context)
                    !message.isNullOrEmpty() -> message
                    messageRes != null && messageRes != 0 -> context.getString(messageRes, *args)
                    else -> EMPTY_STRING
                }
            }
}