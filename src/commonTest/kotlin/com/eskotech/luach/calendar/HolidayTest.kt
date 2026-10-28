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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Instant

class HolidayTest {

    private class Setting(val inIsrael: Boolean, val mukefChoma: Boolean) {
        override fun toString(): String = "inIsrael=$inIsrael mukefChoma=$mukefChoma"
    }

    private val settings = listOf(
        Setting(inIsrael = false, mukefChoma = false),
        Setting(inIsrael = true, mukefChoma = false),
        Setting(inIsrael = true, mukefChoma = true),
        Setting(inIsrael = false, mukefChoma = true),
    )

    private val melacha = setOf(
        Holiday.PESACH, Holiday.SHAVUOS, Holiday.SUKKOS, Holiday.SHEMINI_ATZERES, Holiday.SIMCHAS_TORAH, Holiday.ROSH_HASHANAH, Holiday.YOM_KIPPUR,
    )

    private val fasts = setOf(
        Holiday.SHIVA_ASAR_BETAMMUZ, Holiday.TISHA_BEAV, Holiday.YOM_KIPPUR, Holiday.TZOM_GEDALIAH, Holiday.ASARAH_BETEVES, Holiday.TAANIS_ESTHER,
    )

    private val eves = setOf(
        Holiday.EREV_PESACH, Holiday.EREV_SHAVUOS, Holiday.EREV_ROSH_HASHANAH, Holiday.EREV_YOM_KIPPUR, Holiday.EREV_SUKKOS,
    )

    private fun purimMonth(d: Int, w: DayOfWeek): Holiday? = when {
        (d == 13 && w != DayOfWeek.FRIDAY && w != DayOfWeek.SATURDAY) || (d == 11 && w == DayOfWeek.THURSDAY) -> Holiday.TAANIS_ESTHER
        d == 14 -> Holiday.PURIM
        d == 15 -> Holiday.SHUSHAN_PURIM
        else -> null
    }

    private fun expectedHoliday(day: ReferenceCalendar.Day, inIsrael: Boolean): Holiday? {
        val d = day.day
        val w = day.dayOfWeek
        val kislevShort = ReferenceCalendar.monthLength(day.year, HebrewMonth.KISLEV) == 29
        val leap = ReferenceCalendar.isLeap(day.year)
        return when (day.month) {
            HebrewMonth.TISHREI -> when {
                d == 1 || d == 2 -> Holiday.ROSH_HASHANAH
                (d == 3 && w != DayOfWeek.SATURDAY) || (d == 4 && w == DayOfWeek.SUNDAY) -> Holiday.TZOM_GEDALIAH
                d == 9 -> Holiday.EREV_YOM_KIPPUR
                d == 10 -> Holiday.YOM_KIPPUR
                d == 14 -> Holiday.EREV_SUKKOS
                d == 15 -> Holiday.SUKKOS
                d == 16 -> if (inIsrael) Holiday.CHOL_HAMOED_SUKKOS else Holiday.SUKKOS
                d in 17..20 -> Holiday.CHOL_HAMOED_SUKKOS
                d == 21 -> Holiday.HOSHANA_RABBAH
                d == 22 -> Holiday.SHEMINI_ATZERES
                d == 23 -> if (inIsrael) Holiday.ISRU_CHAG else Holiday.SIMCHAS_TORAH
                d == 24 && !inIsrael -> Holiday.ISRU_CHAG
                else -> null
            }
            HebrewMonth.KISLEV -> if (d >= 25) Holiday.CHANUKAH else null
            HebrewMonth.TEVET -> when {
                d == 1 || d == 2 || (d == 3 && kislevShort) -> Holiday.CHANUKAH
                d == 10 -> Holiday.ASARAH_BETEVES
                else -> null
            }
            HebrewMonth.SHEVAT -> if (d == 15) Holiday.TU_BISHVAT else null
            HebrewMonth.ADAR -> if (leap) {
                when (d) {
                    14 -> Holiday.PURIM_KATAN
                    15 -> Holiday.SHUSHAN_PURIM_KATAN
                    else -> null
                }
            } else {
                purimMonth(d, w)
            }
            HebrewMonth.ADAR_II -> purimMonth(d, w)
            HebrewMonth.NISAN -> when {
                d == 14 -> Holiday.EREV_PESACH
                d == 15 || d == 21 -> Holiday.PESACH
                d == 16 -> if (inIsrael) Holiday.CHOL_HAMOED_PESACH else Holiday.PESACH
                d in 17..20 -> Holiday.CHOL_HAMOED_PESACH
                d == 22 -> if (inIsrael) Holiday.ISRU_CHAG else Holiday.PESACH
                d == 23 && !inIsrael -> Holiday.ISRU_CHAG
                else -> null
            }
            HebrewMonth.IYAR -> when (d) {
                14 -> Holiday.PESACH_SHENI
                18 -> Holiday.LAG_BAOMER
                else -> null
            }
            HebrewMonth.SIVAN -> when {
                d == 5 -> Holiday.EREV_SHAVUOS
                d == 6 -> Holiday.SHAVUOS
                d == 7 -> if (inIsrael) Holiday.ISRU_CHAG else Holiday.SHAVUOS
                d == 8 && !inIsrael -> Holiday.ISRU_CHAG
                else -> null
            }
            HebrewMonth.TAMMUZ -> if ((d == 17 && w != DayOfWeek.SATURDAY) || (d == 18 && w == DayOfWeek.SUNDAY)) Holiday.SHIVA_ASAR_BETAMMUZ else null
            HebrewMonth.AV -> when {
                (d == 9 && w != DayOfWeek.SATURDAY) || (d == 10 && w == DayOfWeek.SUNDAY) -> Holiday.TISHA_BEAV
                d == 15 -> Holiday.TU_BEAV
                else -> null
            }
            HebrewMonth.ELUL -> if (d == 29) Holiday.EREV_ROSH_HASHANAH else null
            else -> null
        }
    }

