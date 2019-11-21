package net.maxsmr.commonutils.android.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager.getDefaultSharedPreferences
import net.maxsmr.commonutils.android.prefs.PreferencesHolder.PrefType.*
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>(PreferencesHolder::class.java)

/**
 * Holder for storing mapping with editors and prefs;
 * contains wrapped modifying methods
 */
@Suppress("UNCHECKED_CAST")
object PreferencesHolder {

    val sharedPreferencesEditorMap = mutableMapOf<SharedPreferences, SharedPreferences.Editor>()

    fun hasKey(sp: SharedPreferences, key: String) = sp.contains(key)

    fun <V> getValue(
            sp: SharedPreferences,
            key: String,
            type: PrefType, // to avoid conflicts between Kotlin and Java
            defaultValue: V? = null
    ): V? = try {
        when (type) {
            STRING -> sp.getString(key, defaultValue as String?) as V
            INT -> sp.getInt(key, if (defaultValue != null) defaultValue as Int else 0) as V
            LONG -> sp.getLong(key, if (defaultValue != null) defaultValue as Long else 0) as V
            FLOAT -> sp.getFloat(key, if (defaultValue != null) defaultValue as Float else 0f) as V
            DOUBLE -> Double.fromBits(sp.getLong(key, if (defaultValue != null) (defaultValue as Double).toBits() else 0)) as V
            BOOLEAN -> sp.getBoolean(key, if (defaultValue != null) defaultValue as Boolean else false) as V
            STRING_SET -> sp.getStringSet(key, if (defaultValue != null) defaultValue as Set<String>? else null) as V
        }
    } catch (e: ClassCastException) {
        logger.e("A RuntimeException occurred during get value")
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
            logger.e("A RuntimeException occurred during put value")
        }

        if (saveChanges(editor, async)) {
            if (value != oldValue) {
                valueModifiedListener?.invoke(oldValue)
            }
            return true
        }
        return false
    }

    fun removeKey(sp: SharedPreferences,
                  key: String,
                  async: Boolean = true): Boolean {
        val editor = getOrCreateEditor(sp)
        editor.remove(key)
        return saveChanges(editor, async)
    }

    fun clear(sp: SharedPreferences, async: Boolean = true): Boolean {
        val editor = getOrCreateEditor(sp)
        editor.clear()
        return saveChanges(editor, async)
    }

    fun getSharedPreferences(
            context: Context,
            name: String,
            mode: Int = Context.MODE_PRIVATE
    ): SharedPreferences =
            if (name.isEmpty()) {
                getDefaultSharedPreferences(context)
            } else {
                context.getSharedPreferences(name, mode)
            }

    private fun getOrCreateEditor(sp: SharedPreferences): SharedPreferences.Editor =
            synchronized(sharedPreferencesEditorMap) {
                sharedPreferencesEditorMap.getOrPut(sp) {
                    sp.edit()
                }
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