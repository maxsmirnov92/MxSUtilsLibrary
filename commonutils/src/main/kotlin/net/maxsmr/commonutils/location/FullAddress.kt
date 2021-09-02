package net.maxsmr.commonutils.location

import android.location.Address
import net.maxsmr.commonutils.text.EMPTY_STRING

class FullAddress(val address: Address) {

    val fullAddress: String
        get() {
            val result = StringBuilder()
            for (i in 0 until address.maxAddressLineIndex) {
                result.append(address.getAddressLine(i))
            }
            return result.toString()
        }

    val streetAndBuilding: String =
        (if (address.maxAddressLineIndex >= 0) address.getAddressLine(0) else null) ?: EMPTY_STRING

    val city: String = address.locality
        ?: if (address.maxAddressLineIndex >= 1) address.getAddressLine(1) else null
            ?: EMPTY_STRING

    val state: String = address.adminArea ?: EMPTY_STRING

    val country: String = address.countryName ?: EMPTY_STRING

    val postalCode: String = address.postalCode ?: EMPTY_STRING

    val countryCode: String = address.countryCode ?: EMPTY_STRING

    val feature: String = address.featureName ?: EMPTY_STRING
}