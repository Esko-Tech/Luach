/*
 * Luach, a Kotlin Multiplatform library for the Hebrew calendar and zmanim.
 * Copyright (C) 2026 Esko Tech Inc.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7: this program may
 * be conveyed through Apple's application distribution services as set out
 * in ADDITIONAL-PERMISSIONS.md at the root of the repository.
 */

package com.eskotech.luach.calendar

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

class HebrewDate private constructor(
    val epochDay: Long,
    val year: Int,
    val month: HebrewMonth,
    val day: Int,
) : Comparable<HebrewDate> {

    val localDate: LocalDate
        get() = LocalDate.fromEpochDays(epochDay)

    val dayOfWeek: DayOfWeek
        get() = localDate.dayOfWeek

    val isMeuberes: Boolean
        get() = isMeuberes(year)

    val yearLength: Int
        get() = yearLength(year)

    val yearType: YearType
        get() = yearType(year)

    val monthLength: Int
        get() = monthLength(year, month)

    val dayOfYear: Int
        get() = (epochDay - roshHashanaEpochDay(year) + 1).toInt()

    val monthsInYear: List<HebrewMonth>
        get() = HebrewMonth.inYearOrder(isMeuberes)

    val molad: Molad
        get() = molad(year, month)

    fun plusDays(days: Long): HebrewDate = ofEpochDay(epochDay + days)

    fun plusDays(days: Int): HebrewDate = plusDays(days.toLong())

    fun minusDays(days: Long): HebrewDate = ofEpochDay(epochDay - days)

    fun minusDays(days: Int): HebrewDate = minusDays(days.toLong())

    fun plusMonths(months: Int): HebrewDate {
        var targetYear = year
        var order = monthsInYear
        var index = order.indexOf(month)
        var remaining = months
        while (remaining > 0) {
            if (index == order.lastIndex) {
                targetYear++
                order = HebrewMonth.inYearOrder(isMeuberes(targetYear))
                index = 0
            } else {
                index++
            }
            remaining--
        }
        while (remaining < 0) {
            if (index == 0) {
                targetYear--
                order = HebrewMonth.inYearOrder(isMeuberes(targetYear))
                index = order.lastIndex
            } else {
                index--
            }
            remaining++
        }
        return clamped(targetYear, order[index], day)
    }

    fun minusMonths(months: Int): HebrewDate = plusMonths(-months)

    fun plusYears(years: Int): HebrewDate {
        val targetYear = year + years
        val targetMonth = if (month == HebrewMonth.ADAR_II && !isMeuberes(targetYear)) HebrewMonth.ADAR else month
        return clamped(targetYear, targetMonth, day)
    }

    fun minusYears(years: Int): HebrewDate = plusYears(-years)

    fun withDay(day: Int): HebrewDate = of(year, month, day)

    fun withMonth(month: HebrewMonth): HebrewDate = clamped(year, month, day)

    fun withYear(year: Int): HebrewDate = plusYears(year - this.year)

    fun daysUntil(other: HebrewDate): Long = other.epochDay - epochDay

    override fun compareTo(other: HebrewDate): Int = epochDay.compareTo(other.epochDay)

    override fun equals(other: Any?): Boolean = other is HebrewDate && other.epochDay == epochDay

    override fun hashCode(): Int = epochDay.hashCode()

    override fun toString(): String = "$day ${month.name} $year"

    companion object {
        internal const val WEEK_ZERO_EPOCH_DAY: Long = -2092592L

        private val LEAP_YEARS_OF_CYCLE = setOf(0, 3, 6, 8, 11, 14, 17)

        private val DAYS_ROSH_HASHANAH_AVOIDS = setOf(0L, 3L, 5L)

        private const val DEFICIENT_YEAR_REMAINDER = 3

        private const val COMPLETE_YEAR_REMAINDER = 5

        private const val LATE_MOLAD = 18 * Molad.PARTS_PER_HOUR

        private const val TUESDAY_MOLAD_LIMIT = 9 * Molad.PARTS_PER_HOUR + 204

        private const val MONDAY_MOLAD_LIMIT = 15 * Molad.PARTS_PER_HOUR + 589

        fun of(year: Int, month: HebrewMonth, day: Int): HebrewDate {
            require(year >= 1) { "the Hebrew year must be at least 1, not $year" }
            require(month != HebrewMonth.ADAR_II || isMeuberes(year)) { "$year is not a leap year and has no Adar II" }
            val length = monthLength(year, month)
            require(day in 1..length) { "$month $year has $length days, so day $day is invalid" }
            return HebrewDate(roshHashanaEpochDay(year) + daysBeforeMonth(year, month) + day - 1, year, month, day)
        }

        fun of(year: Int, month: Int, day: Int): HebrewDate = of(year, HebrewMonth.of(month), day)

        fun from(date: LocalDate): HebrewDate = ofEpochDay(date.toEpochDays())

        fun from(instant: Instant, timeZone: TimeZone): HebrewDate = from(instant.toLocalDateTime(timeZone).date)

        fun today(timeZone: TimeZone = TimeZone.currentSystemDefault()): HebrewDate = from(Clock.System.todayIn(timeZone))

        fun ofEpochDay(epochDay: Long): HebrewDate {
            var year = ((epochDay - roshHashanaEpochDay(1)) / 365.2468).toInt() + 1
            if (year < 1) {
                year = 1
            }
            while (year > 1 && epochDay < roshHashanaEpochDay(year)) {
                year--
            }
            while (epochDay >= roshHashanaEpochDay(year + 1)) {
                year++
            }
            require(epochDay >= roshHashanaEpochDay(1)) { "epoch day $epochDay is before 1 Tishrei of the year 1" }
            var remaining = (epochDay - roshHashanaEpochDay(year)).toInt()
            for (month in HebrewMonth.inYearOrder(isMeuberes(year))) {
                val length = monthLength(year, month)
                if (remaining < length) {
                    return HebrewDate(epochDay, year, month, remaining + 1)
                }
                remaining -= length
            }
            throw IllegalStateException("epoch day $epochDay does not fall inside the year $year")
        }

        fun isMeuberes(year: Int): Boolean = year.mod(19) in LEAP_YEARS_OF_CYCLE

        fun yearLength(year: Int): Int = (roshHashanaEpochDay(year + 1) - roshHashanaEpochDay(year)).toInt()

        fun yearType(year: Int): YearType = when (yearLength(year) % 10) {
            DEFICIENT_YEAR_REMAINDER -> YearType.DEFICIENT
            COMPLETE_YEAR_REMAINDER -> YearType.COMPLETE
            else -> YearType.REGULAR
        }

        fun monthLength(year: Int, month: HebrewMonth): Int = when (month) {
            HebrewMonth.CHESHVAN -> if (yearType(year) == YearType.COMPLETE) 30 else 29
            HebrewMonth.KISLEV -> if (yearType(year) == YearType.DEFICIENT) 29 else 30
            HebrewMonth.ADAR -> if (isMeuberes(year)) 30 else 29
            HebrewMonth.IYAR, HebrewMonth.TAMMUZ, HebrewMonth.ELUL, HebrewMonth.TEVET, HebrewMonth.ADAR_II -> 29
            else -> 30
        }

        fun roshHashanah(year: Int): HebrewDate = of(year, HebrewMonth.TISHREI, 1)

        fun molad(year: Int, month: HebrewMonth): Molad {
            val monthsIntoYear = HebrewMonth.inYearOrder(isMeuberes(year)).indexOf(month)
            require(monthsIntoYear >= 0) { "$year has no $month" }
            return Molad(Molad.FIRST_MOLAD_PARTS + Molad.LUNATION_PARTS * (monthsBeforeYear(year) + monthsIntoYear))
        }

        internal fun roshHashanaEpochDay(year: Int): Long = WEEK_ZERO_EPOCH_DAY + 1 + newYearDayIndex(year)

        private fun monthsBeforeYear(year: Int): Long = (235L * year - 234) / 19

        private fun newYearDayIndex(year: Int): Long {
            val molad = molad(year, HebrewMonth.TISHREI)
            var dayIndex = molad.partsFromWeekStart / Molad.PARTS_PER_DAY
            val weekday = dayIndex % 7
            val parts = molad.partsOfDay
            val moladTooLate = parts >= LATE_MOLAD
            val tuesdayOfCommonYear = weekday == 2L && parts >= TUESDAY_MOLAD_LIMIT && !isMeuberes(year)
            val mondayAfterLeapYear = weekday == 1L && parts >= MONDAY_MOLAD_LIMIT && isMeuberes(year - 1)
            if (moladTooLate || tuesdayOfCommonYear || mondayAfterLeapYear) {
                dayIndex++
            }
            if (dayIndex % 7 in DAYS_ROSH_HASHANAH_AVOIDS) {
                dayIndex++
            }
            return dayIndex
        }

        private fun daysBeforeMonth(year: Int, month: HebrewMonth): Int {
            var days = 0
            for (candidate in HebrewMonth.inYearOrder(isMeuberes(year))) {
                if (candidate == month) {
                    return days
                }
                days += monthLength(year, candidate)
            }
            throw IllegalArgumentException("$year has no $month")
        }

        private fun clamped(year: Int, month: HebrewMonth, day: Int): HebrewDate =
            of(year, month, minOf(day, monthLength(year, month)))
    }
}
