package net.maxsmr.commonutils.gui.listeners

import android.os.Handler
import android.os.Looper

const val DEFAULT_TARGET_INTERVAL = 200L

/**
 * Кастомный листенер для отслеживания кол-ва
 * кликов по view; при достижении заданого кол-ва вызывается [targetAction]
 * @param targetCount целевое кол-во кликов
 * @param targetInterval целевой интервал: при превышении счётчик нажатий сбрасывается
 */
class NumberedClickListener(
    private val targetCount: Int,
    private val targetInterval: Long = DEFAULT_TARGET_INTERVAL,
) {

    private val handler = Handler(Looper.getMainLooper())

    private val clickResetRunnable = Runnable {
        currentCount = 0
    }

    var currentCount = 0
        private set

    init {
        require(targetCount > 0) { "Incorrect targetCount: $targetCount" }
        require(targetInterval >= 0) { "Incorrect targetInterval: $targetInterval" }
    }

    fun onClick(): Boolean {
        handler.removeCallbacks(clickResetRunnable)
        currentCount++
        val result = if (currentCount >= targetCount) {
            currentCount = 0
            true
        } else {
            false
        }
        targetInterval.takeIf { it > 0 }?.let {
            handler.postDelayed(clickResetRunnable, it)
        }
        return result
    }
}