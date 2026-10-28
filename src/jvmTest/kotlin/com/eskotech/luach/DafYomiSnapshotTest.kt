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

package com.eskotech.luach

import com.eskotech.luach.calendar.BavliTractate
import com.eskotech.luach.calendar.DafYomi
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.YerushalmiTractate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.LocalDate

class DafYomiSnapshotTest {
    @Test
    fun bavliEveryDay1923To2100() {
        val days = SampleDates.days(LocalDate(1923, 9, 11), LocalDate(2100, 12, 31))
        Snapshot.collect("DafYomi.bavliEveryDay1923To2100", days) { s, date ->
            s.group = SampleDates.decade(date)
            s.recordOutcome("DafYomi.bavli") { DafYomi.bavli(date) }
            s.recordOutcome("HebrewDay.bavliDaf") { HebrewDay(date).bavliDaf }
        }.assertMatches()
    }

    @Test
    fun bavliBeforeFirstCycleThrows() {
        val dates = SampleDates.days(LocalDate(1900, 1, 1), LocalDate(1923, 9, 10)) + listOf(LocalDate(1850, 6, 1), LocalDate(1800, 1, 1))
        for (date in dates) {
            assertFailsWith<IllegalArgumentException>("$date") { DafYomi.bavli(date) }
        }
    }

    @Test
    fun yerushalmiEveryDay1980To2100() {
        val days = SampleDates.days(LocalDate(1980, 2, 2), LocalDate(2100, 12, 31))
        val snapshot = Snapshot.collect("DafYomi.yerushalmiEveryDay1980To2100", days) { s, date ->
            s.group = SampleDates.decade(date)
            val daf = outcome { DafYomi.yerushalmi(date) }
            if (daf == null) s.tally("days without a daf")
            s.record("DafYomi.yerushalmi", daf)
            if (date.toEpochDays() % 7 == 0L) {
                s.recordOutcome("HebrewDay.yerushalmiDaf") { HebrewDay(date).yerushalmiDaf }
            }
        }
        snapshot.assertMatches()
        assertEquals(242L, snapshot.tallies["days without a daf"], "days without a Yerushalmi daf")
    }

    @Test
    fun yerushalmiBeforeFirstCycle() {
        var withoutDaf = 0
        for (date in SampleDates.days(LocalDate(1974, 1, 1), LocalDate(1980, 2, 1)) + LocalDate(1923, 9, 11)) {
            val day = HebrewDay(date)
            if (day.isYomKippur || day.isTishaBeav) {
                assertEquals(null, DafYomi.yerushalmi(day), "$date")
                withoutDaf++
            } else {
                assertFailsWith<IllegalArgumentException>("$date") { DafYomi.yerushalmi(day) }
            }
        }
        assertEquals(12, withoutDaf, "Yom Kippur and Tisha B'Av of 1974..1979")
    }

    @Test
    fun tractates() {
        Snapshot.single("DafYomi.tractates") { s ->
            s.record("bavli", BavliTractate.entries.map { listOf(it.name, it.hebrewName, it.firstPage, it.lastPage, it.pages) })
            s.record("yerushalmi", YerushalmiTractate.entries.map { listOf(it.name, it.hebrewName, it.pages) })
            s.record("cycle starts", listOf(DafYomi.BAVLI_FIRST_CYCLE, DafYomi.BAVLI_EIGHTH_CYCLE, DafYomi.YERUSHALMI_FIRST_CYCLE))
        }.assertMatches()
    }
}
