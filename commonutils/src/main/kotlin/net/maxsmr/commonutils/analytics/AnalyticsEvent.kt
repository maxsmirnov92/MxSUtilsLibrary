package net.maxsmr.commonutils.analytics

import android.os.Bundle

/**
 * Событие аналитики с ключом параметрами
 */
interface AnalyticsEvent {

    fun key(): String

    fun params(): Bundle
}