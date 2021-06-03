package net.maxsmr.commonutils.analytics

import net.maxsmr.commonutils.CompareCondition
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

/**
 * Вспомогательный класс для трека событий аналитики
 */
class AnalyticsHelper(
        private val analyticsService: AnalyticsService,
        var eventKeysToExclude: Set<String> = setOf(),
        var eventParamsToExclude: Map<String, List<String>> = mapOf()
) {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>(AnalyticsHelper::class.java)
    
    /**
     * Маппинг, содержащий кол-во отправленных ивентов для экземпляра
     */
    private val analyticsStateMap: MutableMap<AnalyticsEvent, Int> = mutableMapOf()
    

    fun performActionForSingleInstance(event: AnalyticsEvent) {
        performActionWithCheck(event, SingleSentEventInstanceRule())
    }

    fun performActionForSingleType(event: AnalyticsEvent) {
        performActionWithCheck(event, SingleSentEventTypeRule())
    }

    fun performActionWithCheck(event: AnalyticsEvent, rule: EventRule) {
        if (rule.shouldPerformAction(event, analyticsStateMap)) {
            performAction(event)
        }
    }

    fun performAction(event: AnalyticsEvent) {
        if (!eventKeysToExclude.contains(event.key())) {
            filterParamsForEvent(event)
            var count = analyticsStateMap[event] ?: 0
            analyticsService.trackEvent(event)
            count++
            analyticsStateMap[event] = count
            logger?.d("Event $event was sent, count: $count; types count: " + analyticsStateMap.getCountForType(event.javaClass))
        }
    }

    fun clearActions() {
        analyticsStateMap.clear()
    }

    /**
     * @return true, если произошла корректировка [Bundle]
     * в указанном [event]
     */
    fun filterParamsForEvent(event: AnalyticsEvent): Boolean {
        var result = false
        eventParamsToExclude.entries.forEach {entry ->
            if (event.key() == entry.key) {
                val bundle = event.params()
                entry.value.toSet().forEach { param ->
                    if (bundle.containsKey(param)) {
                        bundle.remove(param)
                        result = true
                    }
                }
            }
        }
        return result
    }

    /**
     * Правило, по которому разрешается отправка ивента
     */
    interface EventRule {

        fun shouldPerformAction(event: AnalyticsEvent, analyticsStateMap: MutableMap<AnalyticsEvent, Int>): Boolean
    }

    open class CountableSentEventInstanceRule(val count: Int, private val condition: CompareCondition = CompareCondition.EQUAL) : EventRule {

        override fun shouldPerformAction(event: AnalyticsEvent, analyticsStateMap: MutableMap<AnalyticsEvent, Int>) =
                condition.apply((analyticsStateMap[event] ?: 0), count)
    }

    open class CountableSentEventTypeRule(val count: Int, private val condition: CompareCondition = CompareCondition.EQUAL) : EventRule {

        override fun shouldPerformAction(event: AnalyticsEvent, analyticsStateMap: MutableMap<AnalyticsEvent, Int>) =
                condition.apply(analyticsStateMap.getCountForType(event.javaClass), count)
    }

    /**
     * Отправка возможна, если нет уже отправленного инстанса типа от [AnalyticsEvent]
     */
    class SingleSentEventTypeRule : CountableSentEventTypeRule(0)

    /**
     * Отправка возможна, если нет уже отправленного инстанса, равного [AnalyticsEvent]
     */
    class SingleSentEventInstanceRule : CountableSentEventInstanceRule(0)
}

private fun Map<AnalyticsEvent, Int>.getCountForType(type: Class<AnalyticsEvent>): Int {
    var sum = 0
    filter { it.key.javaClass == type }.forEach { sum += it.value }
    return sum
}