package net.maxsmr.commonutils.gui.message

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Класс для формирования Quantity сообщения в условиях отсутствия контекста (например, во ViewModel).
 * Для более подробного описания см. [TextMessage].
 *
 * @param pluralResId ид quantity ресурса
 * @param quantity количество
 * @param args дополнительные аргументы строки
 *
 * @see TextMessage
 */
@SuppressLint("ResourceType")
class PluralTextMessage(
    @PluralsRes pluralResId: Int,
    quantity: Int,
    vararg args: Any,
) : TextMessage(null, null, *args) {

    @PluralsRes
    var pluralResId: Int = pluralResId
        private set

    var quantity: Int = quantity
        private set

    fun _setMessage(message: CharSequence?) {
        this.message = message
    }

    fun _setMessageResId(@StringRes resId: Int?) {
        messageResId = resId
    }

    override fun get(context: Context): CharSequence {
        val pluralText = context.resources.getQuantityString(pluralResId, quantity, *args.flattenArgs(context))
        return if (!message.isNullOrEmpty() || messageResId != null) {
            getWithArgs(context, arrayOf(pluralText))
        } else {
            pluralText
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PluralTextMessage) return false
        if (!super.equals(other)) return false

        if (pluralResId != other.pluralResId) return false
        if (quantity != other.quantity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + pluralResId
        result = 31 * result + quantity
        return result
    }

    override fun toString(): String {
        return "PluralTextMessage(pluralRes=$pluralResId, quantity=$quantity, super=${super.toString()})"
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        pluralResId = aInputStream.readInt()
        quantity = aInputStream.readInt()
        readFields(aInputStream)
    }

    @Throws(IOException::class)
    private fun writeObject(aOutputStream: ObjectOutputStream) {
        aOutputStream.writeInt(pluralResId)
        aOutputStream.writeInt(quantity)
        writeFields(aOutputStream)
    }
}