    private fun expectedBehab(day: ReferenceCalendar.Day): Boolean {
        if (day.month != HebrewMonth.CHESHVAN && day.month != HebrewMonth.IYAR) {
            return false
        }
        val firstShabbos = 2 + (DayOfWeek.SATURDAY.ordinal - ReferenceCalendar.weekdayOf(day, 2).ordinal + 7) % 7
        return day.day == firstShabbos + 2 || day.day == firstShabbos + 5 || day.day == firstShabbos + 9
    }

    private fun hebrewDay(day: ReferenceCalendar.Day, setting: Setting): HebrewDay =
        HebrewDay(day.date, inIsrael = setting.inIsrael, mukefChoma = setting.mukefChoma)

    @Test
    fun holidayEveryDay() {
        val mismatches = Mismatches()
        for (day in ReferenceCalendar.allDays()) {
            for (setting in settings) {
                val expected = expectedHoliday(day, setting.inIsrael)
                val actual = hebrewDay(day, setting).holiday
                mismatches.check(actual == expected) { "$day $setting: holiday $actual expected $expected" }
            }
        }
        mismatches.assertNone("holiday")
    }

    @Test
    fun holidayPropertiesMatchTheSets() {
        for (holiday in Holiday.entries) {
            assertEquals(holiday in melacha, holiday.asurBemelacha, "$holiday asurBemelacha")
            assertEquals(holiday in fasts, holiday.isTaanis, "$holiday isTaanis")
            assertEquals(holiday in eves, holiday.isErev, "$holiday isErev")
        }
    }

