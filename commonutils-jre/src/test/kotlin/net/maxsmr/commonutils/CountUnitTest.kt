package net.maxsmr.commonutils

import net.maxsmr.commonutils.conversion.CountUnit
import net.maxsmr.commonutils.conversion.decomposeCount
import org.junit.Test
import kotlin.test.assertEquals

class CountUnitTest {

    @Test
    fun testDecomposeUnit() {
        var result: Map<CountUnit, Number> = decomposeCount(
            999.111,
            CountUnit.UNITS,
            countUnitsToExclude = setOf(CountUnit.UNITS),
            ignoreExclusionIfOnly = false,
            precision = null
        )
        assertEquals(0, result.size)

        result = decomposeCount(
            999.111,
            CountUnit.UNITS,
            countUnitsToExclude = setOf(CountUnit.UNITS),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(1, result.size)

        result = decomposeCount(
            400123,
            CountUnit.K_UNITS,
            countUnitsToExclude = setOf(CountUnit.M_UNITS),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(1, result.size)

        result = decomposeCount(
            400123,
            CountUnit.K_UNITS,
            countUnitsToExclude = setOf(),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(2, result.size)
    }
}