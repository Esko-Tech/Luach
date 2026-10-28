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
import com.eskotech.luach.format.LuachFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class ParshaTest {

    private val firstYear = 5700

    private val lastYear = 5900

    private fun date(year: Int, month: HebrewMonth, day: Int): LocalDate = HebrewDate.of(year, month, day).localDate

    private fun shabbosOnOrBefore(date: LocalDate): LocalDate = date.minus((date.dayOfWeek.ordinal + 2) % 7, DateTimeUnit.DAY)

    private fun shabbosAfter(date: LocalDate): LocalDate = shabbosOnOrBefore(date).plus(7, DateTimeUnit.DAY)

    private fun day(date: LocalDate, inIsrael: Boolean): HebrewDay = HebrewDay(date, inIsrael = inIsrael)

    private fun simchasTorah(year: Int, inIsrael: Boolean): LocalDate = date(year, HebrewMonth.TISHREI, if (inIsrael) 22 else 23)

    private fun shabbososOfCycle(year: Int, inIsrael: Boolean): List<LocalDate> {
        val result = ArrayList<LocalDate>()
        var shabbos = shabbosAfter(simchasTorah(year, inIsrael))
        val end = simchasTorah(year + 1, inIsrael)
        while (shabbos < end) {
            result.add(shabbos)
            shabbos = shabbos.plus(7, DateTimeUnit.DAY)
        }
        return result
    }

    private fun hasNoWeeklyReading(k: HebrewDay): Boolean {
        val d = k.dayOfMonth
        return when (k.month) {
            HebrewMonth.TISHREI -> d == 1 || d == 2 || d == 10 || d in 15..(if (k.inIsrael) 22 else 23)
            HebrewMonth.NISAN -> d in 15..(if (k.inIsrael) 21 else 22)
            HebrewMonth.SIVAN -> d == 6 || (d == 7 && !k.inIsrael)
            else -> false
        }
    }

    @Test
    fun enumGroups() {
        assertEquals(53, Parsha.WEEKLY.size)
        assertEquals(Parsha.BERESHIT, Parsha.WEEKLY.first())
        assertEquals(Parsha.HAAZINU, Parsha.WEEKLY.last())
        assertEquals(7, Parsha.DOUBLES.size)
        assertEquals(9, Parsha.SPECIAL_SHABBOSOS.size)
        for (double in Parsha.DOUBLES) {
            assertEquals(double.first!!.ordinal + 1, double.second!!.ordinal, "$double")
        }
    }

    @Test
    fun shabbosWithoutWeeklyReadingIsExactlyYomTov() {
        val mismatches = Mismatches()
        for (inIsrael in listOf(false, true)) {
            for (year in firstYear..lastYear) {
                for (shabbos in shabbososOfCycle(year, inIsrael)) {
                    val k = day(shabbos, inIsrael)
                    mismatches.check((k.parsha == null) == hasNoWeeklyReading(k)) { "$shabbos (${k.date}) inIsrael=$inIsrael: parsha ${k.parsha}" }
                }
            }
        }
        mismatches.assertNone("Shabbos readings")
    }

    @Test
    fun everyCycleReadsEveryParshaOnceInOrder() {
        val mismatches = Mismatches()
        for (inIsrael in listOf(false, true)) {
            for (year in firstYear..lastYear) {
                val read = ArrayList<Parsha>()
                for (shabbos in shabbososOfCycle(year, inIsrael)) {
                    val parsha = day(shabbos, inIsrael).parsha ?: continue
                    if (parsha.isDouble) {
                        read += parsha.first!!
                        read += parsha.second!!
                    } else {
                        read += parsha
                    }
                }
                mismatches.check(read == Parsha.WEEKLY) { "cycle starting $year inIsrael=$inIsrael read $read" }
            }
        }
        mismatches.assertNone("Torah cycles")
    }

    @Test
    fun weekdaysHaveNoParsha() {
        val mismatches = Mismatches()
        var date = date(5784, HebrewMonth.TISHREI, 1)
        val end = date(5787, HebrewMonth.TISHREI, 1)
        while (date < end) {
            if (date.dayOfWeek != DayOfWeek.SATURDAY) {
                val k = HebrewDay(date)
                mismatches.check(k.parsha == null && k.specialShabbos == null) { "$date: ${k.parsha} ${k.specialShabbos}" }
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        mismatches.assertNone("weekdays")
    }

    @Test
    fun anchoredShabbosos() {
        val mismatches = Mismatches()
        for (inIsrael in listOf(false, true)) {
            for (year in firstYear..lastYear) {
                val leap = HebrewDate.isMeuberes(year)
                val roshHashanahWeekday = date(year, HebrewMonth.TISHREI, 1).dayOfWeek
                val nextRoshHashanahWeekday = date(year + 1, HebrewMonth.TISHREI, 1).dayOfWeek
                fun check(label: String, date: LocalDate, expected: Set<Parsha?>) {
                    val actual = day(date, inIsrael).parsha
                    mismatches.check(actual in expected) { "$year inIsrael=$inIsrael $label on $date: $actual expected $expected" }
                }
                check("Bereishis", shabbosAfter(simchasTorah(year, inIsrael)), setOf(Parsha.BERESHIT))
                val beforePesach = when {
                    !leap -> Parsha.TZAV
                    roshHashanahWeekday == DayOfWeek.THURSDAY -> Parsha.ACHAREI_MOT
                    else -> Parsha.METZORA
                }
                check("Shabbos before Pesach", shabbosOnOrBefore(date(year, HebrewMonth.NISAN, 14)), setOf(beforePesach))
                val beforeShavuos = if (inIsrael) {
                    setOf(Parsha.BAMIDBAR, Parsha.NASO)
                } else {
                    setOf(if (leap && roshHashanahWeekday == DayOfWeek.THURSDAY) Parsha.NASO else Parsha.BAMIDBAR)
                }
                check("Shabbos before Shavuos", shabbosOnOrBefore(date(year, HebrewMonth.SIVAN, 5)), beforeShavuos)
                check("Shabbos Chazon", shabbosOnOrBefore(date(year, HebrewMonth.AV, 9)), setOf(Parsha.DEVARIM))
                check("Shabbos Nachamu", shabbosOnOrBefore(date(year, HebrewMonth.AV, 16)), setOf(Parsha.VAETCHANAN))
                val earlyRoshHashanah = nextRoshHashanahWeekday == DayOfWeek.MONDAY || nextRoshHashanahWeekday == DayOfWeek.TUESDAY
                check(
                    "Shabbos before Rosh Hashanah", shabbosOnOrBefore(date(year, HebrewMonth.ELUL, 29)),
                    setOf(if (earlyRoshHashanah) Parsha.NITZAVIM else Parsha.NITZAVIM_VAYELECH),
                )
                val shuvah = shabbosOnOrBefore(date(year + 1, HebrewMonth.TISHREI, 9))
                check("Shabbos Shuvah", shuvah, setOf(if (earlyRoshHashanah) Parsha.VAYELECH else Parsha.HAAZINU))
                check("Shabbos after Shuvah", shuvah.plus(7, DateTimeUnit.DAY), setOf(if (earlyRoshHashanah) Parsha.HAAZINU else null))
            }
        }
        mismatches.assertNone("anchored Shabbosos")
    }

    @Test
    fun specialShabbosRules() {
        val mismatches = Mismatches()
        for (inIsrael in listOf(false, true)) {
            for (year in firstYear..lastYear) {
                val leap = HebrewDate.isMeuberes(year)
                val purimMonth = if (leap) HebrewMonth.ADAR_II else HebrewMonth.ADAR
                val shekalim = shabbosOnOrBefore(date(year, purimMonth, 1))
                val zachor = shabbosOnOrBefore(date(year, purimMonth, 13))
                val hachodesh = shabbosOnOrBefore(date(year, HebrewMonth.NISAN, 1))
                val parah = hachodesh.minus(7, DateTimeUnit.DAY)
                val hagadol = shabbosOnOrBefore(date(year, HebrewMonth.NISAN, 14))
                val chazon = shabbosOnOrBefore(date(year, HebrewMonth.AV, 9))
                val nachamu = shabbosOnOrBefore(date(year, HebrewMonth.AV, 16))
                val shuvah = shabbosOnOrBefore(date(year, HebrewMonth.TISHREI, 9))
                var shabbos = shabbosOnOrBefore(date(year, HebrewMonth.TISHREI, 1))
                val end = date(year + 1, HebrewMonth.TISHREI, 1)
                while (shabbos < end) {
                    val k = day(shabbos, inIsrael)
                    val expected = when (shabbos) {
                        shekalim -> Parsha.SHEKALIM
                        zachor -> Parsha.ZACHOR
                        parah -> Parsha.PARAH
                        hachodesh -> Parsha.HACHODESH
                        hagadol -> Parsha.HAGADOL
                        chazon -> Parsha.CHAZON
                        nachamu -> Parsha.NACHAMU
                        shuvah -> Parsha.SHUVAH
                        else -> if (k.parsha == Parsha.BESHALACH) Parsha.SHIRAH else null
                    }
                    mismatches.check(k.specialShabbos == expected) { "$shabbos (${k.date}) inIsrael=$inIsrael: ${k.specialShabbos} expected $expected" }
                    shabbos = shabbos.plus(7, DateTimeUnit.DAY)
                }
            }
        }
        mismatches.assertNone("special Shabbosos")
    }

    @Test
    fun upcomingParshaIsTheNextShabbosWithAReading() {
        val mismatches = Mismatches()
        for (inIsrael in listOf(false, true)) {
            var date = date(5783, HebrewMonth.TISHREI, 1)
            val end = date(5788, HebrewMonth.TISHREI, 1)
            while (date < end) {
                var shabbos = if (date.dayOfWeek == DayOfWeek.SATURDAY) date.plus(7, DateTimeUnit.DAY) else shabbosAfter(date)
                while (day(shabbos, inIsrael).parsha == null) {
                    shabbos = shabbos.plus(7, DateTimeUnit.DAY)
                }
                val expected = day(shabbos, inIsrael).parsha
                val actual = day(date, inIsrael).upcomingParsha
                mismatches.check(actual == expected) { "$date inIsrael=$inIsrael: upcomingParsha $actual expected $expected" }
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
        mismatches.assertNone("upcoming parsha")
    }

    @Test
    fun rememberedParshios() {
        val formatter = LuachFormatter()
        val remembered = listOf(
            LocalDate(2023, 10, 14) to "Bereishis", LocalDate(2024, 1, 27) to "Beshalach", LocalDate(2024, 3, 9) to "Vayakhel",
            LocalDate(2024, 4, 20) to "Metzora", LocalDate(2024, 6, 8) to "Bamidbar", LocalDate(2024, 8, 10) to "Devarim",
            LocalDate(2024, 9, 28) to "Nitzavim-Vayeilech", LocalDate(2024, 10, 5) to "Ha'azinu", LocalDate(2025, 4, 12) to "Tzav",
            LocalDate(2025, 9, 20) to "Nitzavim", LocalDate(2025, 9, 27) to "Vayeilech", LocalDate(2022, 4, 9) to "Metzora",
            LocalDate(2014, 4, 12) to "Acharei Mos", LocalDate(2011, 4, 16) to "Acharei Mos", LocalDate(2014, 5, 31) to "Naso",
            LocalDate(2023, 9, 9) to "Nitzavim-Vayeilech", LocalDate(2023, 4, 1) to "Tzav", LocalDate(2020, 5, 23) to "Bamidbar",
            LocalDate(2024, 2, 17) to "Terumah",
        )
        for ((date, expected) in remembered) {
            assertEquals(expected, formatter.formatParsha(HebrewDay(date)), "$date")
        }
        assertEquals("Kedoshim", formatter.formatParsha(HebrewDay(LocalDate(2022, 4, 30), inIsrael = true)))
        assertEquals("Acharei Mos", formatter.formatParsha(HebrewDay(LocalDate(2022, 4, 30))))
        assertEquals("Naso", formatter.formatParsha(HebrewDay(LocalDate(2022, 6, 4), inIsrael = true)))
        assertEquals("Bamidbar", formatter.formatParsha(HebrewDay(LocalDate(2022, 6, 4))))
        assertEquals("בראשית", LuachFormatter(hebrew = true).formatParsha(HebrewDay(LocalDate(2023, 10, 14))))
    }
}