    @Test
    fun derivedFlagsEveryDay() {
        val mismatches = Mismatches()
        for (day in ReferenceCalendar.allDays()) {
            val d = day.day
            val w = day.dayOfWeek
            val m = day.month
            val kislevShort = ReferenceCalendar.monthLength(day.year, HebrewMonth.KISLEV) == 29
            for (setting in settings) {
                val k = hebrewDay(day, setting)
                val holiday = expectedHoliday(day, setting.inIsrael)
                val erev = holiday in eves || holiday == Holiday.HOSHANA_RABBAH || (m == HebrewMonth.NISAN && d == 20)
                val yomTov = holiday != null && holiday !in eves && holiday != Holiday.ISRU_CHAG && (holiday !in fasts || holiday == Holiday.YOM_KIPPUR)
                val erevSheni = (m == HebrewMonth.TISHREI && d == 1) || (!setting.inIsrael && (
                    (m == HebrewMonth.NISAN && (d == 15 || d == 21)) || (m == HebrewMonth.TISHREI && (d == 15 || d == 22)) || (m == HebrewMonth.SIVAN && d == 6)))
                val restricted = holiday in melacha
                fun check(name: String, actual: Any?, expected: Any?) {
                    mismatches.check(actual == expected) { "$day $setting: $name $actual expected $expected" }
                }
                check("isYomTov", k.isYomTov, yomTov)
                check("isErevYomTov", k.isErevYomTov, erev)
                check("isErevYomTovSheni", k.isErevYomTovSheni, erevSheni)
                check("hasHadlakasNeiros", k.hasHadlakasNeiros, w == DayOfWeek.FRIDAY || erev || erevSheni)
                check("isYomTovAsurBemelacha", k.isYomTovAsurBemelacha, restricted)
                check("isAsurBemelacha", k.isAsurBemelacha, w == DayOfWeek.SATURDAY || restricted)
                check("isTaanis", k.isTaanis, holiday in fasts)
                check("isTaanisBechoros", k.isTaanisBechoros, m == HebrewMonth.NISAN && ((d == 14 && w != DayOfWeek.SATURDAY) || (d == 12 && w == DayOfWeek.THURSDAY)))
                check("isIsruChag", k.isIsruChag, holiday == Holiday.ISRU_CHAG)
                check("isAseresYemeiTeshuvah", k.isAseresYemeiTeshuvah, m == HebrewMonth.TISHREI && d <= 10)
                check("isPesach", k.isPesach, holiday == Holiday.PESACH || holiday == Holiday.CHOL_HAMOED_PESACH)
                check("isCholHamoedPesach", k.isCholHamoedPesach, holiday == Holiday.CHOL_HAMOED_PESACH)
                check("isShavuos", k.isShavuos, holiday == Holiday.SHAVUOS)
                check("isRoshHashanah", k.isRoshHashanah, holiday == Holiday.ROSH_HASHANAH)
                check("isYomKippur", k.isYomKippur, holiday == Holiday.YOM_KIPPUR)
                check("isSukkos", k.isSukkos, holiday == Holiday.SUKKOS || holiday == Holiday.CHOL_HAMOED_SUKKOS || holiday == Holiday.HOSHANA_RABBAH)
                check("isHoshanaRabbah", k.isHoshanaRabbah, holiday == Holiday.HOSHANA_RABBAH)
                check("isShminiAtzeres", k.isShminiAtzeres, holiday == Holiday.SHEMINI_ATZERES)
                check("isSimchasTorah", k.isSimchasTorah, holiday == Holiday.SIMCHAS_TORAH)
                check("isCholHamoedSukkos", k.isCholHamoedSukkos, holiday == Holiday.CHOL_HAMOED_SUKKOS || holiday == Holiday.HOSHANA_RABBAH)
                check("isCholHamoed", k.isCholHamoed, holiday == Holiday.CHOL_HAMOED_PESACH || holiday == Holiday.CHOL_HAMOED_SUKKOS || holiday == Holiday.HOSHANA_RABBAH)
                check("isTishaBeav", k.isTishaBeav, holiday == Holiday.TISHA_BEAV)
                check("isChanukah", k.isChanukah, holiday == Holiday.CHANUKAH)
                val chanukahDay = when {
                    holiday != Holiday.CHANUKAH -> null
                    m == HebrewMonth.KISLEV -> d - 24
                    else -> d + (if (kislevShort) 5 else 6)
                }
                check("dayOfChanukah", k.dayOfChanukah, chanukahDay)
                check("isPurim", k.isPurim, if (setting.mukefChoma) holiday == Holiday.SHUSHAN_PURIM else holiday == Holiday.PURIM)
                check("isRoshChodesh", k.isRoshChodesh, (d == 1 && m != HebrewMonth.TISHREI) || d == 30)
                check("isErevRoshChodesh", k.isErevRoshChodesh, d == 29 && m != HebrewMonth.ELUL)
                check("isMacharChodesh", k.isMacharChodesh, w == DayOfWeek.SATURDAY && (d == 29 || d == 30))
                check("isShabbosMevorchim", k.isShabbosMevorchim, w == DayOfWeek.SATURDAY && d in 23..29 && m != HebrewMonth.ELUL)
                val yomKippurKatanMonth = m != HebrewMonth.ELUL && m != HebrewMonth.TISHREI && m != HebrewMonth.KISLEV && m != HebrewMonth.NISAN
                check(
                    "isYomKippurKatan", k.isYomKippurKatan,
                    yomKippurKatanMonth && ((d == 29 && w != DayOfWeek.FRIDAY && w != DayOfWeek.SATURDAY) || (w == DayOfWeek.THURSDAY && (d == 27 || d == 28))),
                )
                check("isBehab", k.isBehab, expectedBehab(day))
                val omer = when {
                    m == HebrewMonth.NISAN && d >= 16 -> d - 15
                    m == HebrewMonth.IYAR -> d + 15
                    m == HebrewMonth.SIVAN && d < 6 -> d + 44
                    else -> null
                }
                check("sefirasHaomer", k.sefirasHaomer, omer)
            }
        }
        mismatches.assertNone("derived flags")
    }

