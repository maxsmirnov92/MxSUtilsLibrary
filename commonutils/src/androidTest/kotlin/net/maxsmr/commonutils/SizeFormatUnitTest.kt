package net.maxsmr.commonutils

import androidx.test.platform.app.InstrumentationRegistry
import net.maxsmr.commonutils.conversion.SizeUnit
import net.maxsmr.commonutils.format.formatSpeedSize
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(JUnit4::class)
class SizeFormatUnitTest {

    @Test
    fun testSpeedSizeFormat() {
        val appContext = InstrumentationRegistry.getInstrumentation().context

        val speed1 = 27.57564564321
        val speed1Formatted = formatSpeedSize(
            speed1,
            SizeUnit.BYTES,
            precision = 2,
            timeUnit = TimeUnit.SECONDS
        )
        assertNotNull(speed1Formatted)

        val speed2 = 0.25564564321
        val speed2Formatted = formatSpeedSize(
            speed2,
            SizeUnit.BYTES,
            precision = 0,
            timeUnit = TimeUnit.SECONDS
        )
        assertNull(speed2Formatted)

        val speed3 = 0.25564564321
        val speed3Formatted = formatSpeedSize(
            speed3,
            SizeUnit.BYTES,
            precision = 0,
            emptyIfZero = false,
            timeUnit = TimeUnit.SECONDS
        )
        assertNotNull(speed3Formatted)

        speed1Formatted.get(appContext)
        speed3Formatted.get(appContext)
    }
}