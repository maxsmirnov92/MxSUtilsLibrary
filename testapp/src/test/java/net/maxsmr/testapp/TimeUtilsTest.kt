package net.maxsmr.testapp

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.maxsmr.commonutils.conversion.*
import net.maxsmr.commonutils.format.TIME_UNITS_TO_EXCLUDE_DEFAULT
import net.maxsmr.commonutils.format.TIME_UNITS_TO_INCLUDE_ROUND_DEFAULT
import net.maxsmr.commonutils.format.TimePluralFormat
import net.maxsmr.commonutils.format.decomposeTimeFormatted
import net.maxsmr.commonutils.gui.actions.message.text.JoinTextMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TimeUtilsTest : LoggerTest() {

    @Test
    fun timeTest() {
        var decomposedResult: Map<TimeUnit, Long> = mapOf()
        var roundedResult: Long = 0

        fun writeToLog() {
            logger.i(
                "Result message: ${
                    JoinTextMessage(
                        ", ",
                        decomposeTimeFormatted(decomposedResult, TimePluralFormat.NORMAL_FORMAT)
                    ).get(context)
                }"
            )
        }

        fun writeToLogRounded() {
            logger.i("Result round: $roundedResult")
        }

        decomposedResult =
            decomposeTime(
                TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(55) + TimeUnit.SECONDS.toMillis(10),
                TimeUnit.MILLISECONDS,
                timeUnitsToExclude = TIME_UNITS_TO_EXCLUDE_DEFAULT
            )
        writeToLog()
        assertEquals(decomposedResult.size, 2)

        decomposedResult = decomposeTime(
            TimeUnit.MINUTES.toMillis(61) + TimeUnit.SECONDS.toMillis(10),
            TimeUnit.MILLISECONDS,
            timeUnitsToExclude = TIME_UNITS_TO_EXCLUDE_DEFAULT
        )
        writeToLog()
        assertEquals(decomposedResult.size, 2)

        decomposedResult = decomposeTime(
            61, TimeUnit.SECONDS,
            timeUnitsToExclude = TIME_UNITS_TO_EXCLUDE_DEFAULT
        )
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        decomposedResult = decomposeTime(
            60, TimeUnit.SECONDS,
            timeUnitsToExclude = TIME_UNITS_TO_EXCLUDE_DEFAULT
        )
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        decomposedResult = decomposeTime(
            59, TimeUnit.SECONDS,
            timeUnitsToExclude = TIME_UNITS_TO_EXCLUDE_DEFAULT
        )
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        decomposedResult = decomposeTime(
            1, TimeUnit.HOURS,
            timeUnitsToExclude = setOf(TimeUnit.HOURS)
        )
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        decomposedResult = decomposeTime(
            1, TimeUnit.HOURS,
            timeUnitsToExclude = setOf(TimeUnit.HOURS, TimeUnit.MINUTES)
        )
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        decomposedResult = decomposeTime(61, TimeUnit.SECONDS)
        writeToLog()
        assertEquals(decomposedResult.size, 2)

        decomposedResult = decomposeTime(59, TimeUnit.SECONDS)
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        roundedResult =
            roundTimeIncluded(59, TimeUnit.SECONDS, timeUnitsToInclude = TIME_UNITS_TO_INCLUDE_ROUND_DEFAULT)
        writeToLogRounded()
        // ожидается округление до 1-ой минуты
        assertEquals(roundedResult, TimeUnit.MINUTES.toNanos(1))
        decomposedResult = decomposeTime(roundedResult, TimeUnit.NANOSECONDS)
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        roundedResult = roundTime(59, TimeUnit.SECONDS, timeUnitsToExclude = TIME_UNITS_TO_INCLUDE_ROUND_DEFAULT)
        writeToLogRounded()
        // без округления
        assertEquals(roundedResult, TimeUnit.SECONDS.toNanos(59))
        decomposedResult = decomposeTime(roundedResult, TimeUnit.NANOSECONDS)
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        roundedResult = roundTime(
            TimeUnit.DAYS.toMinutes(1) + TimeUnit.HOURS.toMinutes(23),
            TimeUnit.MINUTES
        )
        writeToLogRounded()
        // ожидается округление до 2-ух дней
        assertEquals(roundedResult, TimeUnit.DAYS.toNanos(2))
        decomposedResult = decomposeTime(roundedResult, TimeUnit.NANOSECONDS)
        writeToLog()
        assertEquals(decomposedResult.size, 1)

        roundedResult = roundTime(
            TimeUnit.DAYS.toMinutes(1) + TimeUnit.HOURS.toMinutes(23),
            TimeUnit.MINUTES,
            timeUnitsToExclude = setOf(TimeUnit.DAYS)
        )
        writeToLogRounded()
        // без округления
        assertEquals(
            roundedResult,
            TimeUnit.MINUTES.toNanos(TimeUnit.DAYS.toMinutes(1) + TimeUnit.HOURS.toMinutes(23))
        )
        decomposedResult = decomposeTime(roundedResult, TimeUnit.NANOSECONDS)
        writeToLog()
        assertEquals(decomposedResult.size, 2)


        roundedResult = roundTime(
            29,
            TimeUnit.SECONDS,
            rule = TimeRoundRule.ALL
        )
        writeToLogRounded()
        // округление в меньшую сторону: было 0 минут 29 секунд, стало 0 минут
        assertEquals(roundedResult, 0)
        decomposedResult = decomposeTime(roundedResult, TimeUnit.NANOSECONDS)
        writeToLog()
        assertEquals(decomposedResult.size, 0)

        roundedResult = roundTime(
            29,
            TimeUnit.SECONDS,
            rule = TimeRoundRule.MORE
        )
        writeToLogRounded()
        // без округления
        assertEquals(roundedResult, TimeUnit.SECONDS.toNanos(29))
        decomposedResult = decomposeTimeIncluded(roundedResult, TimeUnit.NANOSECONDS, timeUnitsToInclude = setOf(TimeUnit.SECONDS))
        writeToLog()
        assertEquals(decomposedResult.size, 1)
    }
}