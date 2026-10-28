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

import com.eskotech.luach.Mismatches
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class HebrewDateTest {

    private fun parts(date: HebrewDate): Triple<Int, HebrewMonth, Int> = Triple(date.year, date.month, date.day)

    @Test
    fun anchorTableIsSelfConsistent() {
        for (year in ReferenceCalendar.FIRST_YEAR..ReferenceCalendar.LAST_YEAR) {
            val roshHashanah = ReferenceCalendar.roshHashanah(year)
            assertTrue(
                roshHashanah.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                "Rosh Hashanah $year on $roshHashanah falls on ${roshHashanah.dayOfWeek}",
            )
            val length = ReferenceCalendar.yearLength(year)
            val allowed = if (ReferenceCalendar.isLeap(year)) setOf(383, 384, 385) else setOf(353, 354, 355)
            assertTrue(length in allowed, "year $year has $length days")
        }
    }

    @Test
    fun independentlyRememberedDates() {
        val known = listOf(
            Triple(5784, HebrewMonth.NISAN, 15) to LocalDate(2024, 4, 23),
            Triple(5785, HebrewMonth.KISLEV, 25) to LocalDate(2024, 12, 26),
            Triple(5785, HebrewMonth.ADAR, 14) to LocalDate(2025, 3, 14),
            Triple(5786, HebrewMonth.TISHREI, 10) to LocalDate(2025, 10, 2),
            Triple(5786, HebrewMonth.SIVAN, 6) to LocalDate(2026, 5, 22),
            Triple(5786, HebrewMonth.AV, 9) to LocalDate(2026, 7, 23),
            Triple(5787, HebrewMonth.SHEVAT, 15) to LocalDate(2027, 1, 23),
            Triple(5787, HebrewMonth.ADAR_II, 14) to LocalDate(2027, 3, 23),
            Triple(5787, HebrewMonth.NISAN, 15) to LocalDate(2027, 4, 22),
            Triple(5787, HebrewMonth.SIVAN, 6) to LocalDate(2027, 6, 11),
            Triple(5787, HebrewMonth.AV, 9) to LocalDate(2027, 8, 12),
            Triple(5787, HebrewMonth.CHESHVAN, 11) to LocalDate(2026, 10, 22),
            Triple(5782, HebrewMonth.AV, 9) to LocalDate(2022, 8, 6),
            Triple(5784, HebrewMonth.ADAR_II, 14) to LocalDate(2024, 3, 24),
            Triple(5780, HebrewMonth.NISAN, 15) to LocalDate(2020, 4, 9),
            Triple(5781, HebrewMonth.NISAN, 15) to LocalDate(2021, 3, 28),
            Triple(5783, HebrewMonth.TISHREI, 10) to LocalDate(2022, 10, 5),
            Triple(5779, HebrewMonth.NISAN, 15) to LocalDate(2019, 4, 20),
            Triple(5774, HebrewMonth.NISAN, 15) to LocalDate(2014, 4, 15),
            Triple(5771, HebrewMonth.NISAN, 15) to LocalDate(2011, 4, 19),
            Triple(5771, HebrewMonth.NISAN, 1) to LocalDate(2011, 4, 5),
            Triple(5771, HebrewMonth.TISHREI, 1) to LocalDate(2010, 9, 9),
            Triple(5775, HebrewMonth.NISAN, 15) to LocalDate(2015, 4, 4),
            Triple(5776, HebrewMonth.NISAN, 15) to LocalDate(2016, 4, 23),
            Triple(5778, HebrewMonth.KISLEV, 25) to LocalDate(2017, 12, 13),
            Triple(5777, HebrewMonth.TISHREI, 1) to LocalDate(2016, 10, 3),
            Triple(5769, HebrewMonth.NISAN, 14) to LocalDate(2009, 4, 8),
            Triple(5760, HebrewMonth.TISHREI, 1) to LocalDate(1999, 9, 11),
            Triple(5750, HebrewMonth.TISHREI, 1) to LocalDate(1989, 9, 30),
            Triple(5708, HebrewMonth.IYAR, 5) to LocalDate(1948, 5, 14),
            Triple(5727, HebrewMonth.IYAR, 28) to LocalDate(1967, 6, 7),
            Triple(5684, HebrewMonth.TISHREI, 1) to LocalDate(1923, 9, 11),
            Triple(3761, HebrewMonth.TEVET, 18) to LocalDate(1, 1, 1),
        )
        for ((expected, date) in known) {
            val (year, month, day) = expected
            assertEquals(date, HebrewDate.of(year, month, day).localDate, "$day $month $year")
            assertEquals(expected, parts(HebrewDate.from(date)), "$date")
        }
    }

    @Test
    fun everyDayOfEveryAnchoredYearConvertsBothWays() {
        val mismatches = Mismatches()
        for (expected in ReferenceCalendar.allDays()) {
            val fromHebrew = HebrewDate.of(expected.year, expected.month, expected.day)
            mismatches.check(fromHebrew.localDate == expected.date) { "$expected: localDate ${fromHebrew.localDate}" }
            mismatches.check(fromHebrew.dayOfWeek == expected.dayOfWeek) { "$expected: dayOfWeek ${fromHebrew.dayOfWeek}" }
            mismatches.check(fromHebrew.dayOfYear == expected.ordinalInYear) { "$expected: dayOfYear ${fromHebrew.dayOfYear}" }
            mismatches.check(fromHebrew.monthLength == ReferenceCalendar.monthLength(expected.year, expected.month)) { "$expected: monthLength ${fromHebrew.monthLength}" }
            mismatches.check(fromHebrew.yearLength == ReferenceCalendar.yearLength(expected.year)) { "$expected: yearLength ${fromHebrew.yearLength}" }
            mismatches.check(fromHebrew.isMeuberes == ReferenceCalendar.isLeap(expected.year)) { "$expected: isMeuberes" }
            val yearType = when (ReferenceCalendar.yearLength(expected.year) % 10) {
                3 -> YearType.DEFICIENT
                4 -> YearType.REGULAR
                else -> YearType.COMPLETE
            }
            mismatches.check(fromHebrew.yearType == yearType) { "$expected: yearType ${fromHebrew.yearType}" }
            mismatches.check(fromHebrew.monthsInYear == ReferenceCalendar.monthsInOrder(expected.year)) { "$expected: monthsInYear" }
            val fromGregorian = HebrewDate.from(expected.date)
            mismatches.check(parts(fromGregorian) == Triple(expected.year, expected.month, expected.day)) { "${expected.date}: ${parts(fromGregorian)}" }
            mismatches.check(fromGregorian == fromHebrew && fromGregorian.compareTo(fromHebrew) == 0 && fromGregorian.hashCode() == fromHebrew.hashCode()) {
                "$expected: equality between the two constructions"
            }
        }
        mismatches.assertNone("conversions")
    }

    @Test
    fun walkingForwardAndBackVisitsEveryDay() {
        val days = ReferenceCalendar.allDays().toList()
        var walker = HebrewDate.from(days.first().date)
        val mismatches = Mismatches()
        fun matches(expected: ReferenceCalendar.Day): Boolean =
            parts(walker) == Triple(expected.year, expected.month, expected.day) &&
                walker.localDate == expected.date && walker.dayOfWeek == expected.dayOfWeek &&
                walker.epochDay == expected.date.toEpochDays()
        for (expected in days) {
            mismatches.check(matches(expected)) { "forward reached $walker ${walker.localDate} expected $expected" }
            walker = walker.plusDays(1)
        }
        for (expected in days.asReversed()) {
            walker = walker.minusDays(1)
            mismatches.check(matches(expected)) { "back reached $walker ${walker.localDate} expected $expected" }
        }
        mismatches.assertNone("walking")
    }

    @Test
    fun monthAndYearArithmetic() {
        fun at(year: Int, month: HebrewMonth, day: Int) = HebrewDate.of(year, month, day)
        assertEquals(Triple(5784, HebrewMonth.ADAR_II, 29), parts(at(5784, HebrewMonth.ADAR, 30).plusMonths(1)))
        assertEquals(Triple(5784, HebrewMonth.NISAN, 10), parts(at(5784, HebrewMonth.ADAR_II, 10).plusMonths(1)))
        assertEquals(Triple(5783, HebrewMonth.NISAN, 10), parts(at(5783, HebrewMonth.ADAR, 10).plusMonths(1)))
        assertEquals(Triple(5784, HebrewMonth.TISHREI, 29), parts(at(5783, HebrewMonth.ELUL, 29).plusMonths(1)))
        assertEquals(Triple(5784, HebrewMonth.TISHREI, 1), parts(at(5783, HebrewMonth.TISHREI, 1).plusMonths(12)))
        assertEquals(Triple(5785, HebrewMonth.TISHREI, 1), parts(at(5784, HebrewMonth.TISHREI, 1).plusMonths(13)))
        assertEquals(Triple(5784, HebrewMonth.ADAR_II, 29), parts(at(5784, HebrewMonth.NISAN, 1).minusMonths(1).let { it.withDay(it.monthLength) }))
        assertEquals(Triple(5783, HebrewMonth.ELUL, 1), parts(at(5784, HebrewMonth.TISHREI, 1).minusMonths(1)))
        assertEquals(Triple(5783, HebrewMonth.ADAR, 10), parts(at(5783, HebrewMonth.NISAN, 10).minusMonths(1)))
        assertEquals(Triple(5785, HebrewMonth.ADAR, 29), parts(at(5784, HebrewMonth.ADAR, 30).plusYears(1)))
        assertEquals(Triple(5785, HebrewMonth.ADAR, 14), parts(at(5784, HebrewMonth.ADAR_II, 14).plusYears(1)))
        assertEquals(LocalDate(2024, 4, 23), at(5783, HebrewMonth.NISAN, 15).plusYears(1).localDate)
        assertEquals(LocalDate(2024, 4, 30), at(5784, HebrewMonth.NISAN, 15).plusDays(7).localDate)
        assertEquals(Triple(5783, HebrewMonth.ELUL, 29), parts(at(5784, HebrewMonth.TISHREI, 1).minusDays(1)))
        assertEquals(Triple(5784, HebrewMonth.ADAR_II, 29), parts(at(5784, HebrewMonth.NISAN, 1).minusDays(1)))
        assertEquals(Triple(5783, HebrewMonth.ADAR, 29), parts(at(5783, HebrewMonth.NISAN, 1).minusDays(1)))
        assertEquals(Triple(5786, HebrewMonth.CHESHVAN, 29), parts(at(5785, HebrewMonth.CHESHVAN, 30).withYear(5786)))
        assertEquals(Triple(5787, HebrewMonth.ADAR_II, 14), parts(at(5787, HebrewMonth.ADAR, 14).withMonth(HebrewMonth.ADAR_II)))
        assertEquals(7L, at(5784, HebrewMonth.NISAN, 15).daysUntil(at(5784, HebrewMonth.NISAN, 22)))
    }

    @Test
    fun yearTypes() {
        for (year in listOf(5773, 5777, 5781, 5784, 5790, 5793)) {
            assertEquals(YearType.DEFICIENT, HebrewDate.yearType(year), "$year")
        }
        for (year in listOf(5769, 5772, 5778, 5786, 5789, 5792, 5782)) {
            assertEquals(YearType.REGULAR, HebrewDate.yearType(year), "$year")
        }
        for (year in listOf(5770, 5780, 5783, 5785, 5788, 5791, 5794, 5771, 5774, 5776, 5779, 5787, 5795)) {
            assertEquals(YearType.COMPLETE, HebrewDate.yearType(year), "$year")
        }
        val leap = setOf(5160, 5771, 5774, 5776, 5779, 5782, 5784, 5787, 5790, 5793, 5795)
        for (year in (5770..5795) + listOf(5160, 5536)) {
            assertEquals(year in leap, HebrewDate.isMeuberes(year), "$year")
        }
    }

    @Test
    fun constructionAndValidation() {
        assertEquals(Triple(5787, HebrewMonth.CHESHVAN, 11), parts(HebrewDate.from(LocalDate(2026, 10, 22))))
        assertEquals("11 CHESHVAN 5787", HebrewDate.from(LocalDate(2026, 10, 22)).toString())
        assertEquals(HebrewDate.of(5784, HebrewMonth.NISAN, 15), HebrewDate.of(5784, 1, 15))

        val lateEvening = Instant.parse("2026-10-22T02:00:00Z")
        assertEquals(Triple(5787, HebrewMonth.CHESHVAN, 10), parts(HebrewDate.from(lateEvening, TimeZone.of("America/New_York"))))
        assertEquals(Triple(5787, HebrewMonth.CHESHVAN, 11), parts(HebrewDate.from(lateEvening, TimeZone.UTC)))
        assertEquals(Triple(5787, HebrewMonth.CHESHVAN, 11), parts(HebrewDate.from(lateEvening, TimeZone.of("Asia/Jerusalem"))))

        assertFailsWith<IllegalArgumentException> { HebrewDate.of(5786, HebrewMonth.ADAR_II, 1) }
        assertFailsWith<IllegalArgumentException> { HebrewDate.of(5786, 0, 1) }
        assertFailsWith<IllegalArgumentException> { HebrewDate.of(5786, 14, 1) }
        assertFailsWith<IllegalArgumentException> { HebrewDate.of(5786, HebrewMonth.ADAR, 30) }
        assertFailsWith<IllegalArgumentException> { HebrewDate.of(5786, HebrewMonth.CHESHVAN, 30) }
        assertFailsWith<IllegalArgumentException> { HebrewDate.of(5786, HebrewMonth.ADAR, 0) }
        assertFailsWith<IllegalArgumentException> { HebrewDate.of(0, HebrewMonth.TISHREI, 1) }
        assertFailsWith<IllegalArgumentException> { HebrewDate.molad(5786, HebrewMonth.ADAR_II) }
        assertEquals(30, HebrewDate.of(5785, HebrewMonth.CHESHVAN, 30).day)
        assertEquals(Triple(1, HebrewMonth.TISHREI, 1), parts(HebrewDate.roshHashanah(1)))
        assertEquals(DayOfWeek.MONDAY, HebrewDate.roshHashanah(1).dayOfWeek)
    }

    @Test
    fun epochDaysAndOrdering() {
        assertEquals(10957L, HebrewDate.from(LocalDate(2000, 1, 1)).epochDay)
        assertEquals(HebrewDate.from(LocalDate(2000, 1, 1)), HebrewDate.ofEpochDay(10957L))
        val a = HebrewDate.from(LocalDate(2024, 4, 22))
        val b = HebrewDate.from(LocalDate(2024, 4, 23))
        assertEquals(1L, b.epochDay - a.epochDay)
        assertTrue(a < b)
        assertTrue(b > a)
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
        assertEquals(a, HebrewDate.of(5784, HebrewMonth.NISAN, 14))
        assertEquals(HebrewDate.today(TimeZone.UTC).localDate.toEpochDays(), HebrewDate.today(TimeZone.UTC).epochDay)
    }

    private fun referenceParts(year: Int, month: HebrewMonth): Long {
        val ordinal = ReferenceCalendar.monthsInOrder(year).indexOf(month) + 1
        val cycles = (year - 1) / 19
        val yearInCycle = (year - 1) % 19
        val monthsElapsed = 235L * cycles + 12L * yearInCycle + (7L * yearInCycle + 1) / 19 + (ordinal - 1)
        return 31524L + 765433L * monthsElapsed
    }

    private fun roshHashanahDelay(year: Int): Int {
        val parts = referenceParts(year, HebrewMonth.TISHREI)
        val moladDay = (parts / 25920).toInt()
        val partsOfDay = (parts % 25920).toInt()
        var day = moladDay
        if (partsOfDay >= 19440 ||
            (day % 7 == 2 && partsOfDay >= 9924 && !ReferenceCalendar.isLeap(year)) ||
            (day % 7 == 1 && partsOfDay >= 16789 && ReferenceCalendar.isLeap(year - 1))
        ) {
            day++
        }
        if (day % 7 == 0 || day % 7 == 3 || day % 7 == 5) {
            day++
        }
        return day - moladDay
    }

    @Test
    fun moladMatchesFirstPrinciples() {
        val mismatches = Mismatches()
        val weekdays = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        for (year in ReferenceCalendar.FIRST_YEAR..ReferenceCalendar.LAST_YEAR) {
            val tishreiParts = referenceParts(year, HebrewMonth.TISHREI)
            val tishreiDay = tishreiParts / 25920
            val delay = roshHashanahDelay(year)
            val predictedWeekday = weekdays[((tishreiDay + delay) % 7).toInt()]
            mismatches.check(predictedWeekday == ReferenceCalendar.roshHashanah(year).dayOfWeek) { "$year: the postponement rules predict $predictedWeekday" }
            for (month in ReferenceCalendar.monthsInOrder(year)) {
                val parts = referenceParts(year, month)
                val day = parts / 25920
                val partsOfDay = (parts % 25920).toInt()
                val hours = partsOfDay / 1080
                val minutes = partsOfDay % 1080 / 18
                val remaining = partsOfDay % 18
                val daytimeOfMoladDay = ReferenceCalendar.roshHashanah(year).plus((day - tishreiDay - delay).toInt(), DateTimeUnit.DAY)
                val civilDate = if (hours < 6) daytimeOfMoladDay.minus(1, DateTimeUnit.DAY) else daytimeOfMoladDay
                val molad = HebrewDate.of(year, month, 1).molad
                mismatches.check(molad.dayOfWeek == weekdays[(day % 7).toInt()]) { "$year $month: molad weekday ${molad.dayOfWeek}" }
                mismatches.check(molad.hour == hours && molad.minute == minutes && molad.parts == remaining) {
                    "$year $month: molad time ${molad.hour}:${molad.minute}:${molad.parts} expected $hours:$minutes:$remaining"
                }
                mismatches.check(molad.clockHour == (hours + 18) % 24) { "$year $month: clock hour ${molad.clockHour}" }
                mismatches.check(molad.date.localDate == civilDate) { "$year $month: molad date ${molad.date.localDate} expected $civilDate" }
                mismatches.check(molad == HebrewDate.molad(year, month) && molad.partsOfDay == partsOfDay) { "$year $month: molad identity" }
            }
        }
        mismatches.assertNone("molad")
    }

    @Test
    fun moladInstantIsJerusalemMeanSolarTime() {
        val molad = HebrewDate.of(5778, HebrewMonth.CHESHVAN, 1).molad
        assertEquals(DayOfWeek.FRIDAY, molad.dayOfWeek)
        assertEquals(Triple(18, 12, 17), Triple(molad.hour, molad.minute, molad.parts))
        assertEquals(LocalDate(2017, 10, 20), molad.date.localDate)
        assertEquals(Instant.parse("2017-10-20T09:52:00.170Z"), molad.instant)
        assertEquals(Instant.parse("2017-10-23T09:52:00.170Z"), molad.earliestKiddushLevana3Days)
        assertEquals(Instant.parse("2017-10-27T09:52:00.170Z"), molad.earliestKiddushLevana7Days)
        assertEquals(Instant.parse("2017-11-04T09:52:00.170Z"), molad.latestKiddushLevana15Days)
        assertEquals(Instant.parse("2017-11-04T04:14:01.836Z"), molad.latestKiddushLevanaHalfMonth)
        assertTrue(molad < HebrewDate.of(5778, HebrewMonth.KISLEV, 1).molad)
    }

    @Test
    fun staticHelpers() {
        for (year in ReferenceCalendar.FIRST_YEAR..ReferenceCalendar.LAST_YEAR) {
            assertEquals(ReferenceCalendar.isLeap(year), HebrewDate.isMeuberes(year), "leap $year")
            assertEquals(ReferenceCalendar.yearLength(year), HebrewDate.yearLength(year), "length $year")
            assertEquals(ReferenceCalendar.roshHashanah(year), HebrewDate.roshHashanah(year).localDate, "Rosh Hashanah $year")
            for (month in ReferenceCalendar.monthsInOrder(year)) {
                assertEquals(ReferenceCalendar.monthLength(year, month), HebrewDate.monthLength(year, month), "$month $year")
            }
        }
        assertEquals(HebrewMonth.ADAR, HebrewMonth.of(12))
        assertFailsWith<IllegalArgumentException> { HebrewMonth.of(14) }
    }
}
