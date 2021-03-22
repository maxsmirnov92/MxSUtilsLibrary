package net.maxsmr.commonutils.gui.actions.message.text

import android.content.Context
import androidx.annotation.StringRes
import com.google.gson.internal.LazilyParsedNumber
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

fun TextMessage?.orEmpty(): TextMessage = this ?: TextMessage.EMPTY

/**
 * Текстовое сообщение, может быть инстанциировано либо строкой, либо идентификатором ресурса.
 * Для получения сообщения используется метод [get]. При получении сообщения приоритет у [message].
 * Используется в случаях, когда надо сформировать сообщение, которое может содержать строковые ресурсы,
 * и при этом недоступен контекст, с которым это сообщение будет показано (контекст приложения использовать нельзя,
 * т.к. язык приложения может отличаться от системного).
 */
open class TextMessage internal constructor(
        message: CharSequence?,
        @StringRes messageResId: Int?,
        vararg args: Any,
) : Serializable {

    var message: CharSequence? = message
        private set
    var messageResId: Int? = messageResId
        private set
    var args: Array<Arg<*>> = args.map {
        when (it) {
            is Arg<*> -> it
            is CharSequence -> CharSequenceArg(it)
            is Number -> NumArg(it)
            is TextMessage -> MessageArg(it)
            else -> throw IllegalArgumentException("Unexpected arg $it")
        }
    }.toTypedArray()
        private set

    constructor(message: CharSequence, vararg args: Any) : this(message, null, *args)

    constructor(@StringRes messageResId: Int, vararg args: Any) : this(null, messageResId, *args)

    open fun get(context: Context): CharSequence {
        val message = message
        val messageRes = messageResId
        return when {
            !message.isNullOrEmpty() -> {
                if (args.isEmpty()) message else String.format(message.toString(), *flattenArgs(context))
            }
            messageRes != null && messageRes != 0 -> {
                context.getString(messageRes, *flattenArgs(context))
            }
            else -> EMPTY_STRING
        }
    }

    fun argValueAt(context: Context, index: Int): String? = args.getOrNull(index)?.get(context)?.toString()

    protected fun flattenArgs(context: Context): Array<Any> {
        return args.map { it.get(context) }.toTypedArray()
    }

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

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        readFields(aInputStream)
    }

    @Throws(IOException::class)
    private fun writeObject(aOutputStream: ObjectOutputStream) {
        writeFields(aOutputStream)
    }

    override fun toString(): String {
        return "TextMessage(message=$message, messageResId=$messageResId, args=${args.contentToString()})"
    }

    interface Arg<out T : Any> : Serializable {

        fun get(context: Context): T
    }

    private data class NumArg<T : Number>(val value: T) : Arg<T> {
        override fun get(context: Context): T = value
    }

    data class ResArg(@StringRes val value: Int) : Arg<String> {
        override fun get(context: Context): String = context.getString(value)
    }

    private data class CharSequenceArg(val value: CharSequence) : Arg<CharSequence> {
        override fun get(context: Context): CharSequence = value
    }

    private data class MessageArg(val value: TextMessage) : Arg<CharSequence> {
        override fun get(context: Context): CharSequence = value.get(context)
    }

    companion object {

        @JvmStatic
        val EMPTY = TextMessage(null, null)

        @Throws(ClassNotFoundException::class, IOException::class)
        fun TextMessage.readFields(aInputStream: ObjectInputStream) {
            message = aInputStream.readUTF()
            messageResId = aInputStream.readInt()
            val argsClass = try {
                Class.forName(aInputStream.readUTF())
            } catch (e: ClassNotFoundException) {
                null
            }
            val args = mutableListOf<Arg<Any>>()
            val count = aInputStream.readInt()
            for (i in 0 until count) {
                argsClass?.let {
                    when {
                        argsClass.isAssignableFrom(NumArg::class.java) -> NumArg(LazilyParsedNumber(aInputStream.readUTF()))
                        argsClass.isAssignableFrom(ResArg::class.java) -> ResArg(aInputStream.readInt())
                        argsClass.isAssignableFrom(CharSequenceArg::class.java) -> CharSequenceArg(aInputStream.readUTF())
                        argsClass.isAssignableFrom(MessageArg::class.java) -> MessageArg(aInputStream.readObject() as TextMessage)
                        else -> null
                    }?.let {
                        args.add(it)
                    }
                }
            }
            this.args = args.toTypedArray()
        }

        @Throws(IOException::class)
        fun TextMessage.writeFields(aOutputStream: ObjectOutputStream) {
            aOutputStream.writeUTF(message?.toString() ?: EMPTY_STRING)
            aOutputStream.writeInt(messageResId ?: 0)
            aOutputStream.writeInt(args.size)
            args.forEach {
                when (it) {
                    is NumArg -> {
                        aOutputStream.writeUTF(it.value.toString())
                    }
                    is ResArg -> {
                        aOutputStream.writeInt(it.value)
                    }
                    is CharSequenceArg -> {
                        aOutputStream.writeUTF(it.value.toString())
                    }
                    is MessageArg -> {
                        aOutputStream.writeObject(it.value)
                    }
                }
            }
        }
    }
}