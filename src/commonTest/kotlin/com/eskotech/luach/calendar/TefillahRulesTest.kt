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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class TefillahRulesTest {

    private val rules = TefillahRules()

    private val eves = setOf(Holiday.EREV_PESACH, Holiday.EREV_SHAVUOS, Holiday.EREV_ROSH_HASHANAH, Holiday.EREV_YOM_KIPPUR, Holiday.EREV_SUKKOS)

    private val fasts = setOf(Holiday.SHIVA_ASAR_BETAMMUZ, Holiday.TISHA_BEAV, Holiday.YOM_KIPPUR, Holiday.TZOM_GEDALIAH, Holiday.ASARAH_BETEVES, Holiday.TAANIS_ESTHER)

    private val yaalehVeyavo = setOf(
        Holiday.PESACH, Holiday.CHOL_HAMOED_PESACH, Holiday.SHAVUOS, Holiday.ROSH_HASHANAH, Holiday.YOM_KIPPUR, Holiday.SUKKOS,
        Holiday.CHOL_HAMOED_SUKKOS, Holiday.HOSHANA_RABBAH, Holiday.SHEMINI_ATZERES, Holiday.SIMCHAS_TORAH,
    )

    private fun holidayOf(day: ReferenceCalendar.Day, inIsrael: Boolean): Holiday? = HebrewDay(day.date, inIsrael = inIsrael).holiday

    private fun roshChodesh(day: ReferenceCalendar.Day): Boolean = (day.day == 1 && day.month != HebrewMonth.TISHREI) || day.day == 30

    private fun tachanunShacharis(day: ReferenceCalendar.Day, inIsrael: Boolean): Boolean {
        val holiday = holidayOf(day, inIsrael)
        val d = day.day
        val m = day.month
        val yomTov = holiday != null && holiday !in eves && holiday != Holiday.ISRU_CHAG && (holiday !in fasts || holiday == Holiday.YOM_KIPPUR)
        val omitted = day.dayOfWeek == DayOfWeek.SATURDAY || m == HebrewMonth.NISAN ||
            (m == HebrewMonth.TISHREI && d in 9..21) || (m == HebrewMonth.SIVAN && d <= 12) ||
            yomTov || holiday == Holiday.EREV_ROSH_HASHANAH || holiday == Holiday.TISHA_BEAV || holiday == Holiday.ISRU_CHAG || roshChodesh(day)
        return !omitted
    }

    private fun veseinTalUmatarNominalStart(year: Int): LocalDate {
        val civilYear = ReferenceCalendar.roshHashanah(year).year
        val nextIsLeap = (civilYear + 1) % 4 == 0 && ((civilYear + 1) % 100 != 0 || (civilYear + 1) % 400 == 0)
        return LocalDate(civilYear, 12, if (nextIsLeap) 6 else 5)
    }

    private fun veseinTalUmatarStart(year: Int): LocalDate {
        val nominal = veseinTalUmatarNominalStart(year)
        return if (nominal.dayOfWeek == DayOfWeek.SATURDAY) nominal.plus(1, DateTimeUnit.DAY) else nominal
    }

    @Test
    fun defaultRulesEveryDay() {
        val mismatches = Mismatches()
        val days = ReferenceCalendar.allDays().toList()
        for (inIsrael in listOf(false, true)) {
            for ((position, day) in days.withIndex()) {
                if (position == days.lastIndex) {
                    break
                }
                val tomorrow = days[position + 1]
                val k = HebrewDay(day.date, inIsrael = inIsrael)
                val d = day.day
                val m = day.month
                val holiday = holidayOf(day, inIsrael)
                val chanukah = holiday == Holiday.CHANUKAH
                fun check(name: String, actual: Any?, expected: Any?) {
                    mismatches.check(actual == expected) { "$day inIsrael=$inIsrael: $name $actual expected $expected" }
                }
                val shacharis = tachanunShacharis(day, inIsrael)
                check("tachanunAtShacharis", rules.tachanunAtShacharis(k), shacharis)
                val tomorrowHoliday = holidayOf(tomorrow, inIsrael)
                val minchaOmitted = day.dayOfWeek == DayOfWeek.FRIDAY || !shacharis ||
                    (!tachanunShacharis(tomorrow, inIsrael) && tomorrowHoliday != Holiday.EREV_ROSH_HASHANAH &&
                        tomorrowHoliday != Holiday.EREV_YOM_KIPPUR && tomorrowHoliday != Holiday.PESACH_SHENI) ||
                    tomorrowHoliday == Holiday.LAG_BAOMER
                check("tachanunAtMincha", rules.tachanunAtMincha(k), !minchaOmitted)

                val start = if (inIsrael) ReferenceCalendar.gregorian(day.year, HebrewMonth.CHESHVAN, 7) else veseinTalUmatarStart(day.year)
                val nominalStart = if (inIsrael) start else veseinTalUmatarNominalStart(day.year)
                val winterMonth = m in setOf(HebrewMonth.CHESHVAN, HebrewMonth.KISLEV, HebrewMonth.TEVET, HebrewMonth.SHEVAT, HebrewMonth.ADAR, HebrewMonth.ADAR_II)
                val talUmatar = (m == HebrewMonth.NISAN && d < 15) || (winterMonth && day.date >= nominalStart)
                check("isVeseinTalUmatarStart", rules.isVeseinTalUmatarStart(k), day.date == start)
                check("veseinTalUmatarStartsTonight", rules.veseinTalUmatarStartsTonight(k), day.date == start.minus(1, DateTimeUnit.DAY))
                check("veseinTalUmatar", rules.veseinTalUmatar(k), talUmatar)
                check("veseinBerachah", rules.veseinBerachah(k), !talUmatar)

                val mashivStart = m == HebrewMonth.TISHREI && d == 22
                val mashivEnd = m == HebrewMonth.NISAN && d == 15
                val mashiv = day.ordinalInYear > ReferenceCalendar.dayOfYear(day.year, HebrewMonth.TISHREI, 22) &&
                    day.ordinalInYear < ReferenceCalendar.dayOfYear(day.year, HebrewMonth.NISAN, 15)
                check("isMashivHaruachStart", rules.isMashivHaruachStart(k), mashivStart)
                check("isMashivHaruachEnd", rules.isMashivHaruachEnd(k), mashivEnd)
                check("mashivHaruach", rules.mashivHaruach(k), mashiv)
                check("moridHatal", rules.moridHatal(k), !mashiv || mashivStart || mashivEnd)

                val hallel = roshChodesh(day) || chanukah ||
                    (m == HebrewMonth.NISAN && d in 15..(if (inIsrael) 21 else 22)) ||
                    (m == HebrewMonth.SIVAN && (d == 6 || (d == 7 && !inIsrael))) ||
                    (m == HebrewMonth.TISHREI && d in 15..(if (inIsrael) 22 else 23))
                val full = hallel && !(roshChodesh(day) && !chanukah) && !(m == HebrewMonth.NISAN && d > (if (inIsrael) 15 else 16))
                check("hallel", rules.hallel(k), if (!hallel) Hallel.NONE else if (full) Hallel.FULL else Hallel.PARTIAL)
                check("alHanissim", rules.alHanissim(k), holiday == Holiday.PURIM || chanukah)
                check("yaalehVeyavo", rules.yaalehVeyavo(k), holiday in yaalehVeyavo || roshChodesh(day))
            }
        }
        mismatches.assertNone("tefillah rules")
    }

    @Test
    fun veseinTalUmatarPublishedStartDates() {
        val expected = mapOf(
            2019 to (LocalDate(2019, 12, 6) to LocalDate(2019, 12, 6)), 2020 to (LocalDate(2020, 12, 5) to LocalDate(2020, 12, 6)),
            2021 to (LocalDate(2021, 12, 5) to LocalDate(2021, 12, 5)), 2022 to (LocalDate(2022, 12, 5) to LocalDate(2022, 12, 5)),
            2023 to (LocalDate(2023, 12, 6) to LocalDate(2023, 12, 6)), 2024 to (LocalDate(2024, 12, 5) to LocalDate(2024, 12, 5)),
            2025 to (LocalDate(2025, 12, 5) to LocalDate(2025, 12, 5)), 2026 to (LocalDate(2026, 12, 5) to LocalDate(2026, 12, 6)),
            2027 to (LocalDate(2027, 12, 6) to LocalDate(2027, 12, 6)),
        )
        for ((year, dates) in expected) {
            val (nominal, start) = dates
            assertTrue(rules.isVeseinTalUmatarStart(HebrewDay(start)), "$year start $start")
            assertTrue(rules.veseinTalUmatarStartsTonight(HebrewDay(start.minus(1, DateTimeUnit.DAY))), "$year night before $start")
            assertFalse(rules.veseinTalUmatar(HebrewDay(nominal.minus(1, DateTimeUnit.DAY))), "$year day before")
            assertTrue(rules.veseinTalUmatar(HebrewDay(nominal)), "$year nominal start")
            var date = LocalDate(year, 11, 1)
            var starts = 0
            var nights = 0
            while (date.year == year) {
                if (rules.isVeseinTalUmatarStart(HebrewDay(date))) starts++
                if (rules.veseinTalUmatarStartsTonight(HebrewDay(date))) nights++
                date = date.plus(1, DateTimeUnit.DAY)
            }
            assertEquals(1, starts, "$year has one start date")
            assertEquals(1, nights, "$year has one starting night")
        }
        val israel = HebrewDay(HebrewDate.of(5786, HebrewMonth.CHESHVAN, 7), inIsrael = true)
        assertTrue(rules.isVeseinTalUmatarStart(israel))
        assertTrue(rules.veseinTalUmatarStartsTonight(HebrewDay(HebrewDate.of(5786, HebrewMonth.CHESHVAN, 6), inIsrael = true)))
        assertFalse(rules.veseinTalUmatar(HebrewDay(HebrewDate.of(5786, HebrewMonth.CHESHVAN, 6), inIsrael = true)))
        assertTrue(rules.veseinTalUmatar(HebrewDay(HebrewDate.of(5786, HebrewMonth.NISAN, 14), inIsrael = true)))
        assertFalse(rules.veseinTalUmatar(HebrewDay(HebrewDate.of(5786, HebrewMonth.NISAN, 15), inIsrael = true)))
    }

    @Test
    fun customs() {
        fun with(customs: TachanunCustoms) = TefillahRules(customs)
        val endOfTishrei = HebrewDay(HebrewDate.of(5786, HebrewMonth.TISHREI, 27))
        assertTrue(rules.tachanunAtShacharis(endOfTishrei))
        assertFalse(with(TachanunCustoms(omitAfterSukkos = true)).tachanunAtShacharis(endOfTishrei))

        val tenthOfSivan = HebrewDay(HebrewDate.of(5786, HebrewMonth.SIVAN, 10))
        assertFalse(rules.tachanunAtShacharis(tenthOfSivan))
        assertTrue(with(TachanunCustoms(omitWeekAfterShavuos = false)).tachanunAtShacharis(tenthOfSivan))
        assertFalse(with(TachanunCustoms(omitWeekAfterShavuos = false)).tachanunAtShacharis(HebrewDay(HebrewDate.of(5786, HebrewMonth.SIVAN, 6))))

        val thirteenthOfSivan = HebrewDay(HebrewDate.of(5785, HebrewMonth.SIVAN, 13))
        assertTrue(rules.tachanunAtShacharis(thirteenthOfSivan))
        assertFalse(with(TachanunCustoms(omit13SivanOutsideIsrael = true)).tachanunAtShacharis(thirteenthOfSivan))
        assertTrue(with(TachanunCustoms(omit13SivanOutsideIsrael = true)).tachanunAtShacharis(thirteenthOfSivan.copy(inIsrael = true)))

        val pesachSheni = HebrewDay(HebrewDate.of(5785, HebrewMonth.IYAR, 14))
        assertFalse(rules.tachanunAtShacharis(pesachSheni))
        assertTrue(with(TachanunCustoms(omitPesachSheni = false)).tachanunAtShacharis(pesachSheni))

        val fifteenthOfIyar = HebrewDay(HebrewDate.of(5785, HebrewMonth.IYAR, 15))
        assertTrue(rules.tachanunAtShacharis(fifteenthOfIyar))
        assertFalse(with(TachanunCustoms(omit15IyarOutsideIsrael = true)).tachanunAtShacharis(fifteenthOfIyar))
        assertTrue(with(TachanunCustoms(omit15IyarOutsideIsrael = true)).tachanunAtShacharis(fifteenthOfIyar.copy(inIsrael = true)))

        val erevLagBaomer = HebrewDay(HebrewDate.of(5785, HebrewMonth.IYAR, 17))
        assertFalse(rules.tachanunAtMincha(erevLagBaomer))
        assertTrue(with(TachanunCustoms(omitMinchaErevLagBaomer = false)).tachanunAtMincha(erevLagBaomer))

        val shivasYemeiHamiluim = HebrewDay(HebrewDate.of(5785, HebrewMonth.ADAR, 24))
        assertTrue(rules.tachanunAtShacharis(shivasYemeiHamiluim))
        assertFalse(with(TachanunCustoms(omitShivasYemeiHamiluim = true)).tachanunAtShacharis(shivasYemeiHamiluim))
        assertFalse(with(TachanunCustoms(omitShivasYemeiHamiluim = true)).tachanunAtShacharis(HebrewDay(HebrewDate.of(5784, HebrewMonth.ADAR_II, 24))))
        assertTrue(with(TachanunCustoms(omitShivasYemeiHamiluim = true)).tachanunAtShacharis(HebrewDay(HebrewDate.of(5784, HebrewMonth.ADAR, 24))))

        val weekOfHod = HebrewDay(HebrewDate.of(5785, HebrewMonth.IYAR, 16))
        assertTrue(rules.tachanunAtShacharis(weekOfHod))
        assertFalse(with(TachanunCustoms(omitWeekOfHod = true)).tachanunAtShacharis(weekOfHod))

        val weekOfPurim = HebrewDay(HebrewDate.of(5785, HebrewMonth.ADAR, 12))
        assertTrue(rules.tachanunAtShacharis(weekOfPurim))
        assertFalse(with(TachanunCustoms(omitWeekOfPurim = true)).tachanunAtShacharis(weekOfPurim))

        val friday = HebrewDay(LocalDate(2025, 5, 9))
        assertTrue(rules.tachanunAtShacharis(friday))
        assertFalse(with(TachanunCustoms(omitFridays = true)).tachanunAtShacharis(friday))
        assertFalse(rules.tachanunAtMincha(friday))

        val sunday = HebrewDay(LocalDate(2025, 5, 11))
        assertTrue(rules.tachanunAtShacharis(sunday))
        assertFalse(with(TachanunCustoms(omitSundays = true)).tachanunAtShacharis(sunday))
        assertTrue(rules.tachanunAtMincha(sunday))
        assertFalse(with(TachanunCustoms(omitMincha = true)).tachanunAtMincha(sunday))

        assertTrue(rules.tachanunAtMincha(HebrewDay(HebrewDate.of(5785, HebrewMonth.ELUL, 28))))
        assertTrue(rules.tachanunAtMincha(HebrewDay(HebrewDate.of(5786, HebrewMonth.TISHREI, 8))))
        val erevRoshChodesh = HebrewDay(HebrewDate.of(5786, HebrewMonth.CHESHVAN, 29))
        assertTrue(rules.tachanunAtShacharis(erevRoshChodesh))
        assertFalse(rules.tachanunAtMincha(erevRoshChodesh))
    }

    @Test
    fun rememberedDays() {
        val summerWeekday = HebrewDay(LocalDate(2023, 8, 21))
        assertTrue(rules.tachanunAtShacharis(summerWeekday))
        assertTrue(rules.tachanunAtMincha(summerWeekday))
        assertFalse(rules.veseinTalUmatar(summerWeekday))
        assertTrue(rules.veseinBerachah(summerWeekday))
        assertFalse(rules.mashivHaruach(summerWeekday))
        assertTrue(rules.moridHatal(summerWeekday))
        assertEquals(Hallel.NONE, rules.hallel(summerWeekday))
        assertFalse(rules.alHanissim(summerWeekday))
        assertFalse(rules.yaalehVeyavo(summerWeekday))

        val firstDayOfSukkos = HebrewDay(LocalDate(2023, 10, 7))
        assertFalse(rules.tachanunAtShacharis(firstDayOfSukkos))
        assertFalse(rules.tachanunAtMincha(firstDayOfSukkos))
        assertTrue(rules.isMashivHaruachStart(firstDayOfSukkos))
        assertFalse(rules.mashivHaruach(firstDayOfSukkos))
        assertTrue(rules.moridHatal(firstDayOfSukkos))
        assertEquals(Hallel.FULL, rules.hallel(firstDayOfSukkos))
        assertTrue(rules.yaalehVeyavo(firstDayOfSukkos))
        assertEquals(TachanunCustoms(), rules.customs)
    }

    @Test
    fun summaryMatchesTheIndividualRules() {
        val variants = listOf(TachanunCustoms(), TachanunCustoms(omitFridays = true, omitMincha = true, omitWeekOfHod = true, omitAfterSukkos = true))
        val mismatches = Mismatches()
        for (day in ReferenceCalendar.allDays()) for (inIsrael in listOf(false, true)) for (customs in variants) {
            val r = TefillahRules(customs)
            val k = HebrewDay(day.date, inIsrael = inIsrael)
            val expected = TefillahSummary(
                tachanunAtShacharis = r.tachanunAtShacharis(k),
                tachanunAtMincha = r.tachanunAtMincha(k),
                veseinTalUmatar = r.veseinTalUmatar(k),
                veseinBerachah = r.veseinBerachah(k),
                isVeseinTalUmatarStart = r.isVeseinTalUmatarStart(k),
                veseinTalUmatarStartsTonight = r.veseinTalUmatarStartsTonight(k),
                mashivHaruach = r.mashivHaruach(k),
                moridHatal = r.moridHatal(k),
                isMashivHaruachStart = r.isMashivHaruachStart(k),
                isMashivHaruachEnd = r.isMashivHaruachEnd(k),
                hallel = r.hallel(k),
                alHanissim = r.alHanissim(k),
                yaalehVeyavo = r.yaalehVeyavo(k),
            )
            mismatches.check(r.all(k) == expected) { "$day inIsrael=$inIsrael $customs: ${r.all(k)} expected $expected" }
        }
        mismatches.assertNone("summary")
        val summary = rules.all(HebrewDay(LocalDate(2026, 4, 2)))
        assertFalse(summary.tachanunAtShacharis)
        assertEquals(Hallel.FULL, summary.hallel)
        assertTrue(summary.yaalehVeyavo)
    }
}
