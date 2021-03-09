package net.maxsmr.commonutils.format

import net.maxsmr.commonutils.number.isZero
import net.maxsmr.commonutils.number.mergeFraction
import net.maxsmr.commonutils.number.mergeFractionSimple
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Функция для форматирования сумм в рублях/баллах
 * ВАЖНО! При добавлении символа рубля проверяйте, что используете подходящий шрифт!
 *
 * @param price         сумма
 * @param showZeroPrice выводить нулевую сумму, или возвращать пустую строку
 * @param priceFormat   указывать копейки (варианты: всегда, никогда, только ненулевые)
 * @param currencySymbol     символ валюты (добавляется в конец суммы через пробел)
 * @return форматированная сумма в виде строки
 */
fun Double.formatPrice(
        showZeroPrice: Boolean,
        priceFormat: PriceFormat,
        currencySymbol: String,
        decimalSeparator: Char = '.',
): String {
    var price = this
    if (price.isNaN() || price.isZero()) {
        price = if (showZeroPrice) 0.0 else return EMPTY_STRING
    }
    val symbols = DecimalFormatSymbols()
    symbols.groupingSeparator = '\u00A0' //неразрывный пробел
    symbols.decimalSeparator = decimalSeparator
    var costStr: String
    var decimalFormat: DecimalFormat
    if (priceFormat === PriceFormat.TRUNC) {
        decimalFormat = DecimalFormat("#0", symbols)
    } else if (priceFormat === PriceFormat.ROUND) {
        decimalFormat = DecimalFormat("#0", symbols)
        decimalFormat.roundingMode = RoundingMode.HALF_UP
    } else if (priceFormat === PriceFormat.ROUNDUP) {
        decimalFormat = DecimalFormat("#0", symbols)
        decimalFormat.roundingMode = RoundingMode.UP
    } else if (priceFormat === PriceFormat.ALWAYS) {
        decimalFormat = DecimalFormat("#0.00", symbols)
    } else if (priceFormat === PriceFormat.IF_NON_ZERO) {
        decimalFormat = DecimalFormat("#0.0", symbols)
        decimalFormat.roundingMode = RoundingMode.HALF_UP
        costStr = decimalFormat.format(price)
        decimalFormat.applyPattern(if (costStr.toFloat() - costStr.toFloat().toInt() == 0f) "#0" else "#0.0")
    } else {
        decimalFormat = DecimalFormat("#0.00", symbols)
        costStr = decimalFormat.format(price)
        if (costStr.endsWith("00")) {
            // == TRUNC
            decimalFormat = DecimalFormat("#0", symbols)
        }
    }
    decimalFormat.groupingSize = 3
    decimalFormat.isGroupingUsed = true
    costStr = decimalFormat.format(price)
    if (currencySymbol.isNotEmpty()) {
        costStr = "$costStr $currencySymbol"
    }
    return costStr
}

fun BigDecimal?.sumToFractionalSimple(): Long = mergeFractionSimple(2)

fun BigDecimal?.sumToFractional(): Long = mergeFraction(2)

enum class PriceFormat {
    /**
     * всегда выводить с дробными
     */
    ALWAYS,

    /**
     * всегда выводить без дробных, усечение суммы
     */
    TRUNC,

    /**
     * всегда выводить без дробных, округление суммы по правилам округления
     */
    ROUND,

    /**
     * всегда выводить без дробных, округление всегда вверх
     */
    ROUNDUP,

    /**
     * выводить копейки, только если они ненулевые
     */
    IF_NONZERO,

    /**
     * выводить один знак копеек (дестки), если такой имеентся, если нет - без копеек. Без округления
     */
    IF_NON_ZERO
}
