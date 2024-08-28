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
        assertEquals(result.size, 0)

        result = decomposeSize(
            999.643453,
            SizeUnit.BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(result.size, 1)

        result = decomposeSize(
            345999,
            SizeUnit.KBYTES,
            sizeUnitsToExclude = setOf(SizeUnit.KBYTES),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(result.size, 2)

        result = decomposeSize(
            345999,
            SizeUnit.KBYTES,
            sizeUnitsToExclude = setOf(SizeUnit.BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2
        )
        assertEquals(result.size, 2)


        result = decomposeSize(
            345999.22222,
            SizeUnit.BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.KBYTES),
            ignoreExclusionIfOnly = true,
            precision = 2,
            singleResult = false
        )
        assertEquals(result.size, 1)

        result = decomposeSize(
            345999.22222,
            SizeUnit.BYTES,
            sizeUnitsToExclude = setOf(SizeUnit.KBYTES, SizeUnit.BYTES),
            ignoreExclusionIfOnly = true,
            precision = 2,
            singleResult = true
        )
        assertEquals(result.size, 1)

        result = decomposeSize(
            0,
            SizeUnit.GBYTES,
            sizeUnitsToExclude = setOf(SizeUnit.BYTES),
            emptyMapIfZero = false
        )
        assertEquals(result.size, 1)
    }
}