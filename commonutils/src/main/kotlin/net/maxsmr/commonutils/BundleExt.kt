package net.maxsmr.commonutils

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat.getParcelable
import androidx.core.os.BundleCompat.getParcelableArray
import androidx.core.os.BundleCompat.getParcelableArrayList
import java.io.Serializable

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return getParcelable(this, key, T::class.java)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Parcelable> Bundle.getParcelableArrayCompat(key: String): Array<T> {
    return (getParcelableArray(this, key, T::class.java) as? Array<T>) ?: arrayOf()
}

inline fun <reified T : Parcelable> Bundle.getParcelableListCompat(key: String): ArrayList<T> {
    return getParcelableArrayList(this, key, T::class.java) ?: arrayListOf()
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
    return if (isAtLeastTiramisu()) {
        getSerializable(key, T::class.java)
    } else {
        getSerializable(key) as? T
    }
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
    return if (isAtLeastTiramisu()) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Parcelable> Intent.getParcelableArrayExtraCompat(key: String): Array<T> {
    return if (isAtLeastTiramisu()) {
        getParcelableArrayExtra(key, T::class.java) ?: arrayOf()
    } else {
        (getParcelableArrayExtra(key) as? Array<T>) ?: arrayOf()
    }
}

inline fun <reified T : Parcelable> Intent.getParcelableListExtraCompat(key: String): ArrayList<T> {
    return if (isAtLeastTiramisu()) {
        getParcelableArrayListExtra(key, T::class.java) ?: arrayListOf()
    } else {
        getParcelableArrayListExtra<T>(key) ?: arrayListOf()
    }
}

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(key: String): T? {
    return if (isAtLeastTiramisu()) {
        getSerializableExtra(key, T::class.java)
    } else {
        getSerializableExtra(key) as? T
    }
}