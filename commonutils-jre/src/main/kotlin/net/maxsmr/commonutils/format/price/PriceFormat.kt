package net.maxsmr.commonutils.format.price

enum class PriceFormat {

    /*
     * всегда выводить с копейками
     */
    KOPEKS_ALWAYS,
    /**
     * всегда выводить без копеек, усечение суммы
     */
    KOPEKS_TRUNC,
    /**
     * всегда выводить без копеек, округление суммы по правилам округления;
     * использование не предусмотрено
     */
    KOPEKS_ROUND,
    /**
     * всегда выводить без копеек, округление всегда вверх
     */
    KOPEKS_ROUNDUP,
    /**
     * выводить копейки, только если они ненулевые
     */
    KOPEKS_IF_NONZERO
}