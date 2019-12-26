package net.maxsmr.commonutils.data

interface EmptyValidable : Validable {

    fun isEmpty(): Boolean

    override fun isValid() = !isEmpty()
}