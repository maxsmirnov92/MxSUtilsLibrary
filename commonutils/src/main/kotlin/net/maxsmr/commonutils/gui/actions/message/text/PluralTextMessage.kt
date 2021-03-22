package net.maxsmr.commonutils.gui.actions.message.text

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.PluralsRes
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@SuppressLint("ResourceType")
class PluralTextMessage(
        @PluralsRes pluralResId: Int,
        quantity: Int,
        vararg args: Any
) : TextMessage(null, null, *args) {

    @PluralsRes
    var pluralResId: Int = pluralResId
        private set

    var quantity: Int = quantity
        private set

    override fun get(context: Context): CharSequence =
            context.resources.getQuantityString(pluralResId, quantity, *flattenArgs(context))

    override fun toString(): String {
        return "PluralTextMessage(pluralRes=$pluralResId, quantity=$quantity, super=${super.toString()})"
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        readFields(aInputStream)
        pluralResId = aInputStream.readInt()
        quantity = aInputStream.readInt()
    }

    @Throws(IOException::class)
    private fun writeObject(aOutputStream: ObjectOutputStream) {
        writeFields(aOutputStream)
        aOutputStream.writeInt(pluralResId)
        aOutputStream.writeInt(quantity)
    }
}