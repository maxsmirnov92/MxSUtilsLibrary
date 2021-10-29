package net.maxsmr.commonutils.format.price

import net.maxsmr.commonutils.format.price.PriceStyle.*
import net.maxsmr.commonutils.number.isZero
import net.maxsmr.commonutils.text.EMPTY_STRING
import  net.maxsmr.commonutils.text.isEmpty
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException

/**
 * Форматирует цену из Double в строку, используя заданный [format]
 * @param style комбинация настроек в соот-ии со стилем
 */
class PriceFormatter(style: PriceStyle = ECONOMY) {

    private var format: KopecksFormat = PRICE_FORMAT_DEFAULT

    private var currency: String? = null

    private var currencySeparator: Char? = CHAR_SPACE_INSEPARABLE

    private var groupingSize: Int = GROUPING_SIZE_DEFAULT

    private var groupingSeparator: Char = CHAR_SPACE_INSEPARABLE

    private var decimalSeparator: Char = '.'

    init {
        when (style) {
            EXTENDED -> {
                setFormat(KopecksFormat.ALWAYS)
                setGroupingSize(GROUPING_SIZE_DEFAULT)
                setCurrencySeparator(CHAR_SPACE_INSEPARABLE)
            }
            ECONOMY -> {
                setFormat(KopecksFormat.IF_NONZERO)
                setGroupingSize(GROUPING_SIZE_DEFAULT)
                setCurrencySeparator(CHAR_SPACE_INSEPARABLE)
            }
            ROUNDED -> {
                setFormat(KopecksFormat.ROUNDUP)
                setGroupingSize(GROUPING_SIZE_DEFAULT)
                setCurrencySeparator(CHAR_SPACE_INSEPARABLE)
            }
            ECONOMY_EXTRA -> {
                setFormat(KopecksFormat.ROUNDUP)
                resetGroupingSize()
                setCurrencySeparator(null)
            }
        }
    }

    fun setFormat(format: KopecksFormat) = apply {
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

    /**
     * Кол-во для getQuantityString в соот-ии с [price]
     * и текущим форматом
     */
    fun quantity(
        price: Double?,
        ifNonZero: Boolean = false,
        ifNonNullOrNan: Boolean = true,
    ): Int {
        if (ifNonNullOrNan && (price == null || price.isNaN())) {
            return 0
        }
        val price = price?.takeIf { !it.isNaN() } ?: .0
        if (ifNonZero && price.isZero()) {
            return 0
        }
        val format = createDecimalFormat(price)
        val formattedPrice = format(price, format)
        return try {
            format.parse(formattedPrice)?.toInt() ?: 0
        } catch (e: ParseException) {
            0
        }
    }

    /**
     * Форматирует цену из Double в String согласно заданным в этом инстансе форматтера параметрам
     *
     * @param price цена для форматирования
     * @param ifNonZero true, если в случае [price] == 0 вместо форматирования надо вернуть [EMPTY_STRING], иначе false
     * @param ifNonNullOrNan true, если в случае [price] == [Double.NaN] или [price] == null вместо форматирования надо вернуть [EMPTY_STRING], иначе false
     */
    @JvmOverloads
    fun format(
        price: Double?,
        ifNonZero: Boolean = false,
        ifNonNullOrNan: Boolean = true,
    ): String {
        if (ifNonNullOrNan && (price == null || price.isNaN())) {
            return EMPTY_STRING
        }
        val price = price?.takeIf { !it.isNaN() } ?: .0
        if (ifNonZero && price.isZero()) {
            return EMPTY_STRING
        }
        return format(price, createDecimalFormat(price))
    }

    private fun format(price: Double, format: DecimalFormat): String = buildString {
        append(format.format(price))
        if (!isEmpty(currency)) {
            currencySeparator?.let { append(it) }
            append(currency)
        }
    }

    private fun createDecimalFormat(price: Double): DecimalFormat {
        val symbols = DecimalFormatSymbols().also {
            it.groupingSeparator = groupingSeparator
            it.decimalSeparator = decimalSeparator
        }
        return DecimalFormat(
            format.decimalFormatPattern(price),
            symbols
        ).also {
            it.roundingMode = format.roundingMode()
            it.isGroupingUsed = groupingSize > 0
            if (groupingSize > 0) {
                it.groupingSize = groupingSize
            }
        }
    }

    companion object {


        const val GROUPING_SIZE_DEFAULT = 3

        /**
         * Неразрывный пробел
         */
        const val CHAR_SPACE_INSEPARABLE = '\u00A0'

        val PRICE_FORMAT_DEFAULT = KopecksFormat.IF_NONZERO
    }
}