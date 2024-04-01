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
        (if (address.maxAddressLineIndex >= 0) address.getAddressLine(0) else null).orEmpty()

    val city: String = address.locality
        ?: if (address.maxAddressLineIndex >= 1) address.getAddressLine(1) else null
           .orEmpty()

    val state: String = address.adminArea.orEmpty()

    val country: String = address.countryName.orEmpty()

    val postalCode: String = address.postalCode.orEmpty()

    val countryCode: String = address.countryCode.orEmpty()

    val feature: String = address.featureName.orEmpty()
}