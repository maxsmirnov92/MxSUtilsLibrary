package net.maxsmr.commonutils.analytics

/**
 * Ответсвенен за выполнение действия аналитики.
 */
interface AnalyticsService {

    fun trackEvent(event: AnalyticsEvent)
}