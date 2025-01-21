package net.maxsmr.commonutils

import net.maxsmr.commonutils.conversion.SizeUnit
import net.maxsmr.commonutils.conversion.decomposeSize
import org.junit.Test
import kotlin.test.assertEquals

class SizeUnitTest {

    @Test
    fun testDecomposeUnit() {
        var result: Map<SizeUnit, Number> = decomposeSize(
            999.235,
            SizeUnit.BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.BYTES),
            ignoreExclusionIfOnly = false,
            precision = null
        )
        assertEquals(0, result.size)

        result = decomposeSize(
            999.643453,
            SizeUnit.BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(1, result.size)

        result = decomposeSize(
            345999,
            SizeUnit.K_BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.K_BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(2, result.size)

        result = decomposeSize(
            345999,
            SizeUnit.K_BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(2, result.size)


        result = decomposeSize(
            345999.22222,
            SizeUnit.BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.K_BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2,
            singleResult = false
        )
        assertEquals(1, result.size)

        result = decomposeSize(
            345999.22222,
            SizeUnit.BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.K_BYTES, SizeUnit.BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2,
            singleResult = true
        )
        assertEquals(1, result.size)

        result = decomposeSize(
            0,
            SizeUnit.G_BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.BYTES),
            emptyMapIfZero = false
        )
        assertEquals(1, result.size)
    }
}