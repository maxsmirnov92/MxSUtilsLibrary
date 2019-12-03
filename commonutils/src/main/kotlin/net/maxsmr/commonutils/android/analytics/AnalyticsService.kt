package net.maxsmr.commonutils.android.analytics

/**
 * Ответсвенен за выполнение действия аналитики.
 */
interface AnalyticsService {

    fun trackEvent(event: AnalyticsEvent)
}