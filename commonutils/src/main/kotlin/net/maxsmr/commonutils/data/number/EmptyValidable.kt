package net.maxsmr.commonutils.data.number

/**
 * [Validable] с проверкой на пустоту
 */
interface EmptyValidable : Validable {

    fun isEmpty(): Boolean
}