package net.maxsmr.commonutils.data.entity

interface EmptyValidable : Validable {

    fun isEmpty(): Boolean

    override fun isValid() = !isEmpty()
}