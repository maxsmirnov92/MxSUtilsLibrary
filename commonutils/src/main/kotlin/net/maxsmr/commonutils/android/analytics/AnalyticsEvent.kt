package net.maxsmr.commonutils.android.analytics

import android.os.Bundle

/**
 * Событие аналитики с ключом параметрами
 */
interface AnalyticsEvent {

    fun key(): String

    fun params(): Bundle
}