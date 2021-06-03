package net.maxsmr.commonutils.prefs

import android.annotation.SuppressLint
import android.content.SharedPreferences
import net.maxsmr.commonutils.prefs.SharedPrefsHolder.PrefType.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>(SharedPrefsHolder::class.java)

const val EMPTY_BOOLEAN_SETTING = false
const val EMPTY_INT_SETTING = -1
const val EMPTY_LONG_SETTING = -1L
const val EMPTY_FLOAT_SETTING = -1f
val EMPTY_SET_SETTING = HashSet<String>()
val EMPTY_STRING_SETTING = EMPTY_STRING

/**
 * Holder for storing mapping with editors and prefs;
 * contains wrapped modifying methods
 */
@Suppress("UNCHECKED_CAST")
object SharedPrefsHolder {

    private val sharedPreferencesEditorMap = mutableMapOf<SharedPreferences, SharedPreferences.Editor>()

    fun hasKey(sp: SharedPreferences, key: String) = sp.contains(key)

    @Synchronized
    fun <V> getValue(
            sp: SharedPreferences,
            key: String,
            type: PrefType, // to avoid conflicts between Kotlin and Java
            defaultValue: V? = null
    ): V? = try {
        when (type) {
            STRING -> sp.getString(key, if (defaultValue != null) defaultValue as String? else EMPTY_STRING_SETTING) as V
            INT -> sp.getInt(key, if (defaultValue != null) defaultValue as Int else EMPTY_INT_SETTING) as V
            LONG -> sp.getLong(key, if (defaultValue != null) defaultValue as Long else EMPTY_LONG_SETTING) as V
            FLOAT -> sp.getFloat(key, if (defaultValue != null) defaultValue as Float else EMPTY_FLOAT_SETTING) as V
            DOUBLE -> Double.fromBits(sp.getLong(key, if (defaultValue != null) (defaultValue as Double).toBits() else EMPTY_LONG_SETTING)) as V
            BOOLEAN -> sp.getBoolean(key, if (defaultValue != null) defaultValue as Boolean else EMPTY_BOOLEAN_SETTING) as V
            STRING_SET -> sp.getStringSet(key, if (defaultValue != null) defaultValue as Set<String>? else EMPTY_SET_SETTING) as V
        }
    } catch (e: ClassCastException) {
        logException(logger, e, "getValue")
        null
    }

    /**
     * @return true if successfully committed
     */
    @SuppressLint("CommitPrefEdits")
    @Synchronized
    fun <V : Any?> setValue(
            sp: SharedPreferences,
            key: String,
            value: V,
            async: Boolean = true,
            valueModifiedListener: ((V?) -> Unit)? = null
    ): Boolean {

        // if not specifying value - string by default
        val prefType = PrefType.fromValue(value as Any?) ?: STRING
        val oldValue: V? = getValue(sp, key, prefType, null)

        val editor = getOrCreateEditor(sp)
        try {
            if (value != null) {
                when (value) {
                    is String -> editor.putString(key, value as String)
                    is Int -> editor.putInt(key, (value as Int))
                    is Long -> editor.putLong(key, (value as Long))
                    is Float -> editor.putFloat(key, (value as Float))
                    is Double -> editor.putLong(key, (value as Double).toBits())
                    is Boolean -> editor.putBoolean(key, (value as Boolean))
                    is Set<*> -> editor.putStringSet(key, value as Set<String>)
                    else -> throw UnsupportedOperationException("incorrect value type: $value")
                }
            } else {
                editor.putString(key, null)
            }
        } catch (e: RuntimeException) {
            logException(logger, e, "setValue")
        }

        if (saveChanges(editor, async)) {
            if (value != oldValue) {
                valueModifiedListener?.invoke(oldValue)
            }
            return true
        }
        return false
    }

    @Synchronized
    fun removeKey(sp: SharedPreferences,
                  key: String,
                  async: Boolean = true): Boolean {
        val editor = getOrCreateEditor(sp)
        editor.remove(key)
        return saveChanges(editor, async)
    }

    @Synchronized
    fun clear(sp: SharedPreferences, async: Boolean = true): Boolean {
        val editor = getOrCreateEditor(sp)
        editor.clear()
        return saveChanges(editor, async)
    }

    private fun getOrCreateEditor(sp: SharedPreferences): SharedPreferences.Editor =
            sharedPreferencesEditorMap.getOrPut(sp) {
                sp.edit()
            }

    private fun saveChanges(editor: SharedPreferences.Editor, async: Boolean = true): Boolean {
        if (async) {
            editor.apply()
        } else {
            return editor.commit()
        }
        return true
    }

    enum class PrefType {

        STRING, INT, FLOAT, DOUBLE, LONG, BOOLEAN, STRING_SET;

        companion object {

            fun fromValue(v: Any?): PrefType? =
                    when (v) {
                        is String -> STRING
                        is Int -> INT
                        is Float -> FLOAT
                        is Double -> DOUBLE
                        is Long -> LONG
                        is Boolean -> BOOLEAN
                        is Set<*> -> STRING_SET
                        else -> null
                    }
        }
    }
}