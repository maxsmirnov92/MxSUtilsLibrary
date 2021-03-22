package net.maxsmr.commonutils.live.event

import androidx.annotation.MainThread
import net.maxsmr.commonutils.gui.actions.BaseViewModelAction
import net.maxsmr.commonutils.Predicate.Methods.filterWithIndex
import net.maxsmr.commonutils.Predicate.Methods.findWithIndex
import net.maxsmr.commonutils.collection.sort.BaseOptionalComparator
import net.maxsmr.commonutils.collection.sort.ISortOption
import net.maxsmr.commonutils.compareInts
import net.maxsmr.commonutils.compareLongs
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.util.*

/**
 * Похожий по смыслу на [VmEvent], но с запоминанием очереди входящих ивентов
 */
@MainThread
class VmListEvent<A : BaseViewModelAction<*>>() {

    private val list: LinkedList<ItemInfo<A>> = LinkedList()

    @JvmOverloads
    constructor(value: A, options: AddOptions = AddOptions()): this() {
        add(value, options)
    }

    constructor(collection: Map<A, AddOptions>): this() {
        addAll(collection)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VmListEvent<*>) return false

        if (list != other.list) return false

        return true
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }

    fun getAll(remove: Boolean) = getAll(remove, null)

    /**
     * @param remove нужно ли удалить вычитанный элемент из очереди
     * @return все итемы, до первого по ходу сингла включительно
     */
    fun getAllBeforeSingle(remove: Boolean) = getAll(remove) {
        !it.isSingle
    }

    fun getAll(remove: Boolean, predicate: ((ItemInfo<A>) -> Boolean)? = null): List<ItemInfo<A>> {
        val result = mutableListOf<ItemInfo<A>>()
        if (remove) {
            do {
                val value = get(true)
                value?.let {
                    result.add(it)
                }
            } while (value != null
                    && (predicate == null || predicate.invoke(value))
            )
        } else {
            var isMatch = true
            // в результирующий список попадают все итемы, соответствующие предикату
            // + первый несоответствующий
            result.addAll(list.filterIndexed { index, item ->
                isMatch.apply {
                    if (isMatch && predicate != null && !predicate.invoke(item)) {
                        isMatch = false
                    }
                }
            })
        }
        return result
    }

    fun get(remove: Boolean): ItemInfo<A>? = if (list.isNotEmpty()) {
        if (remove) {
            list.pollFirst()
        } else {
            list.peekFirst()
        }
    } else {
        null
    }

    @JvmOverloads
    fun add(value: A, options: AddOptions = AddOptions()) {
        val tag = options.tag
        val existingItems = if (tag.isNotEmpty()) {
            filterWithIndex(list) { it.tag == tag }
        } else {
            emptyMap()
        }
        when (options.unique) {
            UniqueStrategy.Ignore -> {
                if (existingItems.isNotEmpty()) {
                    return
                }
            }
            UniqueStrategy.Replace -> {
                existingItems.forEach {
                    list.removeAt(it.key)
                }
            }
            is UniqueStrategy.Custom -> {
                @Suppress("UNCHECKED_CAST")
                options.unique.lambda(list as List<ItemInfo<BaseViewModelAction<*>>>)
            }
            else -> {
            // не меняем
            }
        }
        list.add(ItemInfo(value, options.tag, options.priority.value, options.checkSingle))
        list.sortWith(ItemComparator())
    }

    @JvmOverloads
    fun addAll(collection: Collection<A>, options: AddOptions = AddOptions()) {
        collection.forEach {
            add(it, options)
        }
    }

    fun addAll(collection: Map<A, AddOptions>) {
        collection.forEach {
            add(it.key, it.value)
        }
    }

    fun removeFirstByTag(tag: String): ItemInfo<A>? {
        findWithIndex(list) {
            it.tag == tag
        }?.let {
            list.removeAt(it.first)
            return it.second
        }
        return null
    }

    fun removeAllByTag(tag: String): List<ItemInfo<A>> {
        val new = list.filter { it.tag != tag }
        list.clear()
        list.addAll(new)
        return new
    }

    fun clear() {
        list.clear()
    }

    @JvmOverloads
    fun new(value: A, options: AddOptions = AddOptions()): VmListEvent<A> {
        val event = VmListEvent<A>()
        event.list.addAll(list)
        event.add(value, options)
        return event
    }

    fun new(collection: Map<A, AddOptions>): VmListEvent<A> {
        val event = VmListEvent<A>()
        event.list.addAll(list)
        addAll(collection)
        return event
    }

    @JvmOverloads
    fun new(collection: Collection<A>, options: AddOptions = AddOptions()): VmListEvent<A> {
        val event = VmListEvent<A>()
        event.list.addAll(list)
        addAll(collection, options)
        return event
    }

    /**
     * @param priority приоритет добавляемого [A] по отношению к тем, что уже есть в [list]
     * @param checkSingle обработка событий из очереди, следующих за данным возможна только после текущего
     * (например, следующий(ие) snack может быть выведен только после того как закроется этот - см. конкретные параметры в [A])
     */
    data class AddOptions @JvmOverloads constructor(
            val tag: String = EMPTY_STRING,
            val priority: Priority = Priority.NORMAL,
            val unique: UniqueStrategy = UniqueStrategy.None,
            val checkSingle: Boolean = true
    ) {

        enum class Priority(val value: Int) {
            LOWEST(-20),
            LOW(-10),
            NORMAL(0),
            HIGH(10),
            HIGHEST(20);
        }
    }

    data class ItemInfo<A : BaseViewModelAction<*>>(
            val item: A,
            val tag: String,
            val priority: Int,
            val isSingle: Boolean
    ) {

        /**
         * Время создания сообщения для сортировки при одинаковом [priority]
         */
        val timestamp: Long = System.currentTimeMillis()
    }

    sealed class UniqueStrategy {
        /**
         * Сообщение не уникально (может быть более 1 сообщения с одинаковым тэгом в очереди)
         */
        object None: UniqueStrategy()

        /**
         * Добавляемое сообщение не добавляется, если в очереди уже есть сообщение с таким же тегом
         */
        object Ignore: UniqueStrategy()

        /**
         * Добавляемое сообщение заменяет все добавленные в очередь ранее с таким же тегом
         */
        object Replace: UniqueStrategy()

        /**
         * Решение о попадании сообщения в очередь принимается в [lambda]
         */
        class Custom(val lambda: (List<ItemInfo<BaseViewModelAction<*>>>) -> Boolean): UniqueStrategy()
    }

    private class ItemComparator: BaseOptionalComparator<ItemComparator.SortOption, ItemInfo<*>>(
            mapOf(
                    Pair(SortOption.PRIORITY, false),
                    Pair(SortOption.TIMESTAMP, true)
            )
    ) {

        override fun compare(lhs: ItemInfo<*>, rhs: ItemInfo<*>, option: SortOption, ascending: Boolean): Int {
            return when(option) {
                SortOption.PRIORITY -> {
                    compareInts(lhs.priority, rhs.priority, ascending)
                }
                SortOption.TIMESTAMP -> {
                    compareLongs(lhs.timestamp, rhs.timestamp, ascending)
                }
            }
        }

        enum class SortOption : ISortOption {
            PRIORITY, TIMESTAMP;

            override val optionName: String
                get() = name
        }
    }
}