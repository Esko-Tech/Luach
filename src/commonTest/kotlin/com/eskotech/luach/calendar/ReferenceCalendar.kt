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

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

internal object ReferenceCalendar {
    const val FIRST_YEAR = 5770

    const val LAST_YEAR = 5790

    val ROSH_HASHANAH: Map<Int, LocalDate> = mapOf(
        5770 to LocalDate(2009, 9, 19), 5771 to LocalDate(2010, 9, 9), 5772 to LocalDate(2011, 9, 29),
        5773 to LocalDate(2012, 9, 17), 5774 to LocalDate(2013, 9, 5), 5775 to LocalDate(2014, 9, 25),
        5776 to LocalDate(2015, 9, 14), 5777 to LocalDate(2016, 10, 3), 5778 to LocalDate(2017, 9, 21),
        5779 to LocalDate(2018, 9, 10), 5780 to LocalDate(2019, 9, 30), 5781 to LocalDate(2020, 9, 19),
        5782 to LocalDate(2021, 9, 7), 5783 to LocalDate(2022, 9, 26), 5784 to LocalDate(2023, 9, 16),
        5785 to LocalDate(2024, 10, 3), 5786 to LocalDate(2025, 9, 23), 5787 to LocalDate(2026, 9, 12),
        5788 to LocalDate(2027, 10, 2), 5789 to LocalDate(2028, 9, 21), 5790 to LocalDate(2029, 9, 10),
        5791 to LocalDate(2030, 9, 28),
    )

    private val LEAP_YEARS_OF_CYCLE = setOf(0, 3, 6, 8, 11, 14, 17)

    fun isLeap(year: Int): Boolean = year % 19 in LEAP_YEARS_OF_CYCLE

    fun roshHashanah(year: Int): LocalDate = ROSH_HASHANAH.getValue(year)

    fun yearLength(year: Int): Int = roshHashanah(year).daysUntil(roshHashanah(year + 1))

    fun monthsInOrder(year: Int): List<HebrewMonth> {
        val adar = if (isLeap(year)) listOf(HebrewMonth.ADAR, HebrewMonth.ADAR_II) else listOf(HebrewMonth.ADAR)
        return listOf(HebrewMonth.TISHREI, HebrewMonth.CHESHVAN, HebrewMonth.KISLEV, HebrewMonth.TEVET, HebrewMonth.SHEVAT) + adar +
            listOf(HebrewMonth.NISAN, HebrewMonth.IYAR, HebrewMonth.SIVAN, HebrewMonth.TAMMUZ, HebrewMonth.AV, HebrewMonth.ELUL)
    }

    fun monthLength(year: Int, month: HebrewMonth): Int {
        val length = yearLength(year)
        return when (month) {
            HebrewMonth.TISHREI, HebrewMonth.SHEVAT, HebrewMonth.NISAN, HebrewMonth.SIVAN, HebrewMonth.AV -> 30
            HebrewMonth.CHESHVAN -> if (length % 10 == 5) 30 else 29
            HebrewMonth.KISLEV -> if (length % 10 == 3) 29 else 30
            HebrewMonth.TEVET, HebrewMonth.IYAR, HebrewMonth.TAMMUZ, HebrewMonth.ELUL, HebrewMonth.ADAR_II -> 29
            HebrewMonth.ADAR -> if (isLeap(year)) 30 else 29
        }
    }

    fun dayOfYear(year: Int, month: HebrewMonth, day: Int): Int {
        var elapsed = 0
        for (m in monthsInOrder(year)) {
            if (m == month) {
                return elapsed + day
            }
            elapsed += monthLength(year, m)
        }
        throw IllegalArgumentException("$month is not in $year")
    }

    fun gregorian(year: Int, month: HebrewMonth, day: Int): LocalDate =
        roshHashanah(year).plus(dayOfYear(year, month, day) - 1, DateTimeUnit.DAY)

    class Day(val year: Int, val month: HebrewMonth, val day: Int, val date: LocalDate) {
        val dayOfWeek: DayOfWeek = date.dayOfWeek

        val ordinalInYear: Int = dayOfYear(year, month, day)

        override fun toString(): String = "$day $month $year ($date, $dayOfWeek)"
    }

    fun days(year: Int): List<Day> {
        val result = ArrayList<Day>(385)
        var date = roshHashanah(year)
        for (month in monthsInOrder(year)) {
            for (day in 1..monthLength(year, month)) {
                result.add(Day(year, month, day, date))
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
        return result
    }

    fun allDays(): Sequence<Day> = (FIRST_YEAR..LAST_YEAR).asSequence().flatMap { days(it).asSequence() }

    fun weekdayOf(day: Day, dayOfMonth: Int): DayOfWeek = DayOfWeek.entries[(day.dayOfWeek.ordinal + dayOfMonth - day.day).mod(7)]
}
