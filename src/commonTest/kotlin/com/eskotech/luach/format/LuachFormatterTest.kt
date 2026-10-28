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

package com.eskotech.luach.format

import com.eskotech.luach.calendar.BavliDaf
import com.eskotech.luach.calendar.BavliTractate
import com.eskotech.luach.calendar.HebrewDate
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.HebrewMonth
import com.eskotech.luach.calendar.Holiday
import com.eskotech.luach.calendar.Parsha
import com.eskotech.luach.calendar.YerushalmiDaf
import com.eskotech.luach.calendar.YerushalmiTractate
import com.eskotech.luach.geo.Location
import com.eskotech.luach.zmanim.Zman
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class LuachFormatterTest {

    private val english = LuachFormatter()

    private val hebrew = LuachFormatter(hebrew = true)

    @Test
    fun hebrewNumbers() {
        val expected = mapOf(
            0 to "אפס", 1 to "א׳", 2 to "ב׳", 5 to "ה׳", 9 to "ט׳", 10 to "י׳", 11 to "י״א", 14 to "י״ד", 15 to "ט״ו", 16 to "ט״ז",
            17 to "י״ז", 20 to "כ׳", 21 to "כ״א", 30 to "ל׳", 33 to "ל״ג", 49 to "מ״ט", 50 to "נ׳", 99 to "צ״ט", 100 to "ק׳",
            101 to "ק״א", 110 to "ק״י", 115 to "קט״ו", 116 to "קט״ז", 200 to "ר׳", 300 to "ש׳", 400 to "ת׳", 500 to "ת״ק",
            613 to "תרי״ג", 770 to "תש״ע", 784 to "תשפ״ד", 1000 to "א׳ אלפים", 5000 to "ה׳ אלפים", 5700 to "ת״ש",
            5760 to "תש״ס", 5784 to "תשפ״ד", 5785 to "תשפ״ה", 5786 to "תשפ״ו", 5787 to "תשפ״ז", 9999 to "תתקצ״ט",
        )
        for ((number, text) in expected) {
            assertEquals(text, hebrew.formatNumber(number), "$number")
        }
        val withThousands = hebrew.copy(thousands = true)
        assertEquals("ה׳ תשפ״ד", withThousands.formatNumber(5784))
        assertEquals("ה׳ אלפים", withThousands.formatNumber(5000))
        assertEquals("ט״ו", withThousands.formatNumber(15))
        val plain = hebrew.copy(quoteMarks = false)
        assertEquals("תשפד", plain.formatNumber(5784))
        assertEquals("טו", plain.formatNumber(15))
        assertEquals("י", plain.formatNumber(10))
        assertEquals("א אלפים", plain.formatNumber(1000))
        val finals = hebrew.copy(finalLetters = true)
        assertEquals("תש״ף", finals.formatNumber(5780))
        assertEquals("תש״ם", finals.formatNumber(5740))
        assertEquals("תש״ן", finals.formatNumber(5750))
        assertEquals("תש״ץ", finals.formatNumber(5790))
        assertEquals("ק״ך", finals.formatNumber(120))
        assertEquals("מ׳", finals.formatNumber(40))
        assertEquals("כ׳", finals.formatNumber(20))
        assertEquals("תשפ״ד", finals.formatNumber(5784))
        assertFailsWith<IllegalArgumentException> { hebrew.formatNumber(-1) }
        assertFailsWith<IllegalArgumentException> { hebrew.formatNumber(10000) }
    }

    @Test
    fun datesAndMonths() {
        assertEquals("11 Cheshvan, 5787", english.formatDate(HebrewDate.of(5787, HebrewMonth.CHESHVAN, 11)))
        assertEquals("י״א חשון תשפ״ז", hebrew.formatDate(HebrewDate.of(5787, HebrewMonth.CHESHVAN, 11)))
        assertEquals("1 Adar I, 5784", english.formatDate(HebrewDate.of(5784, HebrewMonth.ADAR, 1)))
        assertEquals("א׳ אדר א׳ תשפ״ד", hebrew.formatDate(HebrewDate.of(5784, HebrewMonth.ADAR, 1)))
        assertEquals("14 Adar II, 5784", english.formatDate(HebrewDate.of(5784, HebrewMonth.ADAR_II, 14)))
        assertEquals("י״ד אדר ב׳ תשפ״ד", hebrew.formatDate(HebrewDate.of(5784, HebrewMonth.ADAR_II, 14)))
        assertEquals("14 Adar, 5783", english.formatDate(HebrewDate.of(5783, HebrewMonth.ADAR, 14)))
        assertEquals("י״ד אדר תשפ״ג", hebrew.formatDate(HebrewDate.of(5783, HebrewMonth.ADAR, 14)))
        assertEquals("1 Tishrei, 5785", english.formatDate(HebrewDate.of(5785, HebrewMonth.TISHREI, 1)))
        assertEquals("א׳ תשרי תשפ״ה", hebrew.formatDate(HebrewDate.of(5785, HebrewMonth.TISHREI, 1)))
        assertEquals("ט״ו שבט תשפ״ז", hebrew.formatDate(HebrewDate.of(5787, HebrewMonth.SHEVAT, 15)))
        assertEquals("ל׳ ניסן תשפ״ו", hebrew.formatDate(HebrewDate.of(5786, HebrewMonth.NISAN, 30)))

        val englishMonths = listOf("Nissan", "Iyar", "Sivan", "Tammuz", "Av", "Elul", "Tishrei", "Cheshvan", "Kislev", "Teves", "Shevat", "Adar")
        val hebrewMonths = listOf("ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון", "כסלו", "טבת", "שבט", "אדר")
        for (month in 1..12) {
            assertEquals(englishMonths[month - 1], english.formatMonth(HebrewDate.of(5786, month, 1)), "month $month")
            assertEquals(hebrewMonths[month - 1], hebrew.formatMonth(HebrewDate.of(5786, month, 1)), "month $month")
        }
        assertEquals("Adar I", english.formatMonth(HebrewDate.of(5787, HebrewMonth.ADAR, 1)))
        assertEquals("Adar II", english.formatMonth(HebrewDate.of(5787, HebrewMonth.ADAR_II, 1)))
        assertEquals("אדר א׳", hebrew.formatMonth(HebrewDate.of(5787, HebrewMonth.ADAR, 1)))
        assertEquals("אדר ב׳", hebrew.formatMonth(HebrewDate.of(5787, HebrewMonth.ADAR_II, 1)))
        assertEquals("אדר א", hebrew.copy(quoteMarks = false).formatMonth(HebrewDate.of(5787, HebrewMonth.ADAR, 1)))

        val custom = english.copy(monthNames = english.monthNames + (HebrewMonth.CHESHVAN to "Marcheshvan"), adarInLeapYear = "Adar Rishon")
        assertEquals("11 Marcheshvan, 5787", custom.formatDate(HebrewDate.of(5787, HebrewMonth.CHESHVAN, 11)))
        assertEquals("Adar Rishon", custom.formatMonth(HebrewMonth.ADAR, meuberes = true))
    }

    @Test
    fun weekdays() {
        val thursday = HebrewDate.from(LocalDate(2026, 10, 22))
        val shabbos = HebrewDate.from(LocalDate(2026, 10, 24))
        val sunday = HebrewDate.from(LocalDate(2026, 10, 25))
        val shortEnglish = english.copy(abbreviatedWeekdays = true)
        val shortHebrew = hebrew.copy(abbreviatedWeekdays = true)
        assertEquals("Thursday", english.formatWeekday(thursday))
        assertEquals("Thu", shortEnglish.formatWeekday(thursday))
        assertEquals("חמישי", hebrew.formatWeekday(thursday))
        assertEquals("ה׳", shortHebrew.formatWeekday(thursday))
        assertEquals("Shabbos", english.formatWeekday(shabbos))
        assertEquals("Sha", shortEnglish.formatWeekday(shabbos))
        assertEquals("שבת", hebrew.formatWeekday(shabbos))
        assertEquals("ש׳", shortHebrew.formatWeekday(shabbos))
        assertEquals("Sunday", english.formatWeekday(sunday))
        assertEquals("Sun", shortEnglish.formatWeekday(sunday))
        assertEquals("ראשון", hebrew.formatWeekday(sunday))
        assertEquals("א׳", shortHebrew.formatWeekday(sunday))
        assertEquals("Shabbat", english.copy(weekdayNames = english.weekdayNames + (DayOfWeek.SATURDAY to "Shabbat")).formatWeekday(shabbos))
    }

    @Test
    fun holidayNames() {
        fun names(date: LocalDate): Pair<String, String> {
            val day = HebrewDay(date)
            return english.formatHoliday(day) to hebrew.formatHoliday(day)
        }
        assertEquals("Pesach" to "פסח", names(LocalDate(2024, 4, 23)))
        assertEquals("Chol Hamoed Pesach" to "חול המועד פסח", names(LocalDate(2024, 4, 25)))
        assertEquals("Chanukah 3" to "ג׳ חנוכה", names(LocalDate(2024, 12, 28)))
        assertEquals("Chanukah 7" to "ז׳ חנוכה", names(LocalDate(2025, 1, 1)))
        assertEquals("Chanukah 8" to "ח׳ חנוכה", names(LocalDate(2025, 1, 2)))
        assertEquals("Purim" to "פורים", names(LocalDate(2025, 3, 14)))
        assertEquals("Shushan Purim" to "שושן פורים", names(LocalDate(2025, 3, 15)))
        assertEquals("Yom Kippur" to "יום כיפור", names(LocalDate(2025, 10, 2)))
        assertEquals("Tishah B'Av" to "תשעה באב", names(LocalDate(2026, 7, 23)))
        assertEquals("Rosh Hashanah" to "ראש השנה", names(LocalDate(2025, 9, 23)))
        assertEquals("Tzom Gedaliah" to "צום גדליה", names(LocalDate(2025, 9, 25)))
        assertEquals("Shavuos" to "שבועות", names(LocalDate(2026, 5, 22)))
        assertEquals("Lag BaOmer" to "ל״ג בעומר", names(LocalDate(2026, 5, 5)))
        assertEquals("Pesach Sheini" to "פסח שני", names(LocalDate(2026, 5, 1)))
        assertEquals("Tu B'Shevat" to "ט״ו בשבט", names(LocalDate(2027, 1, 23)))
        assertEquals("Isru Chag" to "אסרו חג", names(LocalDate(2026, 4, 10)))
        assertEquals("Erev Pesach" to "ערב פסח", names(LocalDate(2026, 4, 1)))
        assertEquals("Hoshana Rabbah" to "הושענא רבה", names(LocalDate(2025, 10, 13)))
        assertEquals("Shemini Atzeres" to "שמיני עצרת", names(LocalDate(2025, 10, 14)))
        assertEquals("Simchas Torah" to "שמחת תורה", names(LocalDate(2025, 10, 15)))
        assertEquals("" to "", names(LocalDate(2026, 4, 22)))
        assertEquals("" to "", names(LocalDate(2026, 10, 22)))
        assertEquals("Purim Katan" to "פורים קטן", names(LocalDate(2024, 2, 23)))
        for (holiday in Holiday.entries) {
            assertEquals(true, english.holidayNames.containsKey(holiday) && Transliterations.HOLIDAYS.getValue(holiday).isNotBlank(), "$holiday")
        }
    }

    @Test
    fun roshChodesh() {
        fun names(year: Int, month: HebrewMonth, day: Int): Pair<String, String> {
            val hebrewDay = HebrewDay(HebrewDate.of(year, month, day))
            return english.formatRoshChodesh(hebrewDay) to hebrew.formatRoshChodesh(hebrewDay)
        }
        assertEquals("Rosh Chodesh Kislev" to "ראש חודש כסלו", names(5785, HebrewMonth.CHESHVAN, 30))
        assertEquals("Rosh Chodesh Kislev" to "ראש חודש כסלו", names(5785, HebrewMonth.KISLEV, 1))
        assertEquals("Rosh Chodesh Adar II" to "ראש חודש אדר ב׳", names(5784, HebrewMonth.ADAR_II, 1))
        assertEquals("Rosh Chodesh Adar II" to "ראש חודש אדר ב׳", names(5784, HebrewMonth.ADAR, 30))
        assertEquals("Rosh Chodesh Adar I" to "ראש חודש אדר א׳", names(5784, HebrewMonth.ADAR, 1))
        assertEquals("Rosh Chodesh Adar I" to "ראש חודש אדר א׳", names(5784, HebrewMonth.SHEVAT, 30))
        assertEquals("Rosh Chodesh Adar" to "ראש חודש אדר", names(5783, HebrewMonth.ADAR, 1))
        assertEquals("Rosh Chodesh Adar" to "ראש חודש אדר", names(5783, HebrewMonth.SHEVAT, 30))
        assertEquals("Rosh Chodesh Iyar" to "ראש חודש אייר", names(5786, HebrewMonth.NISAN, 30))
        assertEquals("Rosh Chodesh Nissan" to "ראש חודש ניסן", names(5786, HebrewMonth.NISAN, 1))
        assertEquals("" to "", names(5786, HebrewMonth.TISHREI, 1))
        assertEquals("" to "", names(5786, HebrewMonth.KISLEV, 2))
    }

    @Test
    fun omer() {
        assertEquals("Omer 1", english.formatOmer(HebrewDay(LocalDate(2026, 4, 3))))
        assertEquals("א׳ בעומר", hebrew.formatOmer(HebrewDay(LocalDate(2026, 4, 3))))
        assertEquals("Lag BaOmer", english.formatOmer(HebrewDay(LocalDate(2026, 5, 5))))
        assertEquals("ל״ג בעומר", hebrew.formatOmer(HebrewDay(LocalDate(2026, 5, 5))))
        assertEquals("Omer 49", english.formatOmer(HebrewDay(LocalDate(2026, 5, 21))))
        assertEquals("מ״ט בעומר", hebrew.formatOmer(HebrewDay(LocalDate(2026, 5, 21))))
        assertEquals("", english.formatOmer(HebrewDay(LocalDate(2026, 4, 2))))
        assertEquals("", hebrew.formatOmer(HebrewDay(LocalDate(2026, 5, 22))))
        assertEquals("א׳ לעומר", hebrew.copy(omerPrefix = "ל").formatOmer(HebrewDay(LocalDate(2026, 4, 3))))
        assertEquals("Day 7 of the Omer", english.copy(omerFormat = "Day {day} of the Omer").formatOmer(HebrewDay(LocalDate(2026, 4, 9))))
    }

    @Test
    fun yearSignature() {
        val expected = mapOf(
            5779 to "בשז", 5780 to "בשה", 5781 to "זחא", 5782 to "גכז", 5783 to "בשה", 5784 to "זחג",
            5785 to "השא", 5786 to "גכה", 5787 to "זשה", 5788 to "זשג", 5789 to "הכז", 5790 to "בחה",
        )
        for ((year, text) in expected) {
            assertEquals(text, hebrew.formatYearSignature(year), "$year")
        }
    }

    @Test
    fun parshaAndSpecialShabbos() {
        fun names(date: LocalDate): List<String> {
            val day = HebrewDay(date)
            return listOf(english.formatParsha(day), english.formatSpecialShabbos(day), hebrew.formatParsha(day), hebrew.formatSpecialShabbos(day))
        }
        assertEquals(listOf("Vayakhel", "Shekalim", "ויקהל", "שקלים"), names(LocalDate(2024, 3, 9)))
        assertEquals(listOf("Vayikra", "Zachor", "ויקרא", "זכור"), names(LocalDate(2024, 3, 23)))
        assertEquals(listOf("Tzav", "Parah", "צו", "פרה"), names(LocalDate(2024, 3, 30)))
        assertEquals(listOf("Shemini", "Hachodesh", "שמיני", "החודש"), names(LocalDate(2024, 4, 6)))
        assertEquals(listOf("Metzora", "Hagadol", "מצורע", "הגדול"), names(LocalDate(2024, 4, 20)))
        assertEquals(listOf("Devarim", "Chazon", "דברים", "חזון"), names(LocalDate(2024, 8, 10)))
        assertEquals(listOf("Va'eschanan", "Nachamu", "ואתחנן", "נחמו"), names(LocalDate(2024, 8, 17)))
        assertEquals(listOf("Ha'azinu", "Shuvah", "האזינו", "שובה"), names(LocalDate(2024, 10, 5)))
        assertEquals(listOf("Beshalach", "Shirah", "בשלח", "שירה"), names(LocalDate(2024, 1, 27)))
        assertEquals(listOf("Nitzavim-Vayeilech", "", "ניצבים-וילך", ""), names(LocalDate(2024, 9, 28)))
        assertEquals(listOf("", "", "", ""), names(LocalDate(2026, 10, 22)))
        assertEquals(listOf("", "", "", ""), names(LocalDate(2024, 4, 27)))
        val custom = english.copy(parshaNames = english.parshaNames + (Parsha.BERESHIT to "Bereishit"))
        assertEquals("Bereishit", custom.formatParsha(HebrewDay(LocalDate(2023, 10, 14))))
        for (parsha in Parsha.entries) {
            assertEquals(true, Transliterations.PARSHIOS.getValue(parsha).isNotBlank() && HebrewNames.PARSHIOS.getValue(parsha).isNotBlank(), "$parsha")
        }
    }

    @Test
    fun dafFormats() {
        assertEquals("Berachos 2", english.formatDaf(BavliDaf(BavliTractate.BERACHOS, 2)))
        assertEquals("ברכות ב׳", hebrew.formatDaf(BavliDaf(BavliTractate.BERACHOS, 2)))
        assertEquals("Shekalim 13", english.formatDaf(BavliDaf(BavliTractate.SHEKALIM, 13)))
        assertEquals("שקלים י״ג", hebrew.formatDaf(BavliDaf(BavliTractate.SHEKALIM, 13)))
        assertEquals("Bava Basra 176", english.formatDaf(BavliDaf(BavliTractate.BAVA_BASRA, 176)))
        assertEquals("בבא בתרא קע״ו", hebrew.formatDaf(BavliDaf(BavliTractate.BAVA_BASRA, 176)))
        assertEquals("Berachos 1", english.formatDaf(YerushalmiDaf(YerushalmiTractate.BERACHOS, 1)))
        assertEquals("ברכות א׳", hebrew.formatDaf(YerushalmiDaf(YerushalmiTractate.BERACHOS, 1)))
        assertEquals("Pe'ah 37", english.formatDaf(YerushalmiDaf(YerushalmiTractate.PEAH, 37)))
        assertEquals("פיאה ל״ז", hebrew.formatDaf(YerushalmiDaf(YerushalmiTractate.PEAH, 37)))
        assertEquals("No Daf Today", english.formatDaf(null as YerushalmiDaf?))
        assertEquals("אין דף היום", hebrew.formatDaf(null as YerushalmiDaf?))
        assertEquals("Shekalim 2", english.formatDaf(BavliDaf(BavliTractate.SHEKALIM, 2)))
        for (tractate in BavliTractate.entries) {
            assertEquals(true, Transliterations.BAVLI.getValue(tractate).isNotBlank(), "$tractate")
        }
        for (tractate in YerushalmiTractate.entries) {
            assertEquals(true, Transliterations.YERUSHALMI.getValue(tractate).isNotBlank(), "$tractate")
        }
    }

    @Test
    fun times() {
        val zone = TimeZone.of("America/New_York")
        val morning = Instant.parse("2026-10-13T10:05:09Z")
        val evening = Instant.parse("2026-10-13T22:30:59Z")
        val midnight = Instant.parse("2026-10-13T04:00:00Z")
        val noon = Instant.parse("2026-10-13T16:00:00Z")
        assertEquals("06:05", english.formatTime(morning, zone))
        assertEquals("06:05:09", english.formatTime(morning, zone, seconds = true))
        assertEquals("18:30", english.formatTime(evening, zone))
        assertEquals("00:00", english.formatTime(midnight, zone))
        assertEquals("12:00", english.formatTime(noon, zone))
        assertEquals("", english.formatTime(null, zone))
        assertEquals("18:30", hebrew.formatTime(evening, zone))
        val twelve = LuachFormatter(twelveHourClock = true)
        assertEquals("6:05 AM", twelve.formatTime(morning, zone))
        assertEquals("6:30 PM", twelve.formatTime(evening, zone))
        assertEquals("12:00 AM", twelve.formatTime(midnight, zone))
        assertEquals("12:00 PM", twelve.formatTime(noon, zone))
        assertEquals("6:30:59 PM", twelve.formatTime(evening, zone, seconds = true))
        assertEquals("6:30", LuachFormatter(twelveHourClock = true, amPmNames = "" to "").formatTime(evening, zone))
        assertEquals("6:30 אחה״צ", LuachFormatter(hebrew = true, twelveHourClock = true, amPmNames = "לפנה״צ" to "אחה״צ").formatTime(evening, zone))
        assertEquals("06:05", english.formatTime(morning, Location(40.0, -74.0, zone)))
        assertEquals("", twelve.formatTime(null, Location(40.0, -74.0, zone)))
    }

    @Test
    fun zmanNames() {
        assertEquals("Alos Hashachar", english.formatZman(Zman.ALOS))
        assertEquals("עלות השחר", hebrew.formatZman(Zman.ALOS))
        assertEquals("Sof Zman Krias Shema", english.formatZman(Zman.SOF_ZMAN_SHEMA))
        assertEquals("הדלקת נרות", hebrew.formatZman(Zman.HADLAKAS_NEIROS))
        assertEquals("Dawn", english.copy(zmanNames = english.zmanNames + (Zman.ALOS to "Dawn")).formatZman(Zman.ALOS))
        for (zman in Zman.entries) {
            assertEquals(true, english.zmanNames.containsKey(zman) && Transliterations.ZMANIM.getValue(zman).isNotBlank() && hebrew.formatZman(zman).isNotBlank(), "$zman")
        }
    }
}
