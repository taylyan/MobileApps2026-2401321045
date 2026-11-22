
package com.huntersstory.app.domain.validation

object HuntingTripValidator {

    fun validate(
        date: String,
        location: String,
        species: String
    ): String? {

        if (date.isBlank()) {
            return "Date is required."
        }

        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(date)) {
            return "Date must use YYYY-MM-DD."
        }

        if (!isValidCalendarDate(date)) {
            return "Invalid calendar date."
        }

        if (location.isBlank()) {
            return "Location is required."
        }

        if (species.isBlank()) {
            return "Game species is required."
        }

        return null
    }

    fun isLeapYear(year: Int): Boolean {
        return year % 400 == 0 ||
                (year % 4 == 0 && year % 100 != 0)
    }

    fun isValidCalendarDate(date: String): Boolean {
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(date)) {
            return false
        }

        val parts = date.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        if (year !in 1..9999) return false
        if (month !in 1..12) return false

        val daysInMonth = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        return day in 1..daysInMonth
    }
}
