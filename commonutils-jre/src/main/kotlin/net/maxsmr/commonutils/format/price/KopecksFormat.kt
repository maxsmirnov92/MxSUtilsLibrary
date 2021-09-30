package net.maxsmr.commonutils.format.price

import java.math.RoundingMode
import java.text.DecimalFormat

enum class KopecksFormat {

    /**
     * всегда выводить с копейками
     */
    ALWAYS {
        override fun decimalFormatPattern(forPrice: Double): String = DECIMAL_FORMAT_KOPECKS
    },

    /**
     * всегда выводить без копеек, округление всегда вверх
     */
    ROUNDUP {
        override fun decimalFormatPattern(forPrice: Double): String = DECIMAL_FORMAT_NO_KOPECKS
        override fun roundingMode(): RoundingMode = RoundingMode.UP
    },

    /**
     * выводить копейки, только если они ненулевые
     */
    IF_NONZERO {
        override fun decimalFormatPattern(forPrice: Double): String {
            val noKopecks = DecimalFormat(DECIMAL_FORMAT_KOPECKS)
                .apply { roundingMode = roundingMode() }
                .format(forPrice)
                .endsWith("00")
            return if (noKopecks) DECIMAL_FORMAT_NO_KOPECKS else DECIMAL_FORMAT_KOPECKS
        }
    },
    /**
     * всегда выводить без копеек, усечение суммы
     */
    TRUNC {
        override fun decimalFormatPattern(forPrice: Double): String = DECIMAL_FORMAT_NO_KOPECKS
    },

    /**
     * всегда выводить без копеек, округление суммы по правилам округления
     */
    ROUND {
        override fun decimalFormatPattern(forPrice: Double): String = DECIMAL_FORMAT_NO_KOPECKS
        override fun roundingMode(): RoundingMode = RoundingMode.HALF_UP
    };

    abstract fun decimalFormatPattern(forPrice: Double): String
    open fun roundingMode(): RoundingMode = RoundingMode.HALF_EVEN


    companion object {

        const val DECIMAL_FORMAT_NO_KOPECKS = "#0"
        const val DECIMAL_FORMAT_KOPECKS = "#0.00"
    }
}