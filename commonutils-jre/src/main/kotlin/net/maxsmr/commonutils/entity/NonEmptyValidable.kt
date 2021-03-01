package net.maxsmr.commonutils.entity

/**
 * [EmptyValidable], который всегда не пуст
 */
class NonEmptyValidable : EmptyValidable {

    override fun isEmpty(): Boolean = false
}