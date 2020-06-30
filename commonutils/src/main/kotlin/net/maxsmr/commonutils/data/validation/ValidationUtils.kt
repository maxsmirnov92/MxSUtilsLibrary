package net.maxsmr.commonutils.data.validation

import net.maxsmr.commonutils.data.conversion.format.parseDateNoThrow
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import java.text.SimpleDateFormat

const val REG_EX_PHONE_NUMBER_RUS = "^((\\+7|7|8)+([0-9]){10})\$"
const val REG_EX_PHONE_NUMBER = "^\\d{10}$"
const val REG_EX_EMAIL = "^[^ ]+@.[^!#\$%&'*+/=?^_`{|}~ -]+\\..[^!#\$%&'*+/=?^_`{|}~0-9 -]{1,6}\$"
const val REG_EX_EMAIL_ALT = "^.+@.+\\..+$"
const val REG_EX_SNILS = "^\\d{11}$"

fun isPhoneNumberRusValid(phone: String?): Boolean =
        validate(phone, REG_EX_PHONE_NUMBER_RUS)

/**
 * Проверка валидности формата phone [ValidationUtilsKt.validate]
 *
 * @param phone [String]
 * @return true - если формат соответствует формату [ValidationUtilsKt.REG_EX_PHONE_NUMBER]
 */
fun isPhoneValid(phone: String?): Boolean =
        validate(phone, REG_EX_PHONE_NUMBER)


/**
 * Проверка валидности формата email {@link ValidationUtilsKt#validate}
 *
 * @return true - если формат соответствует формату [REG_EX_EMAIL]
 */
fun isEmailValid(email: CharSequence?): Boolean =
        validate(email, REG_EX_EMAIL)

/**
 * Проверка валидности формата email (выбор способа доставки чека, RZD-7521) [validate]
 *
 * @param email [String]
 * @return true - если формат соответствует формату [REG_EX_EMAIL_ALT]
 */
fun isEmailValidReceipt(email: String?): Boolean =
        validate(email, REG_EX_EMAIL_ALT)

fun isSnilsValid(snils: String?): Boolean =
        validate(snils, REG_EX_SNILS)

/**
 * Вызывать для проверки строки на соотвествие формату
 *
 * @param target  - проверяемая строка
 * @param pattern - формат
 * @return true, если соответствует формату
 */
fun validate(target: CharSequence?, pattern: String?): Boolean =
        target?.matches((pattern?: EMPTY_STRING).toRegex()) ?: false

fun isDateValid(
        dateText: String,
        pattern: String,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
) = parseDateNoThrow(dateText, pattern, dateFormatConfigurator) != null