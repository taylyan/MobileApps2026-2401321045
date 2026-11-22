
package com.huntersstory.app.domain.validation

import org.junit.Assert.*
import org.junit.Test

class HuntingTripDateTest {

    @Test
    fun leapYear2024IsValid() {
        assertTrue(HuntingTripValidator.isLeapYear(2024))
    }

    @Test
    fun year2025IsNotLeapYear() {
        assertFalse(HuntingTripValidator.isLeapYear(2025))
    }

    @Test
    fun year2000IsLeapYear() {
        assertTrue(HuntingTripValidator.isLeapYear(2000))
    }

    @Test
    fun year1900IsNotLeapYear() {
        assertFalse(HuntingTripValidator.isLeapYear(1900))
    }

    @Test
    fun validFebruary29() {
        assertTrue(
            HuntingTripValidator.isValidCalendarDate("2024-02-29")
        )
    }

    @Test
    fun invalidFebruary29() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("2025-02-29")
        )
    }

    @Test
    fun february31IsInvalid() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("2026-02-31")
        )
    }

    @Test
    fun april31IsInvalid() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("2026-04-31")
        )
    }

    @Test
    fun december31IsValid() {
        assertTrue(
            HuntingTripValidator.isValidCalendarDate("2026-12-31")
        )
    }

    @Test
    fun invalidMonth13() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("2026-13-10")
        )
    }

    @Test
    fun invalidMonthZero() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("2026-00-10")
        )
    }

    @Test
    fun invalidDayZero() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("2026-11-00")
        )
    }

    @Test
    fun invalidYearZero() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("0000-11-22")
        )
    }

    @Test
    fun incorrectFormatIsRejected() {
        assertFalse(
            HuntingTripValidator.isValidCalendarDate("22-11-2026")
        )
    }

    @Test
    fun validTripWithRealDate() {
        assertNull(
            HuntingTripValidator.validate(
                "2026-11-22",
                "Kuklen",
                "Pheasant"
            )
        )
    }

    @Test
    fun tripWithImpossibleDateReturnsError() {
        assertEquals(
            "Invalid calendar date.",
            HuntingTripValidator.validate(
                "2026-02-31",
                "Kuklen",
                "Pheasant"
            )
        )
    }
}
