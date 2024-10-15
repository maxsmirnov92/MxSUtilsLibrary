package net.maxsmr.commonutils.gui

import android.content.Context
import android.hardware.SensorManager
import android.view.OrientationEventListener
import kotlin.math.abs

open class DiffOrientationEventListener @JvmOverloads constructor(
    context: Context,
    delay: SensorDelay = SensorDelay.NORMAL,
    protected val notifyInterval: Long = NOTIFY_INTERVAL_NOT_SPECIFIED,
    protected val notifyDiffThreshold: Int = 0,
) : OrientationEventListener(context, delay.value) {

    var lastNotifyTime: Long = 0
        private set

    var lastRotation: Int = ROTATION_NOT_SPECIFIED
        private set(value) {
            if (value != field) {
                field = value
                onRotationChanged(value)
            }
        }

    var lastCorrectedRotation: Int = ROTATION_NOT_SPECIFIED
        private set(value) {
            if (value != field) {
                field = value
                onCorrectedRotationChanged(value)
            }
        }

    init {
        require((notifyInterval == NOTIFY_INTERVAL_NOT_SPECIFIED || notifyInterval > 0)) { "Incorrect notify interval: $notifyInterval" }
        require(notifyDiffThreshold >= 0) { "Incorrect notify diff threshold: $notifyInterval" }
    }

    override fun onOrientationChanged(orientation: Int) {
        if (orientation in 0..359) {
            if (orientation != lastRotation) {
                val currentTime = System.currentTimeMillis()
                var diff = 0

                val intervalPassed =
                    notifyInterval == NOTIFY_INTERVAL_NOT_SPECIFIED || lastNotifyTime == 0L || currentTime - lastNotifyTime >= notifyInterval

                if (intervalPassed) {
                    if (lastRotation != ROTATION_NOT_SPECIFIED) {
                        if (orientation < 90 && lastRotation >= 270 && lastRotation < 360) {
                            val currentRotationFixed = orientation + 360
                            diff = abs((currentRotationFixed - lastRotation).toDouble()).toInt()
                        } else if (orientation >= 270 && lastRotation >= 0 && lastRotation < 90) {
                            val lastRotationFixed = lastRotation + 360
                            diff = abs((orientation - lastRotationFixed).toDouble()).toInt()
                        } else {
                            diff = abs((orientation - lastRotation).toDouble()).toInt()
                        }
                    }

                    if (lastRotation == ROTATION_NOT_SPECIFIED || diff >= notifyDiffThreshold) {
                        lastNotifyTime = currentTime
                        lastRotation = orientation
                        lastCorrectedRotation = getCorrectedDisplayRotation(orientation)
                    }
                }
            }
        }
    }

    override fun disable() {
        resetValues()
        super.disable()
    }

    protected open fun onRotationChanged(rotation: Int) {}

    protected open fun onCorrectedRotationChanged(correctedRotation: Int) {}

    private fun resetValues() {
        lastNotifyTime = 0
        lastRotation = ROTATION_NOT_SPECIFIED
        lastCorrectedRotation = ROTATION_NOT_SPECIFIED
    }

    enum class SensorDelay(internal val value: Int) {

        FASTEST(SensorManager.SENSOR_DELAY_FASTEST),
        GAME(SensorManager.SENSOR_DELAY_GAME),
        UI(SensorManager.SENSOR_DELAY_UI),
        NORMAL(SensorManager.SENSOR_DELAY_NORMAL)
    }

    companion object {

        const val ROTATION_NOT_SPECIFIED: Int = -1

        const val NOTIFY_INTERVAL_NOT_SPECIFIED: Long = -1L
    }
}