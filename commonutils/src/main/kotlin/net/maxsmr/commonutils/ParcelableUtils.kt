package net.maxsmr.commonutils

import android.os.Parcel
import android.os.Parcelable

fun Parcelable.marshall(): ByteArray {
    val parcel = Parcel.obtain()
    this.writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.recycle()
    return bytes
}

fun <T> ByteArray.unmarshall(creator: Parcelable.Creator<T>): T? {
    var result: T? = null
    val parcel = this.unmarshall()
    if (parcel != null) {
        result = creator.createFromParcel(parcel)
        parcel.recycle()
    }
    return result
}

fun ByteArray.unmarshall(): Parcel? {
    var parcel: Parcel? = null
    if (this.isNotEmpty()) {
        parcel = Parcel.obtain()
        parcel.unmarshall(this, 0, this.size)
        parcel.setDataPosition(0) // This is extremely important!
    }
    return parcel
}