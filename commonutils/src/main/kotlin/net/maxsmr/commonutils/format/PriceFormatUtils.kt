package net.maxsmr.commonutils.format

import android.content.Context
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.format.price.KopecksFormat
import net.maxsmr.commonutils.format.price.PriceFormatter.Companion.CHAR_SPACE_INSEPARABLE
import net.maxsmr.commonutils.number.fraction
import net.maxsmr.commonutils.number.isNotZero
import net.maxsmr.commonutils.number.roundInt
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.text.DecimalFormat

/**
 * Формат цены с использованием "рублей" и "копеек"
 * @param kopecksFormat при значении [KopecksFormat.ROUNDUP]
 * округление происходит в большую сторону до целого, копейки отбрасываются
 */
@JvmOverloads
fun Double.formatPrice(
    context: Context,
    isLoyalty: Boolean = false,
    kopecksFormat: KopecksFormat = KopecksFormat.IF_NONZERO,
): String {
    val result = StringBuilder()
    val intPart = if (kopecksFormat == KopecksFormat.ROUNDUP) {
        roundInt()
    } else {
        toInt()
    }
    result.append(
        context.resources.getQuantityString(
            if (isLoyalty) {
                R.plurals.loyalty_price_quantity
            } else {
                R.plurals.rubles_quantity
            },
            intPart,
            intPart
        )
    )
    if (!isLoyalty && kopecksFormat != KopecksFormat.ROUNDUP) {
        val fraction = this.fraction()
        if (fraction.isNotZero()) {
            val fractionPartString = fraction.toString()
            val fractionPartInt = fractionPartString.substring(
                2,
                if (fractionPartString.length > 4) {
                    4
                } else {
                    fractionPartString.length
                }
            ).toIntOrNull()
            if (fractionPartInt != null &&
                (kopecksFormat == KopecksFormat.ALWAYS || fractionPartInt > 0)
            ) {
                result.append(", ")
                result.append(
                    context.resources.getQuantityString(
                        R.plurals.kopecks_quantity,
                        fractionPartInt,
                        fractionPartInt
                    )
                )
            }
        }
    }
    return result.toString()
}

/**
 * Формат строки с ценой из резкультата от [DecimalFormat],
 * с использованием "рублей" и "копеек"
 */
@JvmOverloads
fun String.reformatPrice(
    context: Context,
    isLoyalty: Boolean = false,
    groupingSeparator: Char = CHAR_SPACE_INSEPARABLE,
    kopecksSeparator: Char = '.',
): String {
    val parts = replace("$groupingSeparator", "").split(kopecksSeparator)
    val intPart = parts.getOrNull(0)?.toIntOrNull() ?: return EMPTY_STRING
    val result = StringBuilder()
    result.append(
        context.resources.getQuantityString(
            if (isLoyalty) {
                R.plurals.loyalty_price_quantity
            } else {
                R.plurals.rubles_quantity
            },
            intPart,
            intPart
        )
    )
    if (!isLoyalty) {
        val fractionPart = parts.getOrNull(1)?.toIntOrNull()
        if (fractionPart != null && fractionPart != 0) {
            result.append(", ")
            result.append(
                context.resources.getQuantityString(
                    R.plurals.kopecks_quantity,
                    fractionPart,
                    fractionPart
                )
            )
        }
    }
    return result.toString()
}