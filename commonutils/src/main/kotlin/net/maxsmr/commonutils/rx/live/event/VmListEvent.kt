package net.maxsmr.commonutils.rx.live.event

import java.util.*

/**
 * Похожий по смыслу на [VmEvent], но с запоминанием очереди входящих ивентов
 */
data class VmListEvent<T>(
        private val list: LinkedList<T> = LinkedList()
) {

    constructor(value: T) : this() {
        add(value)
    }

    constructor(collection: Collection<T>) : this() {
        addAll(collection)
    }

    fun getAll(): List<T> {
        val result = mutableListOf<T>()
        do {
            val value = get()
            value?.let {
                result.add(it)
            }
        } while (value != null)
        return result
    }

    fun get(): T? = if (list.isNotEmpty()) list.pollFirst() else null

    fun add(value: T) {
        list.add(value)
    }

    fun addAll(collection: Collection<T>) {
        list.addAll(collection)
    }

    fun clear() {
        list.clear()
    }

    fun new(value: T): VmListEvent<T> {
        val event = VmListEvent(getAll())
        event.add(value)
        return event
    }

    fun new(collection: Collection<T>): VmListEvent<T> {
        val event = VmListEvent(getAll())
        addAll(collection)
        return event
    }
}