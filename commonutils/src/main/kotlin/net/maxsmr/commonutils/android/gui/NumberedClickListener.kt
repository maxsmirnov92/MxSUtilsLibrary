package net.maxsmr.commonutils.android.gui

import android.view.View

const val DEFAULT_TARGET_INTERVAL = 200L

/**
 * Кастомный листенер для отслеживания кол-ва
 * кликов по view; при достижении заданого кол-ва вызывается [targetAction]
 * @param targetCount целевое кол-во кликов
 * @param targetInterval целевой интервал: при превышении счётчик нажатий сбрасывается
 */
class NumberedClickListener(
        targetCount: Int,
        targetInterval: Long = DEFAULT_TARGET_INTERVAL,
        targetAction: (count: Int) -> Unit
) : View.OnClickListener {

    var targetCount: Int = 1
        set(value) {
            require(value > 0) { "Incorrect targetCount: $value" }
            field = value
        }

    var targetInterval: Long = DEFAULT_TARGET_INTERVAL
        set(value) {
            require(value >= 0) { "Incorrect targetInterval: $value" }
            field = value
        }

    var targetAction: (count: Int) -> Unit

    private var lastClickTime = 0L

    private var currentCount = 0

    init {
        this.targetCount = targetCount
        this.targetInterval = targetInterval
        this.targetAction = targetAction
    }

    override fun onClick(v: View) {
        val currentTime = System.currentTimeMillis()
        with (lastClickTime) {
            if (targetInterval > 0L && this != 0L && this <= currentTime) {
                val interval = currentTime - this
                if (interval > targetInterval) {
                    currentCount = 0
                }
            }
        }
        currentCount++
        if (currentCount >= targetCount) {
            targetAction.invoke(currentCount)
            currentCount = 0
        }
        lastClickTime = currentTime
    }
}