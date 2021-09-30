package net.maxsmr.commonutils.location

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.location.Geocoder
import android.location.Location
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import java.util.*
import kotlin.math.*

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("LocationUtils")

private const val GAIA_CIRC_X = 40075.017f
private const val GAIA_CIRC_Y = 40007.860f

@JvmOverloads
fun getFromLocation(
    location: Location,
    context: Context,
    locale: Locale = Locale.getDefault()
): FullAddress? {
    val geocoder = Geocoder(context, locale)
    try {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (addresses.isNotEmpty()) {
            addresses[0]?.let {
                return FullAddress(it)
            }
        }
    } catch (e: Exception) {
        logException(logger, e, "getFromLocation")
    }
    return null
}

/**
 * Very poor math function for converting Earth's degrees to kilometers.
 */
fun angularDistanceToKilometers(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    var x1 = x1
    var y1 = y1
    var x2 = x2
    var y2 = y2
    x1 = x1 / 360.0 * GAIA_CIRC_X
    x2 = x2 / 360.0 * GAIA_CIRC_X
    y1 = y1 / 360.0 * GAIA_CIRC_Y
    y2 = y2 / 360.0 * GAIA_CIRC_Y
    val dX = (x1 - x2).pow(2.0)
    val dY = (y1 - y2).pow(2.0)
    return sqrt(dX + dY)
}

/**
 * Расчет расстояния между двумя георграфическими точками в метрах
 * http://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
 *
 * @param begin - начальная точка [Point]
 * @param end - конечная точка [Point]
 * @return - расстояние, окргуленное до целого значения
 */
fun distance(begin: PointF, end: PointF): Int {
    val xLat = begin.x.toDouble()
    val xLong = begin.y.toDouble()
    val yLat = end.x.toDouble()
    val yLong = end.y.toDouble()
    var result = (sin(Math.toRadians(xLat)) * sin(Math.toRadians(yLat))
            + (cos(Math.toRadians(xLat)) * cos(Math.toRadians(yLat))
            * cos(Math.toRadians(xLong - yLong))))
    result = acos(result)
    result = Math.toDegrees(result)
    result *= 60 * 1.1515
    result *= 1.609344 * 1000 // перевод км в м
    return abs(result.toInt())
}

/**
 * Вычисление скпорости передвижения, используется [.distance]
 *
 * @param begin начальная точка [Location]
 * @param end конечная точка [Location]
 * @return сскорость движения в м/с
 */
fun speed(begin: Location, end: Location): Double {
    val interval = begin.time - end.time
    val distance = distance(
        PointF(begin.latitude.toFloat(), begin.longitude.toFloat()),
        PointF(end.latitude.toFloat(), end.longitude.toFloat())).toDouble()
    return if (interval > 0) distance / interval.toDouble() else 0.0
}