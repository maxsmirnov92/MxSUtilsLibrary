package net.maxsmr.commonutils.gui.actions.message.text

import android.content.Context
import android.text.TextUtils
import androidx.annotation.StringRes
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * @param partsToJoin аргументы для объединения в одну строку с использованием [dividerResId]
 */
class JoinTextMessage internal constructor(
    divider: CharSequence?,
    @StringRes dividerResId: Int?,
    vararg partsToJoin: Any?,
) : TextMessage(null, null) {
    // запрещаем использовать args, не относящиеся к объединению в строку,
    // в такой реализации

    var divider: CharSequence? = divider
        protected set

    @StringRes
    var dividerResId: Int? = dividerResId
        protected set

    var partsToJoin: Array<Arg<*>> = partsToJoin.transformArgs()
        private set

    constructor(divider: CharSequence, vararg partsToJoin: Any) : this(divider, null, *partsToJoin)

    constructor(@StringRes dividerResId: Int, vararg partsToJoin: Any?) : this(null, dividerResId, *partsToJoin)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JoinTextMessage) return false
        if (!super.equals(other)) return false

        if (divider != other.divider) return false
        if (dividerResId != other.dividerResId) return false
        if (!partsToJoin.contentEquals(other.partsToJoin)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (divider?.hashCode() ?: 0)
        result = 31 * result + (dividerResId ?: 0)
        result = 31 * result + partsToJoin.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "JoinTextMessage(dividerResId=$dividerResId, partsToJoin=${partsToJoin.contentToString()}, super=${super.toString()})"
    }

    override fun get(context: Context): CharSequence {
        return if (partsToJoin.isNotEmpty()) {
            val _divider = divider
            val dividerResId = dividerResId
            val divider = when {
                !_divider.isNullOrEmpty() -> {
                    _divider
                }
                dividerResId != null && dividerResId != 0 -> {
                    context.getString(dividerResId)
                }
                else -> EMPTY_STRING
            }
            TextUtils.join(divider, partsToJoin.flattenArgs(context))
        } else {
            EMPTY_STRING
        }
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        divider = aInputStream.readUTF()
        dividerResId = aInputStream.readInt()
        partsToJoin = aInputStream.readArgs()
        readFields(aInputStream)
    }

    @Throws(IOException::class)
    private fun writeObject(aOutputStream: ObjectOutputStream) {
        aOutputStream.writeUTF(divider?.toString() ?: EMPTY_STRING)
        aOutputStream.writeInt(dividerResId ?: 0)
        aOutputStream.writeArgs(partsToJoin)
        writeFields(aOutputStream)
    }
}