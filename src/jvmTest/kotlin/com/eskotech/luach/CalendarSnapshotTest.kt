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

import com.eskotech.luach.calendar.HebrewDate
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.HebrewMonth
import com.eskotech.luach.calendar.Parsha
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class CalendarSnapshotTest {
    private val dateGetters = Getters.zeroArg(HebrewDate::class.java)

    private val dayGetters = Getters.zeroArg(HebrewDay::class.java, exclude = setOf("getBavliDaf", "getYerushalmiDaf", "getDayOfWeek", "getLocalDate", "getMonth", "getDayOfMonth", "getYear", "getMolad"))

    private val dateOnly = dayGetters.filter { it.name in DATE_ONLY }

    private val settingDependent = dayGetters - dateOnly.toSet()

    @Test
    fun getterInventory() {
        assertEquals(13, dateGetters.size, "HebrewDate zero-arg getters: ${dateGetters.map { it.name }}")
        assertEquals(38, dayGetters.size, "HebrewDay zero-arg getters: ${dayGetters.map { it.name }}")
    }

    @Test
    fun everyDay1850To2150() {
        val days = SampleDates.days(LocalDate(1850, 1, 1), LocalDate(2150, 12, 31))
        Snapshot.collect("Calendar.everyDay1850To2150", days) { s, date ->
            s.group = SampleDates.decade(date)
            val hebrew = HebrewDate.from(date)
            s.recordGetters(dateGetters, hebrew)
            s.record("toString", hebrew.toString())
            val day = HebrewDay(hebrew)
            s.recordGetters(dateOnly, day)
            for (inIsrael in BOOLEANS) {
                s.recordGetters(settingDependent, day.copy(inIsrael = inIsrael))
            }
        }.assertMatches()
    }

    @Test
    fun mukefChomaEveryDay1990To2060() {
        val days = SampleDates.days(LocalDate(1990, 1, 1), LocalDate(2060, 12, 31))
        Snapshot.collect("Calendar.mukefChomaEveryDay1990To2060", days) { s, date ->
            s.group = SampleDates.decade(date)
            for (inIsrael in BOOLEANS) {
                s.recordGetters(settingDependent, HebrewDay(date, inIsrael = inIsrael, mukefChoma = true))
            }
        }.assertMatches()
    }

    @Test
    fun instantConstructors() {
        val dates = SampleDates.year(2011) + SampleDates.year(2024) + SampleDates.SPOT
        val zones = listOf("America/New_York", "Asia/Jerusalem", "Pacific/Apia", "Pacific/Kiritimati", "America/Santiago", "UTC")
        Snapshot.collect("Calendar.instantConstructors", zones) { s, zone ->
            s.group = zone
            val timeZone = TimeZone.of(zone)
            for (date in dates) {
                val midnight = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
                for (millis in listOf(midnight - 1, midnight, midnight + 1, midnight + 12 * 3_600_000L)) {
                    val instant = Instant.fromEpochMilliseconds(millis)
                    s.record("HebrewDate.from(instant, zone)", HebrewDate.from(instant, timeZone))
                    s.record("HebrewDay(instant, zone)", HebrewDay(instant, timeZone))
                }
            }
        }.assertMatches()
    }

    @Test
    fun arithmetic() {
        Snapshot.single("Calendar.arithmetic") { s ->
            s.group = "day by day"
            var k = HebrewDate.from(LocalDate(1900, 1, 1))
            repeat(55_000) {
                k = k.plusDays(1)
                s.record("plusDays(1)", k)
            }
            repeat(7_300) {
                k = k.minusDays(1)
                s.record("minusDays(1)", k)
            }
            s.group = "steps"
            for (startYear in listOf(5600, 5784, 5785, 5999)) for (amount in listOf(1, 2, 7, 13, 30, 100, -1, -13)) {
                for (startMonth in HebrewMonth.entries) {
                    val start = outcome { HebrewDate.of(startYear, startMonth, 30) }
                    s.record("of(year, month, 30)", start)
                    if (start !is HebrewDate) continue
                    s.recordOutcome("plusDays($amount)") { start.plusDays(amount) }
                    s.recordOutcome("plusMonths($amount)") { start.plusMonths(amount) }
                    s.recordOutcome("plusYears($amount)") { start.plusYears(amount) }
                    s.recordOutcome("withDay(29)") { start.withDay(29) }
                }
            }
        }.assertMatches()
    }

    @Test
    fun factoriesAndStatics() {
        Snapshot.single("Calendar.factoriesAndStatics") { s ->
            s.group = "statics"
            for (year in 1..9999) {
                s.record("roshHashanah", HebrewDate.roshHashanah(year))
                s.record("yearLength", HebrewDate.yearLength(year))
                s.record("yearType", HebrewDate.yearType(year))
                s.record("isMeuberes", HebrewDate.isMeuberes(year))
            }
            s.group = "of(year, month, day)"
            for (year in (1..5) + (3761..3765) + (5600..5900 step 7) + (5780..5790) + (9995..9999)) for (month in 1..14) for (day in listOf(1, 15, 29, 30, 31, 0)) {
                s.recordOutcome("of") { HebrewDate.of(year, month, day) }
                s.recordOutcome("HebrewDay") { HebrewDay(HebrewDate.of(year, month, day), inIsrael = true).holiday }
            }
            s.group = "epoch days"
            for (epochDay in listOf(-2092592L, -2092591L, -2092590L, -2092589L, -1000000L, -719528L, 0L, 10957L, 19999L, 100000L, 2932896L)) {
                s.recordOutcome("ofEpochDay") { HebrewDate.ofEpochDay(epochDay) }
            }
            s.group = "molad"
            var month = HebrewDate.of(5600, HebrewMonth.TISHREI, 1)
            repeat(4000) {
                val molad = month.molad
                s.record("molad", molad)
                s.record("molad instant", molad.instant)
                s.record("kiddush levana", listOf(molad.earliestKiddushLevana3Days, molad.earliestKiddushLevana7Days, molad.latestKiddushLevanaHalfMonth, molad.latestKiddushLevana15Days))
                month = month.plusMonths(1)
            }
            s.group = "ordering"
            val days = SampleDates.days(LocalDate(2023, 9, 1), LocalDate(2024, 10, 31))
            for ((a, b) in days.zip(days.drop(1) + days.first())) {
                val ka = HebrewDate.from(a)
                val kb = HebrewDate.from(b)
                s.record("compareTo", Integer.signum(ka.compareTo(kb)))
                s.record("equals", ka == kb)
                s.record("daysUntil", ka.daysUntil(kb))
            }
        }.assertMatches()
    }

    @Test
    fun parshaSchedules() {
        Snapshot.single("Calendar.parshaSchedules") { s ->
            s.record("Parsha entries", Parsha.entries)
            s.record("weekly", Parsha.WEEKLY)
            s.record("doubles", Parsha.DOUBLES.map { listOf(it, it.first, it.second) })
            for (year in 5600..6000) for (inIsrael in BOOLEANS) {
                s.group = if (inIsrael) "Israel" else "diaspora"
                var shabbos = HebrewDate.of(year, HebrewMonth.TISHREI, 1)
                while (shabbos.dayOfWeek != kotlinx.datetime.DayOfWeek.SATURDAY) shabbos = shabbos.plusDays(1)
                val readings = ArrayList<Any?>()
                while (shabbos.year == year) {
                    readings += HebrewDay(shabbos, inIsrael = inIsrael).parsha
                    shabbos = shabbos.plusDays(7)
                }
                s.record("year", readings)
            }
        }.assertMatches()
    }

    companion object {
        private val BOOLEANS = listOf(false, true)

        private val DATE_ONLY = setOf("getDaysFromTekufasTishrei", "isBirkasHachamah", "isRoshChodesh", "isErevRoshChodesh", "isMacharChodesh", "isShabbosMevorchim", "isYomKippurKatan", "isBehab", "isTaanisBechoros", "isAseresYemeiTeshuvah", "getDayOfOmer")
    }
}
