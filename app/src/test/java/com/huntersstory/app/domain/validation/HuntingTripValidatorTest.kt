
package com.huntersstory.app.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HuntingTripValidatorTest {

    @Test
    fun validTripReturnsNull() {
        val result = HuntingTripValidator.validate(
            "2026-11-22",
            "Kuklen",
            "Pheasant"
        )
        assertNull(result)
    }

    @Test
    fun emptyDateReturnsError() {
        val result = HuntingTripValidator.validate(
            "",
            "Kuklen",
            "Pheasant"
        )
        assertEquals("Date is required.", result)
    }

    @Test
    fun blankDateReturnsError() {
        val result = HuntingTripValidator.validate(
            "   ",
            "Kuklen",
            "Pheasant"
        )
        assertEquals("Date is required.", result)
    }

    @Test
    fun incorrectDateFormatReturnsError() {
        val result = HuntingTripValidator.validate(
            "22-11-2026",
            "Kuklen",
            "Pheasant"
        )
        assertEquals("Date must use YYYY-MM-DD.", result)
    }

    @Test
    fun dateWithSlashesReturnsError() {
        val result = HuntingTripValidator.validate(
            "2026/11/22",
            "Kuklen",
            "Pheasant"
        )
        assertEquals("Date must use YYYY-MM-DD.", result)
    }

    @Test
    fun dateWithoutLeadingZerosReturnsError() {
        val result = HuntingTripValidator.validate(
            "2026-1-2",
            "Kuklen",
            "Pheasant"
        )
        assertEquals("Date must use YYYY-MM-DD.", result)
    }

    @Test
    fun emptyLocationReturnsError() {
        val result = HuntingTripValidator.validate(
            "2026-11-22",
            "",
            "Pheasant"
        )
        assertEquals("Location is required.", result)
    }

    @Test
    fun blankLocationReturnsError() {
        val result = HuntingTripValidator.validate(
            "2026-11-22",
            "   ",
            "Pheasant"
        )
        assertEquals("Location is required.", result)
    }

    @Test
    fun emptySpeciesReturnsError() {
        val result = HuntingTripValidator.validate(
            "2026-11-22",
            "Kuklen",
            ""
        )
        assertEquals("Game species is required.", result)
    }

    @Test
    fun validTripWithLongTextReturnsNull() {
        val result = HuntingTripValidator.validate(
            "2026-11-22",
            "Rhodope Mountains, Bulgaria",
            "Common Wood Pigeon"
        )
        assertNull(result)
    }
}
