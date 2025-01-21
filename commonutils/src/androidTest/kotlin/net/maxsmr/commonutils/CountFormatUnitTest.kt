package net.maxsmr.commonutils

import androidx.test.platform.app.InstrumentationRegistry
import net.maxsmr.commonutils.conversion.CountUnit
import net.maxsmr.commonutils.format.decomposeCountFormatted
import net.maxsmr.commonutils.format.formatCountSingle
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(JUnit4::class)
class CountFormatUnitTest {

    @Test
    fun testCountFormat() {
        val appContext = InstrumentationRegistry.getInstrumentation().context
        val message = formatCountSingle(
            3890,
            CountUnit.UNITS,
            precision = 4,
        )
        assertNotNull(message)
        val messages = decomposeCountFormatted(38910.555552, CountUnit.UNITS, precision = 2)
        assertEquals(2, messages.size)
    }
}