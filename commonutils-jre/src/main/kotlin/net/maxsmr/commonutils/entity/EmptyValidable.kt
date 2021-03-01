package net.maxsmr.commonutils.entity

interface EmptyValidable : Validable {

    fun isEmpty(): Boolean

    override fun isValid() = !isEmpty()
}