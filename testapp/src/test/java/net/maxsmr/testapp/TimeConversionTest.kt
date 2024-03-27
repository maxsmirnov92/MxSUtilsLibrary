package net.maxsmr.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.maxsmr.commonutils.format.decomposeTimeFormatted
import net.maxsmr.commonutils.gui.actions.message.text.JoinTextMessage
import net.maxsmr.commonutils.text.isEmpty
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TimeConversionTest : LoggerTest() {

    @Test
    override fun test() {
        val targetTime = TimeUnit.DAYS.toMillis(5) +
                TimeUnit.HOURS.toMillis(25) +
                TimeUnit.MINUTES.toMillis(50) +
                TimeUnit.SECONDS.toMillis(40) +
                TimeUnit.MILLISECONDS.toMillis(900) +
                TimeUnit.MICROSECONDS.toMillis(1000000) +
                TimeUnit.NANOSECONDS.toMillis(1000000000)
        val decomposeResult: CharSequence =
            JoinTextMessage(
                R.string.divider_comma,
                decomposeTimeFormatted(targetTime, TimeUnit.MILLISECONDS, true, setOf(TimeUnit.SECONDS))
            ).get(context)
        logger.i("decompose result: $decomposeResult")
        Assert.assertFalse(isEmpty(decomposeResult))
    }
}