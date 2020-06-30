package net.maxsmr.commonutils.data.entity

/**
 * [EmptyValidable], который всегда не пуст
 */
class NonEmptyValidable : EmptyValidable {

    override fun isEmpty(): Boolean = false
}