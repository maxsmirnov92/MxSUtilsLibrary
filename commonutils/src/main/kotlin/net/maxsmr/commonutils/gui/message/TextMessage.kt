package net.maxsmr.commonutils.gui.message

import android.content.Context
import androidx.annotation.StringRes
import com.google.gson.internal.LazilyParsedNumber
import net.maxsmr.commonutils.gui.message.TextMessage.Arg
import net.maxsmr.commonutils.gui.message.TextMessage.ResArg
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Класс для формирования текстового сообщения в условиях отсутствия контекста (например, во ViewModel).
 * Само сообщение может быть извлечено позднее с помощью метода [get] с передачей контекста активити/фрагмента/view.
 * Контекст приложения использовать некорректно, т.к. в приложении язык может отличаться от системного.
 *
 * Допустимые варианты создания сообщения:
 * 1. С помощью [CharSequence] (Внимание, при сериализации/десериализации интерпретируется
 * как строка, т.е. может быть потеря информации, например о спанах). **Пример: TextMessage("Произвольное сообщение")**
 * 1. С помощью форматированной строки с аргументами. **Пример: TextMessage("В вагоне %s %d свободных мест", ResArg(R.string.car_type), 5)**
 * 1. С помощью строки из ресурсов.
 *      * без аргументов. **Пример: TextMessage(R.string.some_message)**
 *      * с аргументами. **Пример: TextMessage(R.string.some_message_args, ResArg(R.string.car_type), "SomeArg")**
 *
 * Для передачи в качестве аргумента другого строкового ресурса, необходимо его оборачивать в [ResArg],
 * чтобы его можно было отличить от простого целочисленного аргумента. Аргументы прочих типов можно
 * не оборачивать в соответствующие наследники [Arg] для лаконичности - они будут обернуты автоматически.
 *
 * @param message строковое сообщение. При отсутствии [args] отображается как есть, иначе используется [String.format].
 * Приоритетнее, чем [messageResId]
 * @param messageResId ид ресурса сообщения.
 * @param args массив аргументов. Допустимые значения:
 * 1. любой наследник [Arg]
 * 1. любая реализация [CharSequence] (Внимание, при сериализации/десериализации интерпретируется
 * как строка, т.е. может быть потеря информации, например о спанах)
 * 1. любой наследник [Number]
 * 1. другой [TextMessage]
 *
 * @see PluralTextMessage
 */
