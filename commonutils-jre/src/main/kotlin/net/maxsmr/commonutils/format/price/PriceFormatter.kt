package net.maxsmr.commonutils.format.price

import net.maxsmr.commonutils.format.price.PriceStyle.*
import net.maxsmr.commonutils.number.isZero
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Форматирует цену из Double в строку, используя заданный [format]
 * @param style комбинация настроек в соот-ии со стилем
 */
class PriceFormatter(style: PriceStyle = ECONOMY) {

    private var format: PriceFormat = PRICE_FORMAT_DEFAULT

    private var currency: String? = null

    private var currencySeparator: Char? = CHAR_SPACE_INSEPARABLE

    private var fractionalPartCount = FRACTIONAL_PART_DEFAULT

    private var groupingSize: Int = GROUPING_SIZE_DEFAULT

    private var groupingSeparator: Char = CHAR_SPACE_INSEPARABLE

    private var decimalSeparator: Char = '.'

    init {
        when (style) {
            EXTENDED -> {
                setFormat(PriceFormat.KOPEKS_ALWAYS)
                setFractionalPart(FRACTIONAL_PART_DEFAULT)
                setGroupingSize(GROUPING_SIZE_DEFAULT)
                setCurrencySeparator(CHAR_SPACE_INSEPARABLE)
            }
            ECONOMY -> {
                setFormat(PriceFormat.KOPEKS_IF_NONZERO)
                setFractionalPart(FRACTIONAL_PART_DEFAULT)
                setGroupingSize(GROUPING_SIZE_DEFAULT)
                setCurrencySeparator(CHAR_SPACE_INSEPARABLE)
            }
            ROUNDED -> {
                setFormat(PriceFormat.KOPEKS_ROUNDUP)
                setFractionalPart(0)
                setGroupingSize(GROUPING_SIZE_DEFAULT)
                setCurrencySeparator(CHAR_SPACE_INSEPARABLE)
            }
            ECONOMY_EXTRA -> {
                setFormat(PriceFormat.KOPEKS_ROUNDUP)
                setFractionalPart(0)
                resetGroupingSize()
                setCurrencySeparator(null)
            }
        }
    }

    fun setFormat(format: PriceFormat) = apply {
        this.format = format
    }

    /**
     * Добавляет символ(ы) валюты в конец через пробел
     */
    fun setCurrency(currency: String) = apply {
        this.currency = currency
    }

    fun setCurrencySeparator(currencySeparator: Char?) = apply {
        this.currencySeparator = currencySeparator
    }

    fun setFractionalPart(count: Int) = apply {
        fractionalPartCount = count
    }

    fun setGroupingSize(groupingSize: Int) = apply {
        this.groupingSize = groupingSize.takeIf { it > 0 } ?: 0
    }

    fun resetGroupingSize() = apply {
        this.groupingSize = 0
    }

    fun setGroupingSeparator(groupingSeparator: Char) = apply {
        this.groupingSeparator = groupingSeparator
    }

    fun setDecimalSeparator(decimalSeparator: Char) = apply {
        this.decimalSeparator = decimalSeparator
    }

    @JvmOverloads
    fun formatPrice(
        price: Double?,
        isNonZero: Boolean = false,
        isNonNullOrNan: Boolean = price == null || price.isNaN()
    ): String {
        if (isNonNullOrNan && (price == null || price.isNaN())) {
            return EMPTY_STRING
        }
        val price = price?.takeIf { !it.isNaN() } ?: .0
        if (isNonZero && price.isZero()) {
            return EMPTY_STRING
        }
        val symbols = DecimalFormatSymbols()
        symbols.groupingSeparator = groupingSeparator
        symbols.decimalSeparator = decimalSeparator
        var costStr: String
        var decimalFormat: DecimalFormat
        when (format) {
            PriceFormat.KOPEKS_TRUNC -> {
                decimalFormat = DecimalFormat(DECIMAL_FORMAT_NO_KOPECKS, symbols)
            }
            PriceFormat.KOPEKS_ROUND -> {
                decimalFormat = DecimalFormat(DECIMAL_FORMAT_NO_KOPECKS, symbols)
                decimalFormat.roundingMode = RoundingMode.HALF_UP
            }
            PriceFormat.KOPEKS_ROUNDUP -> {
                decimalFormat = DecimalFormat(DECIMAL_FORMAT_NO_KOPECKS, symbols)
                decimalFormat.roundingMode = RoundingMode.UP
            }
            PriceFormat.KOPEKS_ALWAYS -> {
                decimalFormat = DecimalFormat(fractionalPartCount.toDecimalFormat(), symbols)
            }
            else -> {
                decimalFormat = DecimalFormat(fractionalPartCount.toDecimalFormat(), symbols)
                costStr = decimalFormat.format(price)
                if (costStr.endsWith("00")) {
                    decimalFormat = DecimalFormat(DECIMAL_FORMAT_NO_KOPECKS, symbols)
                }
            }
        }
        groupingSize.let {
            if (it > 0) {
                decimalFormat.groupingSize = it
                decimalFormat.isGroupingUsed = true
            } else {
                decimalFormat.isGroupingUsed = false
            }
        }
        costStr = decimalFormat.format(price)
        currency?.takeIf { it.isNotEmpty() }?.let {
            costStr = currencySeparator?.let {
                costStr + it + currency
            } ?: "$costStr$currency"
        }
        return costStr
    }

    companion object {

        const val DECIMAL_FORMAT_NO_KOPECKS = "#0"
        const val DECIMAL_FORMAT_KOPECKS = "#0.%s"

        const val FRACTIONAL_PART_DEFAULT = 2
        const val GROUPING_SIZE_DEFAULT = 3

        /**
         * Неразрывный пробел
         */
        const val CHAR_SPACE_INSEPARABLE = '\u00A0'

        val PRICE_FORMAT_DEFAULT = PriceFormat.KOPEKS_IF_NONZERO

        private fun Int.toDecimalFormat(): String {
            if (this <= 0) return DECIMAL_FORMAT_NO_KOPECKS
            val part = StringBuilder()
            for (i in 0 until this) {
                part.append('0')
            }
            return DECIMAL_FORMAT_KOPECKS.format(part)
        }
    }
}