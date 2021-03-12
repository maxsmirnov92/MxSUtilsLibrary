package net.maxsmr.commonutils.gui.actions.message.text

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.PluralsRes

@SuppressLint("ResourceType")
class PluralMessage(
        @PluralsRes private val messageRes: Int,
        val quantity: Int,
        private vararg val args: Any = emptyArray()
) : TextMessage(null, messageRes, args) {

    override fun get(context: Context): CharSequence =
            context.resources.getQuantityString(messageRes, quantity, *args)
}