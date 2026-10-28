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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

class DafYomiTest {

    private class Tractate(val name: String, val firstDaf: Int, val lastDaf: Int) {
        val pages: Int get() = lastDaf - firstDaf + 1
    }

    private val bavli = listOf(
        Tractate("Berachos", 2, 64), Tractate("Shabbos", 2, 157), Tractate("Eruvin", 2, 105), Tractate("Pesachim", 2, 121),
        Tractate("Shekalim", 2, 22), Tractate("Yoma", 2, 88), Tractate("Sukkah", 2, 56), Tractate("Beitzah", 2, 40),
        Tractate("Rosh Hashanah", 2, 35), Tractate("Taanis", 2, 31), Tractate("Megillah", 2, 32), Tractate("Moed Katan", 2, 29),
        Tractate("Chagigah", 2, 27), Tractate("Yevamos", 2, 122), Tractate("Kesubos", 2, 112), Tractate("Nedarim", 2, 91),
        Tractate("Nazir", 2, 66), Tractate("Sotah", 2, 49), Tractate("Gittin", 2, 90), Tractate("Kiddushin", 2, 82),
        Tractate("Bava Kamma", 2, 119), Tractate("Bava Metzia", 2, 119), Tractate("Bava Basra", 2, 176), Tractate("Sanhedrin", 2, 113),
        Tractate("Makkos", 2, 24), Tractate("Shevuos", 2, 49), Tractate("Avodah Zarah", 2, 76), Tractate("Horayos", 2, 14),
        Tractate("Zevachim", 2, 120), Tractate("Menachos", 2, 110), Tractate("Chullin", 2, 142), Tractate("Bechoros", 2, 61),
        Tractate("Arachin", 2, 34), Tractate("Temurah", 2, 34), Tractate("Kereisos", 2, 28), Tractate("Meilah", 2, 22),
        Tractate("Kinnim", 23, 25), Tractate("Tamid", 26, 33), Tractate("Middos", 34, 37), Tractate("Niddah", 2, 73),
    )

    private val yerushalmiPages = intArrayOf(
        68, 37, 34, 44, 31, 59, 26, 33, 28, 20, 13, 92, 65, 71, 22, 22, 42, 26, 26, 33, 34, 22,
        19, 85, 72, 47, 40, 47, 54, 48, 44, 37, 34, 44, 9, 57, 37, 19, 13,
    )

    private val firstCycleStart = LocalDate(1923, 9, 11)

    private val eighthCycleStart = LocalDate(1975, 6, 24)

    private val yerushalmiStart = LocalDate(1980, 2, 2)

    private fun expectedBavli(date: LocalDate): Pair<Int, Int> {
        val modern = date >= eighthCycleStart
        val start = if (modern) eighthCycleStart else firstCycleStart
        val cycleLength = if (modern) 2711 else 2702
        var index = start.daysUntil(date) % cycleLength
        for ((number, tractate) in bavli.withIndex()) {
            val pages = if (number == 4 && !modern) 12 else tractate.pages
            if (index < pages) {
                return number to tractate.firstDaf + index
            }
            index -= pages
        }
        throw IllegalStateException("$date")
    }

    private fun describe(daf: BavliDaf?): String = daf?.let { "${it.tractate.ordinal}:${it.page}" } ?: "null"

    private fun describe(daf: YerushalmiDaf?): String = daf?.let { "${it.tractate.ordinal}:${it.page}" } ?: "null"

    @Test
    fun tractateTablesMatchTheReference() {
        assertEquals(2711, bavli.sumOf { it.pages })
        assertEquals(2711, BavliTractate.entries.sumOf { it.pages })
        assertEquals(1554, yerushalmiPages.sum())
        assertEquals(1554, YerushalmiTractate.PAGES_PER_CYCLE)
        assertEquals(eighthCycleStart, firstCycleStart.plus(7 * 2702, DateTimeUnit.DAY))
        for ((number, tractate) in bavli.withIndex()) {
            val entry = BavliTractate.entries[number]
            assertEquals(tractate.name, LuachFormatter().bavliNames.getValue(entry), "$number")
            assertEquals(tractate.firstDaf, entry.firstPage, "$entry")
            assertEquals(tractate.lastDaf, entry.lastPage, "$entry")
        }
        for ((number, pages) in yerushalmiPages.withIndex()) {
            assertEquals(pages, YerushalmiTractate.entries[number].pages, "${YerushalmiTractate.entries[number]}")
        }
    }

