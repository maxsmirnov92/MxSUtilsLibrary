package net.maxsmr.commonutils.rx.live.event

import androidx.annotation.MainThread
import net.maxsmr.commonutils.android.gui.actions.BaseViewModelAction
import net.maxsmr.commonutils.android.gui.fragments.dialogs.holder.DialogFragmentsHolder
import net.maxsmr.commonutils.data.Predicate.Methods.findWithIndex
import net.maxsmr.commonutils.data.collection.sort.BaseOptionalComparator
import net.maxsmr.commonutils.data.collection.sort.ISortOption
import net.maxsmr.commonutils.data.compareInts
import net.maxsmr.commonutils.data.compareLongs
import net.maxsmr.commonutils.data.text.EMPTY_STRING
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
     * @param remove нужно ли удалить вычитанный элемент из очереди (нужно, если например обработали показ диалога в [DialogFragmentsHolder]
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
            // первый из очереди должен попасть в результат всегда
            // далее при соот-ии предикату
            result.addAll(list.filterIndexed { index, item ->
                index == 0
                        || predicate == null || predicate.invoke(item)
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
        val existingItem = if (tag.isNotEmpty()) {
            findWithIndex(list) {
                it.tag == tag
                // && (it !is BaseMessageAction<*,*> || value !is BaseMessageAction<*,*> || it.show != value.show)
            }
        } else {
            null
        }
        when (options.unique) {
            UniqueStrategy.IGNORE -> {
                if (existingItem != null) {
                    return
                }
            }
            UniqueStrategy.REPLACE -> {
                existingItem?.let {
                    list.removeAt(it.first)
                }
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
            val unique: UniqueStrategy = UniqueStrategy.NONE,
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

    enum class UniqueStrategy {
        /**
         * Сообщение не уникально (может быть более 1 сообщения с одинаковым тэгом в очереди)
         */
        NONE,

        /**
         * Добавляемое сообщение не добавляется, если в очереди уже есть сообщение с таким же тегом
         */
        IGNORE,

        /**
         * Добавляемое сообщение заменяет все добавленные в очередь ранее с таким же тегом
         */
        REPLACE;
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