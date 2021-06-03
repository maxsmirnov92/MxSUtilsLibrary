package net.maxsmr.commonutils.logger.holder

import net.maxsmr.commonutils.text.isEmpty
import java.util.*

private const val TAG_FORMAT = "%1s/%2s"

abstract class BaseTagLoggerHolder(val tag: String) : BaseLoggerHolder() {

    init {
        require(!isEmpty(tag)) { "Log tag is empty" }
    }

    /** %1s - app prefix, %2s - log tag  */
    private val tags = Collections.synchronizedMap(HashMap<String, String>())
    
    protected fun getTag(clazz: Class<*>): String =
            getTag(clazz.simpleName)

    protected fun getTag(className: String): String {
        var tag = tags[className]
        if (tag == null) {
            tag = String.format(TAG_FORMAT, tag, className)
            tags[className] = tag
        }
        return tag
    }

}
