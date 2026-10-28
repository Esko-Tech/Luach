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

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

enum class BavliTractate(val hebrewName: String, val lastPage: Int, val firstPage: Int = 2) {
    BERACHOS("ברכות", 64),
    SHABBOS("שבת", 157),
    ERUVIN("עירובין", 105),
    PESACHIM("פסחים", 121),
    SHEKALIM("שקלים", 22),
    YOMA("יומא", 88),
    SUKKAH("סוכה", 56),
    BEITZAH("ביצה", 40),
    ROSH_HASHANAH("ראש השנה", 35),
    TAANIS("תענית", 31),
    MEGILLAH("מגילה", 32),
    MOED_KATAN("מועד קטן", 29),
    CHAGIGAH("חגיגה", 27),
    YEVAMOS("יבמות", 122),
    KESUBOS("כתובות", 112),
    NEDARIM("נדרים", 91),
    NAZIR("נזיר", 66),
    SOTAH("סוטה", 49),
    GITTIN("גיטין", 90),
    KIDDUSHIN("קידושין", 82),
    BAVA_KAMMA("בבא קמא", 119),
    BAVA_METZIA("בבא מציעא", 119),
    BAVA_BASRA("בבא בתרא", 176),
    SANHEDRIN("סנהדרין", 113),
    MAKKOS("מכות", 24),
    SHEVUOS("שבועות", 49),
    AVODAH_ZARAH("עבודה זרה", 76),
    HORAYOS("הוריות", 14),
    ZEVACHIM("זבחים", 120),
    MENACHOS("מנחות", 110),
    CHULLIN("חולין", 142),
    BECHOROS("בכורות", 61),
    ARACHIN("ערכין", 34),
    TEMURAH("תמורה", 34),
    KEREISOS("כריתות", 28),
    MEILAH("מעילה", 22),
    KINNIM("קינים", 25, firstPage = 23),
    TAMID("תמיד", 33, firstPage = 26),
    MIDDOS("מידות", 37, firstPage = 34),
    NIDDAH("נדה", 73);

    val pages: Int
        get() = lastPage - firstPage + 1
}

enum class YerushalmiTractate(val hebrewName: String, val pages: Int) {
    BERACHOS("ברכות", 68),
    PEAH("פיאה", 37),
    DEMAI("דמאי", 34),
    KILAYIM("כלאים", 44),
    SHEVIIS("שביעית", 31),
    TERUMOS("תרומות", 59),
    MAASROS("מעשרות", 26),
    MAASER_SHENI("מעשר שני", 33),
    CHALLAH("חלה", 28),
    ORLAH("ערלה", 20),
    BIKKURIM("ביכורים", 13),
    SHABBOS("שבת", 92),
    ERUVIN("עירובין", 65),
    PESACHIM("פסחים", 71),
    BEITZAH("ביצה", 22),
    ROSH_HASHANAH("ראש השנה", 22),
    YOMA("יומא", 42),
    SUKKAH("סוכה", 26),
    TAANIS("תענית", 26),
    SHEKALIM("שקלים", 33),
    MEGILLAH("מגילה", 34),
    CHAGIGAH("חגיגה", 22),
    MOED_KATAN("מועד קטן", 19),
    YEVAMOS("יבמות", 85),
    KESUBOS("כתובות", 72),
    SOTAH("סוטה", 47),
    NEDARIM("נדרים", 40),
    NAZIR("נזיר", 47),
    GITTIN("גיטין", 54),
    KIDDUSHIN("קידושין", 48),
    BAVA_KAMMA("בבא קמא", 44),
    BAVA_METZIA("בבא מציעא", 37),
    BAVA_BASRA("בבא בתרא", 34),
    SHEVUOS("שבועות", 44),
    MAKKOS("מכות", 9),
    SANHEDRIN("סנהדרין", 57),
    AVODAH_ZARAH("עבודה זרה", 37),
    HORAYOS("הוריות", 19),
    NIDDAH("נדה", 13);

    companion object {
        val PAGES_PER_CYCLE: Int = entries.sumOf { it.pages }
    }
}

data class BavliDaf(val tractate: BavliTractate, val page: Int)

data class YerushalmiDaf(val tractate: YerushalmiTractate, val page: Int)

object DafYomi {
    val BAVLI_FIRST_CYCLE: LocalDate = LocalDate(1923, 9, 11)

    val BAVLI_EIGHTH_CYCLE: LocalDate = LocalDate(1975, 6, 24)

    val YERUSHALMI_FIRST_CYCLE: LocalDate = LocalDate(1980, 2, 2)

    private const val EARLY_SHEKALIM_PAGES = 12

    fun bavli(date: LocalDate): BavliDaf {
        require(date >= BAVLI_FIRST_CYCLE) { "$date is before the first Daf Yomi cycle, which began on $BAVLI_FIRST_CYCLE" }
        val modernCycles = date >= BAVLI_EIGHTH_CYCLE
        val start = if (modernCycles) BAVLI_EIGHTH_CYCLE else BAVLI_FIRST_CYCLE
        var index = start.daysUntil(date) % bavliCycleLength(modernCycles)
        for (tractate in BavliTractate.entries) {
            val pages = bavliPages(tractate, modernCycles)
            if (index < pages) {
                return BavliDaf(tractate, tractate.firstPage + index)
            }
            index -= pages
        }
        throw IllegalStateException("no daf found for $date")
    }

    fun bavli(day: HebrewDay): BavliDaf = bavli(day.localDate)

    fun yerushalmi(day: HebrewDay): YerushalmiDaf? {
        val date = day.localDate
        if (day.holiday == Holiday.YOM_KIPPUR || day.holiday == Holiday.TISHA_BEAV) {
            return null
        }
        require(date >= YERUSHALMI_FIRST_CYCLE) { "$date is before the first Yerushalmi cycle, which began on $YERUSHALMI_FIRST_CYCLE" }
        var index = (YERUSHALMI_FIRST_CYCLE.daysUntil(date) - skippedDaysBefore(date)) % YerushalmiTractate.PAGES_PER_CYCLE
        for (tractate in YerushalmiTractate.entries) {
            if (index < tractate.pages) {
                return YerushalmiDaf(tractate, index + 1)
            }
            index -= tractate.pages
        }
        throw IllegalStateException("no daf found for $date")
    }

    fun yerushalmi(date: LocalDate): YerushalmiDaf? = yerushalmi(HebrewDay(date))

    private fun bavliCycleLength(modernCycles: Boolean): Int =
        BavliTractate.entries.sumOf { bavliPages(it, modernCycles) }

    private fun bavliPages(tractate: BavliTractate, modernCycles: Boolean): Int =
        if (tractate == BavliTractate.SHEKALIM && !modernCycles) EARLY_SHEKALIM_PAGES else tractate.pages

    private fun skippedDaysBefore(date: LocalDate): Int {
        val firstYear = HebrewDate.from(YERUSHALMI_FIRST_CYCLE).year
        val lastYear = HebrewDate.from(date).year
        var skipped = 0
        for (year in firstYear..lastYear) {
            for (fast in listOf(HebrewDate.of(year, HebrewMonth.TISHREI, 10), observedTishaBeav(year))) {
                val fastDate = fast.localDate
                if (fastDate >= YERUSHALMI_FIRST_CYCLE && fastDate < date) {
                    skipped++
                }
            }
        }
        return skipped
    }

    private fun observedTishaBeav(year: Int): HebrewDate {
        val nominal = HebrewDate.of(year, HebrewMonth.AV, 9)
        return if (nominal.dayOfWeek == DayOfWeek.SATURDAY) nominal.plusDays(1) else nominal
    }
}
