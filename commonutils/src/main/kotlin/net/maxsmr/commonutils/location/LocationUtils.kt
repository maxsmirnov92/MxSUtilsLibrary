package net.maxsmr.commonutils.location

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.PointF
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import java.util.*
import kotlin.math.*

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("LocationUtils")

private const val GAIA_CIRC_X = 40075.017f
private const val GAIA_CIRC_Y = 40007.860f

@JvmOverloads
fun getAddressFromLocation(
    location: Location,
    context: Context,
    locale: Locale = Locale.getDefault()
): FullAddress? {
    val geocoder = Geocoder(context, locale)
    try {
        geocoder.getFromLocation(location.latitude, location.longitude, 1)?.let { addresses ->
            addresses.getOrNull(0)?.let {
                return FullAddress(it)
            }
        }

    } catch (e: Exception) {
        logException(logger, e, "getFromLocation")
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun getAddressFromLocationAsync(
    location: Location,
    context: Context,
    locale: Locale = Locale.getDefault(),
    callback: (FullAddress) -> Unit
): FullAddress? {
    val geocoder = Geocoder(context, locale)
    try {
        geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                addresses.getOrNull(0)?.let {
                    callback(FullAddress(it))
                }
            }
        })
    } catch (e: Exception) {
        logException(logger, e, "getFromLocation")
    }
    return null
}

/**
 * Very poor math function for converting Earth's degrees to kilometers.
 */
fun angularDistanceToKilometers(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    val _x1 = x1 / 360.0 * GAIA_CIRC_X
    val _x2 = x2 / 360.0 * GAIA_CIRC_X
    val _y1 = y1 / 360.0 * GAIA_CIRC_Y
    val _y2 = y2 / 360.0 * GAIA_CIRC_Y
    val dX = (_x1 - _x2).pow(2.0)
    val dY = (_y1 - _y2).pow(2.0)
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
fun distance(begin: PointF, end: PointF): Float {
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
    return abs(result).toFloat()
}

/**
 * Calculate distance between two points in latitude and longitude taking
 * into account height difference. If you are not interested in height
 * difference pass 0.0. Uses Haversine method as its base.
 *
 * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
 * el2 End altitude in meters
 * @return Distance in Meters
 */
fun distance2(
    begin: PointF,
    end: PointF,
    startAltitude: Float = 0f,
    endAltitude: Float = 0f,
): Float {
    val latDistance = Math.toRadians((end.x - begin.x).toDouble())
    val lonDistance = Math.toRadians((end.y - begin.y).toDouble())
    val a = sin(latDistance / 2) * sin(latDistance / 2) +
            cos(Math.toRadians(begin.x.toDouble())) * cos(Math.toRadians((end.x.toDouble()))) * sin(lonDistance / 2) *
            sin(lonDistance / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    // convert to meters, 6371 km is radius of the earth
    var distance = 6371 * c * 1000

    val height = (startAltitude - endAltitude).toDouble()
    distance = distance.pow(2.0) + height.pow(2.0)
    return sqrt(distance).toFloat()
}

/**
 * Вычисление скорости передвижения, используется [.distance]
 *
 * @param begin начальная точка [Location]
 * @param end конечная точка [Location]
 * @return сскорость движения в м/с
 */
fun speed(begin: Location, end: Location): Double {
    val interval = begin.time - end.time
    val distance = distance(
        PointF(begin.latitude.toFloat(), begin.longitude.toFloat()),
        PointF(end.latitude.toFloat(), end.longitude.toFloat())
    ).toDouble()
    return if (interval > 0) distance / interval.toDouble() else 0.0
}

/**
 * Проверка наличия функции GPS на устройстве
 */
fun isGpsAvailable(isGpsOnly: Boolean, context: Context): Boolean {
    val pm: PackageManager = context.packageManager
    return pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) || !isGpsOnly && pm.hasSystemFeature(
        PackageManager.FEATURE_LOCATION_NETWORK)
}

/**
 * Проверка того, что функция GPS включена на устройстве
 */
fun isGpsEnabled(isGpsOnly: Boolean, context: Context): Boolean {
    return checkLocationProviderEnabled(isGpsOnly, context) != null
}

/**
 * Проверка того, что функция GPS включена на устройстве
 * @return имя доступного провайдера
 */
private fun checkLocationProviderEnabled(gpsOnly: Boolean, context: Context): String? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        return LocationManager.GPS_PROVIDER
    } else if (!gpsOnly && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        return LocationManager.NETWORK_PROVIDER
    }
    return null
}