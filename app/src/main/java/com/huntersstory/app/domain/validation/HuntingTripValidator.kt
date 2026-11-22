
package com.huntersstory.app.domain.validation

object HuntingTripValidator {
    private const val LEAP_DIVISOR = 4
    private const val CENTURY_DIVISOR = 100
    private const val FOUR_CENTURIES_DIVISOR = 400

    private const val MIN_YEAR = 1
    private const val MAX_YEAR = 9999
    private const val MIN_MONTH = 1
    private const val MAX_MONTH = 12
    private const val FEBRUARY = 2

    private const val FEBRUARY_DAYS = 28
    private const val LEAP_FEBRUARY_DAYS = 29
    private const val SHORT_MONTH_DAYS = 30
    private const val LONG_MONTH_DAYS = 31

    private val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")

    fun validate(
        date: String,
        location: String,
        species: String,
    ): String? =
        when {
            date.isBlank() ->
                "Date is required."

            !datePattern.matches(date) ->
                "Date must use YYYY-MM-DD."

            !isValidCalendarDate(date) ->
                "Invalid calendar date."

            location.isBlank() ->
                "Location is required."

            species.isBlank() ->
                "Game species is required."

            else -> null
        }

    fun isLeapYear(year: Int): Boolean =
        year % FOUR_CENTURIES_DIVISOR == 0 ||
            (
                year % LEAP_DIVISOR == 0 &&
                    year % CENTURY_DIVISOR != 0
            )

    fun isValidCalendarDate(date: String): Boolean {
        if (!datePattern.matches(date)) {
            return false
        }

        val parts = date.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        val validYear = year in MIN_YEAR..MAX_YEAR
        val validMonth = month in MIN_MONTH..MAX_MONTH

        val daysInMonth =
            when {
                month == FEBRUARY && isLeapYear(year) ->
                    LEAP_FEBRUARY_DAYS

                month == FEBRUARY ->
                    FEBRUARY_DAYS

                month in listOf(4, 6, 9, 11) ->
                    SHORT_MONTH_DAYS

                else ->
                    LONG_MONTH_DAYS
            }

        return validYear &&
            validMonth &&
            day in MIN_YEAR..daysInMonth
    }
}