    @Test
    fun birkasHachamahFallsOnlyOnItsTwentyEightYearDates() {
        assertTrue(HebrewDay(LocalDate(2009, 4, 8)).isBirkasHachamah)
        assertTrue(HebrewDay(LocalDate(1981, 4, 8)).isBirkasHachamah)
        assertTrue(HebrewDay(LocalDate(2037, 4, 8)).isBirkasHachamah)
        assertTrue(HebrewDay(LocalDate(1925, 4, 8)).isBirkasHachamah)
        var date = LocalDate(2009, 1, 1)
        var hits = 0
        while (date.year == 2009) {
            if (HebrewDay(date).isBirkasHachamah) {
                hits++
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        assertEquals(1, hits)
        assertFalse(HebrewDay(LocalDate(2024, 4, 8)).isBirkasHachamah)
    }

    @Test
    fun tekufaCount() {
        assertEquals(47, HebrewDay(LocalDate(2024, 12, 5)).daysFromTekufasTishrei)
        assertEquals(46, HebrewDay(LocalDate(2024, 12, 4)).daysFromTekufasTishrei)
        assertEquals(47, HebrewDay(LocalDate(2023, 12, 6)).daysFromTekufasTishrei)
    }

    @Test
    fun constructorsCopyAndEquality() {
        val fromParts = HebrewDay(HebrewDate.of(5784, HebrewMonth.NISAN, 15), inIsrael = true)
        assertTrue(fromParts.inIsrael)
        assertEquals(LocalDate(2024, 4, 23), fromParts.localDate)
        val fromDate = HebrewDay(LocalDate(2024, 4, 23), inIsrael = true)
        assertEquals(fromParts, fromDate)
        val fromInstant = HebrewDay(Instant.parse("2024-04-23T01:00:00Z"), TimeZone.of("Asia/Jerusalem"), inIsrael = true)
        assertEquals(fromParts, fromInstant)
        val diaspora = HebrewDay(LocalDate(2024, 4, 23))
        assertFalse(diaspora.inIsrael)
        assertNotEquals(fromParts, diaspora)
        val copy = fromParts.copy(mukefChoma = true)
        assertFalse(fromParts.mukefChoma)
        assertTrue(copy.mukefChoma)
        assertEquals(Holiday.CHOL_HAMOED_PESACH, HebrewDay(HebrewDate.of(5784, HebrewMonth.NISAN, 16), inIsrael = true).holiday)
        assertEquals(Holiday.PESACH, HebrewDay(HebrewDate.of(5784, HebrewMonth.NISAN, 16)).holiday)
        assertNull(HebrewDay(HebrewDate.of(5784, HebrewMonth.CHESHVAN, 5)).holiday)
        assertEquals(LocalDate(2024, 4, 24), fromParts.plusDays(1).localDate)
    }

    @Test
    fun rememberedHolidays() {
        val day = HebrewDay(LocalDate(2023, 9, 16))
        assertTrue(day.isRoshHashanah)
        assertTrue(day.plusDays(1).isRoshHashanah)
        assertFalse(day.plusDays(2).isRoshHashanah)
        assertTrue(day.plusDays(2).isTaanis)
        val purim = HebrewDay(HebrewDate.of(5783, HebrewMonth.ADAR, 14))
        assertTrue(purim.isPurim)
        assertFalse(purim.copy(mukefChoma = true).isPurim)
        assertTrue(purim.plusDays(1).copy(mukefChoma = true).isPurim)
        assertFalse(purim.plusDays(1).isPurim)
        assertFalse(HebrewDay(HebrewDate.of(5784, HebrewMonth.ADAR, 14)).isPurim)
        assertTrue(HebrewDay(HebrewDate.of(5784, HebrewMonth.TISHREI, 10)).isAsurBemelacha)
        assertFalse(HebrewDay(HebrewDate.of(5784, HebrewMonth.CHESHVAN, 5)).isAsurBemelacha)
    }

    @Test
    fun today() {
        val zone = TimeZone.UTC
        val plain = HebrewDay.today(zone)
        assertEquals(HebrewDate.today(zone), plain.date)
        assertFalse(plain.inIsrael)
        assertFalse(plain.mukefChoma)
        val flagged = HebrewDay.today(zone, inIsrael = true, mukefChoma = true)
        assertEquals(plain.date, flagged.date)
        assertTrue(flagged.inIsrael && flagged.mukefChoma)
    }
}
