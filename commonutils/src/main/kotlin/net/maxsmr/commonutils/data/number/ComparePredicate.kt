package net.maxsmr.commonutils.data.number

enum class ComparePredicate {

    EQUAL,
    LESS,
    MORE,
    LESS_OR_EQUAL,
    MORE_OR_EQUAL;

    fun compare(one: Number, another: Number): Boolean {
        val oneDouble = one.toDouble()
        val anotherDouble = another.toDouble()
        return when(this) {
            EQUAL -> oneDouble == anotherDouble
            LESS -> oneDouble < anotherDouble
            MORE -> oneDouble > anotherDouble
            LESS_OR_EQUAL -> oneDouble <= anotherDouble
            MORE_OR_EQUAL -> oneDouble >= anotherDouble
        }
    }
}