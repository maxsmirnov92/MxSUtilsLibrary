package net.maxsmr.commonutils

import org.junit.Assert.assertFalse
import org.junit.Test

class FileNameTest {

    @Test
    fun testFileName() {
        val regex = REG_EX_FILE_NAME.toRegex()
        val name = "abc:.txt"
        assertFalse(regex.matches(name))
    }
}