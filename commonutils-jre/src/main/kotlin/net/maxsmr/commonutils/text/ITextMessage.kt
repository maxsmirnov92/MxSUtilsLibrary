package net.maxsmr.commonutils.text

import java.io.Serializable

interface ITextMessage<T>: Serializable {

    fun get(with: T): CharSequence
}