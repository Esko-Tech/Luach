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

import com.eskotech.luach.calendar.BavliDaf
import com.eskotech.luach.calendar.BavliTractate
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.HebrewMonth
import com.eskotech.luach.calendar.YerushalmiDaf
import com.eskotech.luach.calendar.YerushalmiTractate
import com.eskotech.luach.format.LuachFormatter
import com.eskotech.luach.zmanim.Zman
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class FormatterSnapshotTest {
    private fun recordDateFormats(s: SnapshotRecorder, f: LuachFormatter, k: HebrewDay) {
        s.recordOutcome("formatDate") { f.formatDate(k.date) }
        s.recordOutcome("formatMonth") { f.formatMonth(k.date) }
        s.recordOutcome("formatWeekday") { f.formatWeekday(k.date) }
        s.recordOutcome("formatHoliday") { f.formatHoliday(k) }
        s.recordOutcome("formatRoshChodesh") { f.formatRoshChodesh(k) }
        s.recordOutcome("formatOmer") { f.formatOmer(k) }
        s.recordOutcome("formatParsha") { f.formatParsha(k) }
        s.recordOutcome("formatSpecialShabbos") { f.formatSpecialShabbos(k) }
    }

    @Test
    fun everyDay1900To2100() {
        Snapshot.collect("Formatter.everyDay1900To2100", (1900..2100).toList()) { s, year ->
            for (date in SampleDates.year(year)) for (inIsrael in listOf(false, true)) {
                val k = HebrewDay(date, inIsrael = inIsrael)
                for (f in CONFIGS) {
                    s.group = f.toString()
                    recordDateFormats(s, f, k)
                }
            }
        }.assertMatches()
    }

    @Test
    fun numbers() {
        val numbers = (-10..10_010) + listOf(Int.MIN_VALUE, Int.MAX_VALUE, 99_999, 100_000)
        Snapshot.single("Formatter.numbers") { s ->
            for (f in CONFIGS.filter { it.hebrew && !it.abbreviatedWeekdays }) {
                s.group = f.toString()
                for (number in numbers) s.recordOutcome("formatNumber") { f.formatNumber(number) }
            }
        }.assertMatches()
    }

    @Test
    fun yearSignatures() {
        Snapshot.single("Formatter.yearSignatures") { s ->
            val f = LuachFormatter(hebrew = true)
            for (year in 3762..6500) s.recordOutcome("formatYearSignature") { f.formatYearSignature(year) }
        }.assertMatches()
    }

    @Test
    fun dafFormats() {
        Snapshot.single("Formatter.dafFormats") { s ->
            for (f in CONFIGS.filter { !it.abbreviatedWeekdays && !it.thousands }) {
                s.group = f.toString()
                for (page in listOf(0, 1, 2, 9, 10, 15, 16, 20, 21, 99, 100, 101, 115, 150, 176, 177, 300)) {
                    for (tractate in BavliTractate.entries) s.recordOutcome("formatDaf bavli") { f.formatDaf(BavliDaf(tractate, page)) }
                    for (tractate in YerushalmiTractate.entries) s.recordOutcome("formatDaf yerushalmi") { f.formatDaf(YerushalmiDaf(tractate, page)) }
                }
                s.recordOutcome("formatDaf(null)") { f.formatDaf(null as YerushalmiDaf?) }
            }
        }.assertMatches()
    }

    @Test
    fun defaults() {
        Snapshot.single("Formatter.defaults") { s ->
            val f = LuachFormatter()
            s.record("hebrew", f.hebrew)
            s.record("quoteMarks", f.quoteMarks)
            s.record("thousands", f.thousands)
            s.record("finalLetters", f.finalLetters)
            s.record("abbreviatedWeekdays", f.abbreviatedWeekdays)
            s.record("monthNames", f.monthNames)
            s.record("adarInLeapYear", f.adarInLeapYear)
            s.record("weekdayNames", f.weekdayNames)
            s.record("holidayNames", f.holidayNames)
            s.record("roshChodeshName", f.roshChodeshName)
            s.record("omerFormat", f.omerFormat)
            s.record("omerPrefix", f.omerPrefix)
            s.record("parshaNames", f.parshaNames)
            s.record("bavliNames", f.bavliNames)
            s.record("yerushalmiNames", f.yerushalmiNames)
            s.record("noDafName", f.noDafName)
            s.record("zmanNames", f.zmanNames)
            s.record("twelveHourClock", f.twelveHourClock)
            s.record("amPmNames", f.amPmNames)
            for (zman in Zman.entries) {
                s.recordOutcome("formatZman hebrew") { LuachFormatter(hebrew = true).formatZman(zman) }
                s.recordOutcome("formatZman") { f.formatZman(zman) }
            }
            for (month in HebrewMonth.entries) for (leap in listOf(false, true)) {
                s.recordOutcome("formatMonth hebrew") { LuachFormatter(hebrew = true).formatMonth(month, leap) }
                s.recordOutcome("formatMonth") { f.formatMonth(month, leap) }
            }
        }.assertMatches()
    }

    @Test
    fun userSuppliedNames() {
        val custom = LuachFormatter(
            monthNames = LuachFormatter().monthNames.mapValues { it.value.uppercase() },
            adarInLeapYear = "Adar Aleph",
            weekdayNames = LuachFormatter().weekdayNames + (DayOfWeek.SATURDAY to "Shabbat"),
            holidayNames = LuachFormatter().holidayNames.mapValues { "~${it.value}~" },
            roshChodeshName = "RC",
            omerFormat = "Day {day}",
            parshaNames = LuachFormatter().parshaNames.mapValues { "<${it.value}>" },
            noDafName = "none",
        )
        val hebrewCustom = LuachFormatter(hebrew = true, omerPrefix = "ל", quoteMarks = false)
        Snapshot.collect("Formatter.userSuppliedNames", (2015..2035).toList()) { s, year ->
            for (date in SampleDates.year(year)) for (inIsrael in listOf(false, true)) {
                val k = HebrewDay(date, inIsrael = inIsrael)
                for (f in listOf(custom, hebrewCustom)) {
                    s.group = f.toString()
                    recordDateFormats(s, f, k)
                }
            }
        }.assertMatches()
    }

    companion object {
        val CONFIGS: List<LuachFormatter> = buildList {
            for (quoteMarks in listOf(true, false)) for (thousands in listOf(false, true)) for (finalLetters in listOf(false, true)) {
                for (abbreviated in listOf(false, true)) add(LuachFormatter(hebrew = true, quoteMarks = quoteMarks, thousands = thousands, finalLetters = finalLetters, abbreviatedWeekdays = abbreviated))
            }
            for (abbreviated in listOf(false, true)) add(LuachFormatter(abbreviatedWeekdays = abbreviated))
        }
    }

    @Test
    fun times() {
        val zones = listOf("America/New_York", "Asia/Jerusalem", "Pacific/Apia", "UTC").map { TimeZone.of(it) }
        val instants = (0 until 48).map { Instant.parse("2026-10-13T00:00:00Z") + (it * 29).minutes + it.seconds } +
            listOf(Instant.parse("2024-03-10T06:30:00Z"), Instant.parse("2024-11-03T05:30:00Z"), Instant.parse("2024-06-21T11:59:59Z"))
        val formatters = listOf(
            LuachFormatter(),
            LuachFormatter(twelveHourClock = true),
            LuachFormatter(twelveHourClock = true, amPmNames = "" to ""),
            LuachFormatter(hebrew = true, twelveHourClock = true, amPmNames = "לפנה״צ" to "אחה״צ"),
        )
        Snapshot.single("Formatter.times") { s ->
            for (f in formatters) {
                s.group = f.toString()
                for (zone in zones) for (instant in instants) for (seconds in listOf(false, true)) {
                    s.recordOutcome("formatTime ${zone.id}") { f.formatTime(instant, zone, seconds) }
                }
                s.recordOutcome("formatTime null") { f.formatTime(null, zones.first()) }
            }
        }.assertMatches()
    }
}
