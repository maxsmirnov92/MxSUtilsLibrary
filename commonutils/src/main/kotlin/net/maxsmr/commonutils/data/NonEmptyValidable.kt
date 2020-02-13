package net.maxsmr.commonutils.data

/**
 * [EmptyValidable], который всегда не пуст
 */
class NonEmptyValidable : EmptyValidable {

    override fun isEmpty(): Boolean = false
}