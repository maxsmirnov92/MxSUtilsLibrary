package net.maxsmr.commonutils.data.validation

import net.maxsmr.commonutils.data.conversion.format.normalizePhoneNumber

private val REG_EX_RUS_PHONE_NUMBER: Regex = "^((\\+7|7|8)+([0-9]){10})\$".toRegex()
private val REG_EX_EMAIL: Regex = "^[^ ]+@.[^!#\$%&'*+/=?^_`{|}~ -]+\\..[^!#\$%&'*+/=?^_`{|}~0-9 -]{1,6}\$".toRegex()

fun isValidRusNumber(phone: String): Boolean =
        normalizePhoneNumber(phone).matches(REG_EX_RUS_PHONE_NUMBER)

fun isEmailValid(email: CharSequence): Boolean {
    return email.matches(REG_EX_EMAIL)
}