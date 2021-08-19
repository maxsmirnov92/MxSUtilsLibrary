package net.maxsmr.commonutils

class ArgsParser(val args: List<String>) {

    @JvmOverloads
    fun containsArg(argName: String, ignoreCase: Boolean = true) =
            findArgIndexByName(argName, ignoreCase) != null

    /**
     * @return индекс аргумента с именем [argName] в [args]
     */
    @JvmOverloads
    fun findArgIndexByName(argName: String, ignoreCase: Boolean = true): Int? =
            Predicate.Methods.findIndexed(args) { arg ->
                arg.equals(argName, ignoreCase)
            }?.first

    @JvmOverloads
    fun containsAssociatedArg(argName: String, ignoreCase: Boolean = true) =
            findAssociatedArgByName(argName, ignoreCase) != null

    /**
     * @return название связанного аргумента,
     * в +1 позиции относительно [argName]
     */
    @JvmOverloads
    fun findAssociatedArgByName(argName: String, ignoreCase: Boolean = true): String? =
            findAssociatedArgInfoByName(argName, ignoreCase)?.second

    /**
     * @return индекс + название связанного аргумента,
     * в +1 позиции относительно [argName]
     */
    @JvmOverloads
    fun findAssociatedArgInfoByName(argName: String, ignoreCase: Boolean = true): Pair<Int, String>? {
        findArgIndexByName(argName, ignoreCase)?.let {
            // индекс аргумента argName был найден -> возвращаем связанный + 1
            return getAssociatedArgByIndex(it)
        }
        return null
    }

    private fun getAssociatedArgByIndex(argIndex: Int): Pair<Int, String>? =
            if (args.isNotEmpty() && argIndex < args.size - 1) {
                Pair(argIndex + 1, args[argIndex + 1])
            } else {
                null
            }
}