open class TextMessage internal constructor(
    message: CharSequence?,
    @StringRes messageResId: Int?,
    vararg args: Any?,
) : Serializable {

    var message: CharSequence? = message
        protected set

    @StringRes
    var messageResId: Int? = messageResId
        protected set

    var args: Array<Arg<*>> = args.transformArgs()
        protected set

    constructor(message: CharSequence, vararg args: Any) : this(message, null, *args)

    constructor(@StringRes messageResId: Int, vararg args: Any?) : this(null, messageResId, *args)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextMessage) return false

        if (message != other.message) return false
        if (messageResId != other.messageResId) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message?.hashCode() ?: 0
        result = 31 * result + (messageResId ?: 0)
        result = 31 * result + args.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "TextMessage(message=$message, messageResId=$messageResId, args=${args.contentToString()})"
    }

    open fun get(context: Context): CharSequence {
        return getWithArgs(context, args.flattenArgs(context))
    }

    fun argValueAt(context: Context, index: Int): String? = args.getOrNull(index)?.get(context)?.toString()

    protected fun getWithArgs(context: Context, args: Array<Any>): CharSequence {
        val message = message
        val messageRes = messageResId
        return when {
            !message.isNullOrEmpty() -> {
                if (args.isEmpty()) message else String.format(message.toString(), *args)
            }
            messageRes != null && messageRes != 0 -> {
                context.getString(messageRes, *args)
            }
            else -> EMPTY_STRING
        }
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        readFields(aInputStream)
    }

    @Throws(IOException::class)
    private fun writeObject(aOutputStream: ObjectOutputStream) {
        writeFields(aOutputStream)
    }

    interface Arg<out T : Any> : Serializable {

        fun get(context: Context): T
    }

    private data class NumArg<T : Number>(val value: T) : Arg<T> {

        override fun get(context: Context): T = value
    }

    data class ResArg(@StringRes val value: Int, val args: List<Any>? = null) : Arg<String> {

        override fun get(context: Context): String = if (args == null || args.isEmpty()) {
            context.getString(value)
        } else {
            context.getString(value, *args.toTypedArray())
        }
    }

    data class CharSequenceArg(val value: CharSequence) : Arg<CharSequence> {

        override fun get(context: Context): CharSequence = value
    }

    private data class MessageArg(val value: TextMessage) : Arg<CharSequence> {

        override fun get(context: Context): CharSequence = value.get(context)
    }

    companion object {

        @JvmField
        val EMPTY = TextMessage(null, null)

        @JvmStatic
        fun TextMessage?.orEmpty(): TextMessage = this ?: EMPTY

        @Throws(ClassNotFoundException::class, IOException::class)
        fun TextMessage.readFields(aInputStream: ObjectInputStream) {
            message = aInputStream.readUTF()
            messageResId = aInputStream.readInt()
            args = aInputStream.readArgs()
        }

        @Throws(IOException::class)
        fun TextMessage.writeFields(aOutputStream: ObjectOutputStream) {
            aOutputStream.writeUTF(message?.toString() ?: EMPTY_STRING)
            aOutputStream.writeInt(messageResId ?: 0)
            aOutputStream.writeArgs(args)
        }

        fun ObjectInputStream.readArgs(): Array<Arg<*>>  {
            val args = mutableListOf<Arg<Any>>()
            val count = readInt()
            for (i in 0 until count) {
                try {
                    Class.forName(readUTF())
                } catch (e: ClassNotFoundException) {
                    null
                }?.let { argsClass ->
                    when {
                        argsClass.isAssignableFrom(NumArg::class.java) -> NumArg(LazilyParsedNumber(readUTF()))
                        argsClass.isAssignableFrom(ResArg::class.java) -> ResArg(readInt())
                        argsClass.isAssignableFrom(CharSequenceArg::class.java) -> CharSequenceArg(readUTF())
                        argsClass.isAssignableFrom(MessageArg::class.java) -> MessageArg(readObject() as TextMessage)
                        else -> null
                    }?.let {
                        args.add(it)
                    }
                }
            }
            return args.toTypedArray()
        }

        fun ObjectOutputStream.writeArgs(args: Array<Arg<*>>) {
            writeInt(args.size)
            args.forEach {
                when (it) {
                    is NumArg -> {
                        writeUTF(NumArg::class.java.name)
                        writeUTF(it.value.toString())
                    }
                    is ResArg -> {
                        writeUTF(ResArg::class.java.name)
                        writeInt(it.value)
                    }
                    is CharSequenceArg -> {
                        writeUTF(CharSequenceArg::class.java.name)
                        writeUTF(it.value.toString())
                    }
                    is MessageArg -> {
                        writeUTF(MessageArg::class.java.name)
                        writeObject(it.value)
                    }
                }
            }
        }

        fun <T : Any> Array<out T?>.transformArgs(): Array<Arg<*>> {
            val result = mutableListOf<Any?>()
            forEach {
                if (it is Collection<Any?>) {
                    it.forEach { item ->
                        result.add(item)
                    }
                } else {
                    result.add(it)
                }
            }
            result.toTypedArray()

            return result.filterNotNull().map {
                when (it) {
                    is Arg<*> -> it
                    is CharSequence -> CharSequenceArg(it)
                    is Number -> NumArg(it)
                    is TextMessage -> MessageArg(it)
                    else -> {
                        throw IllegalArgumentException("Unexpected arg $it")
                    }
                }
            }.toTypedArray()
        }

        fun Array<Arg<*>>.flattenArgs(context: Context): Array<Any> {
            return map { it.get(context) }.toTypedArray()
        }
    }
}