    @Test
    fun everyDaySince1975MatchesCycleArithmetic() {
        val mismatches = Mismatches()
        var date = eighthCycleStart
        val end = LocalDate(2031, 1, 1)
        while (date < end) {
            val expected = expectedBavli(date)
            val daf = HebrewDay(date).bavliDaf
            mismatches.check(daf.tractate.ordinal == expected.first && daf.page == expected.second) {
                "$date: ${describe(daf)} expected ${expected.first}:${expected.second}"
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        mismatches.assertNone("Daf Yomi Bavli since 1975")
    }

    @Test
    fun cyclesBefore1975UseTwelvePagesOfShekalim() {
        val mismatches = Mismatches()
        var date = firstCycleStart
        while (date < eighthCycleStart) {
            val expected = expectedBavli(date)
            val daf = DafYomi.bavli(date)
            mismatches.check(daf.tractate.ordinal == expected.first && daf.page == expected.second) {
                "$date: ${describe(daf)} expected ${expected.first}:${expected.second}"
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        mismatches.assertNone("Daf Yomi Bavli before 1975")
    }

    @Test
    fun publishedCycleDates() {
        val formatter = LuachFormatter()
        val hebrew = LuachFormatter(hebrew = true)
        val expectations = listOf(
            LocalDate(1923, 9, 11) to "Berachos 2", LocalDate(1975, 6, 24) to "Berachos 2", LocalDate(1997, 9, 29) to "Berachos 2",
            LocalDate(2005, 3, 2) to "Berachos 2", LocalDate(2012, 8, 2) to "Niddah 73", LocalDate(2012, 8, 3) to "Berachos 2",
            LocalDate(2020, 1, 4) to "Niddah 73", LocalDate(2020, 1, 5) to "Berachos 2", LocalDate(2020, 3, 8) to "Shabbos 2",
            LocalDate(2021, 4, 13) to "Yoma 2", LocalDate(2027, 6, 7) to "Niddah 73", LocalDate(2027, 6, 8) to "Berachos 2",
            LocalDate(1924, 12, 9) to "Yoma 2", LocalDate(2027, 3, 13) to "Kinnim 23", LocalDate(2027, 3, 27) to "Middos 37",
            LocalDate(1924, 12, 8) to "Shekalim 13",
        )
        for ((date, expected) in expectations) {
            assertEquals(expected, formatter.formatDaf(DafYomi.bavli(date)), "$date")
        }
        assertEquals("ברכות ב׳", hebrew.formatDaf(DafYomi.bavli(LocalDate(2020, 1, 5))))
        assertEquals("נדה ע״ג", hebrew.formatDaf(DafYomi.bavli(LocalDate(2020, 1, 4))))
        assertEquals(BavliDaf(BavliTractate.YOMA, 2), HebrewDay(HebrewDate.of(5685, HebrewMonth.KISLEV, 12)).bavliDaf)
        assertEquals(BavliDaf(BavliTractate.SHEKALIM, 14), HebrewDay(HebrewDate.of(5736, HebrewMonth.ELUL, 26)).bavliDaf)
        assertEquals(BavliDaf(BavliTractate.SANHEDRIN, 47), HebrewDay(HebrewDate.of(5777, HebrewMonth.ELUL, 10)).bavliDaf)
        assertFailsWith<IllegalArgumentException> { DafYomi.bavli(LocalDate(1923, 9, 10)) }
        assertFailsWith<IllegalArgumentException> { HebrewDay(LocalDate(1850, 6, 1)).bavliDaf }
    }

    private fun observedFastDay(k: HebrewDay): Boolean {
        val d = k.dayOfMonth
        val w = k.dayOfWeek
        return (k.month == HebrewMonth.TISHREI && d == 10) ||
            (k.month == HebrewMonth.AV && ((d == 9 && w != DayOfWeek.SATURDAY) || (d == 10 && w == DayOfWeek.SUNDAY)))
    }

    @Test
    fun yerushalmiEveryDaySince1980SkipsOnlyObservedFastDays() {
        val mismatches = Mismatches()
        var date = yerushalmiStart
        val end = LocalDate(2031, 1, 1)
        var index = 0
        while (date < end) {
            val k = HebrewDay(date)
            val daf = k.yerushalmiDaf
            if (observedFastDay(k)) {
                mismatches.check(daf == null) { "$date is a fast day but returned ${describe(daf)}" }
            } else {
                var remaining = index
                var tractate = 0
                while (remaining >= yerushalmiPages[tractate]) {
                    remaining -= yerushalmiPages[tractate]
                    tractate++
                }
                mismatches.check(daf != null && daf.tractate.ordinal == tractate && daf.page == remaining + 1) {
                    "$date: ${describe(daf)} expected $tractate:${remaining + 1}"
                }
                index = (index + 1) % 1554
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        mismatches.assertNone("Daf Yomi Yerushalmi")
    }

    @Test
    fun yerushalmiAnchors() {
        val formatter = LuachFormatter()
        val hebrew = LuachFormatter(hebrew = true)
        assertEquals("Berachos 1", formatter.formatDaf(DafYomi.yerushalmi(yerushalmiStart)))
        assertEquals("ברכות א׳", hebrew.formatDaf(DafYomi.yerushalmi(yerushalmiStart)))
        assertNull(HebrewDay(LocalDate(2025, 10, 2)).yerushalmiDaf)
        assertNull(HebrewDay(LocalDate(2022, 8, 7)).yerushalmiDaf)
        assertNotNull(HebrewDay(LocalDate(2022, 8, 6)).yerushalmiDaf)
        assertEquals("No Daf Today", formatter.formatDaf(HebrewDay(LocalDate(2025, 10, 2)).yerushalmiDaf))
        assertEquals("אין דף היום", hebrew.formatDaf(HebrewDay(LocalDate(2025, 10, 2)).yerushalmiDaf))
        assertEquals(YerushalmiDaf(YerushalmiTractate.KIDDUSHIN, 8), HebrewDay(HebrewDate.of(5777, HebrewMonth.ELUL, 10)).yerushalmiDaf)
        assertEquals(YerushalmiDaf(YerushalmiTractate.BAVA_BASRA, 26), HebrewDay(HebrewDate.of(5744, HebrewMonth.KISLEV, 1)).yerushalmiDaf)
        assertEquals(YerushalmiDaf(YerushalmiTractate.SHEVUOS, 15), HebrewDay(HebrewDate.of(5782, HebrewMonth.SIVAN, 1)).yerushalmiDaf)
        for (fast in listOf(HebrewDate.of(5775, HebrewMonth.TISHREI, 10), HebrewDate.of(5783, HebrewMonth.AV, 9), HebrewDate.of(5775, HebrewMonth.AV, 10))) {
            assertNull(HebrewDay(fast).yerushalmiDaf, "$fast")
        }
        assertFailsWith<IllegalArgumentException> { HebrewDay(LocalDate(1980, 2, 1)).yerushalmiDaf }
        assertNull(HebrewDay(HebrewDate.of(5739, HebrewMonth.TISHREI, 10)).yerushalmiDaf)
    }

    @Test
    fun bavliFromAHebrewDay() {
        for (date in listOf(LocalDate(1975, 6, 24), LocalDate(2020, 1, 5), LocalDate(2024, 3, 1), LocalDate(2026, 10, 27))) {
            assertEquals(DafYomi.bavli(date), DafYomi.bavli(HebrewDay(date)))
            assertEquals(DafYomi.bavli(date), HebrewDay(date, inIsrael = true).bavliDaf)
        }
    }
}
