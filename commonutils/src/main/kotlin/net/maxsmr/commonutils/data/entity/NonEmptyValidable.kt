package net.maxsmr.commonutils.data.entity

import net.maxsmr.commonutils.data.entity.EmptyValidable

/**
 * [EmptyValidable], который всегда не пуст
 */
class NonEmptyValidable : EmptyValidable {

    override fun isEmpty(): Boolean = false
}