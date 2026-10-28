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

data class TachanunCustoms(
    val omitAfterSukkos: Boolean = false,
    val omitWeekAfterShavuos: Boolean = true,
    val omit13SivanOutsideIsrael: Boolean = false,
    val omitPesachSheni: Boolean = true,
    val omit15IyarOutsideIsrael: Boolean = false,
    val omitMinchaErevLagBaomer: Boolean = true,
    val omitShivasYemeiHamiluim: Boolean = false,
    val omitWeekOfHod: Boolean = false,
    val omitWeekOfPurim: Boolean = false,
    val omitFridays: Boolean = false,
    val omitSundays: Boolean = false,
    val omitMincha: Boolean = false,
)

enum class Hallel {
    NONE,
    PARTIAL,
    FULL,
}

data class TefillahSummary(
    val tachanunAtShacharis: Boolean,
    val tachanunAtMincha: Boolean,
    val veseinTalUmatar: Boolean,
    val veseinBerachah: Boolean,
    val isVeseinTalUmatarStart: Boolean,
    val veseinTalUmatarStartsTonight: Boolean,
    val mashivHaruach: Boolean,
    val moridHatal: Boolean,
    val isMashivHaruachStart: Boolean,
    val isMashivHaruachEnd: Boolean,
    val hallel: Hallel,
    val alHanissim: Boolean,
    val yaalehVeyavo: Boolean,
)

class TefillahRules(val customs: TachanunCustoms = TachanunCustoms()) {

    fun tachanunAtShacharis(day: HebrewDay): Boolean {
        val d = day.dayOfMonth
        val month = day.month
        val weekday = day.dayOfWeek
        val holiday = day.holiday
        val adar = if (day.date.isMeuberes) HebrewMonth.ADAR_II else HebrewMonth.ADAR
        val omitted = weekday == DayOfWeek.SATURDAY ||
            (customs.omitSundays && weekday == DayOfWeek.SUNDAY) ||
            (customs.omitFridays && weekday == DayOfWeek.FRIDAY) ||
            month == HebrewMonth.NISAN ||
            (month == HebrewMonth.TISHREI && d > 8 && (customs.omitAfterSukkos || d < 22)) ||
            (month == HebrewMonth.SIVAN && d < sivanTachanunResumes(day)) ||
            (day.isYomTov && !day.isTaanis && !(holiday == Holiday.PESACH_SHENI && !customs.omitPesachSheni)) ||
            (!day.inIsrael && customs.omitPesachSheni && customs.omit15IyarOutsideIsrael && month == HebrewMonth.IYAR && d == 15) ||
            holiday == Holiday.EREV_ROSH_HASHANAH ||
            holiday == Holiday.TISHA_BEAV ||
            day.isIsruChag ||
            day.isRoshChodesh ||
            (customs.omitShivasYemeiHamiluim && month == adar && d > 22) ||
            (customs.omitWeekOfPurim && month == adar && d in 11..17) ||
            (customs.omitWeekOfHod && month == HebrewMonth.IYAR && d in 14..20)
        return !omitted
    }

    fun tachanunAtMincha(day: HebrewDay): Boolean {
        if (customs.omitMincha || day.dayOfWeek == DayOfWeek.FRIDAY || !tachanunAtShacharis(day)) {
            return false
        }
        val tomorrow = day.plusDays(1)
        val tomorrowHoliday = tomorrow.holiday
        if (!tachanunAtShacharis(tomorrow) && tomorrowHoliday !in KEEPS_TACHANUN_THE_DAY_BEFORE) {
            return false
        }
        return !(customs.omitMinchaErevLagBaomer && tomorrowHoliday == Holiday.LAG_BAOMER)
    }

    fun isVeseinTalUmatarStart(day: HebrewDay): Boolean {
        if (day.inIsrael) {
            return day.month == HebrewMonth.CHESHVAN && day.dayOfMonth == 7
        }
        return when (day.dayOfWeek) {
            DayOfWeek.SATURDAY -> false
            DayOfWeek.SUNDAY -> day.daysFromTekufasTishrei in 47..48
            else -> day.daysFromTekufasTishrei == 47
        }
    }

    fun veseinTalUmatarStartsTonight(day: HebrewDay): Boolean {
        if (day.inIsrael) {
            return day.month == HebrewMonth.CHESHVAN && day.dayOfMonth == 6
        }
        return when (day.dayOfWeek) {
            DayOfWeek.FRIDAY -> false
            DayOfWeek.SATURDAY -> day.daysFromTekufasTishrei in 46..47
            else -> day.daysFromTekufasTishrei == 46
        }
    }

    fun veseinTalUmatar(day: HebrewDay): Boolean {
        if (day.month == HebrewMonth.NISAN && day.dayOfMonth < 15) {
            return true
        }
        if (day.month.number < HebrewMonth.CHESHVAN.number) {
            return false
        }
        return if (day.inIsrael) {
            day.month != HebrewMonth.CHESHVAN || day.dayOfMonth >= 7
        } else {
            day.daysFromTekufasTishrei >= 47
        }
    }

    fun veseinBerachah(day: HebrewDay): Boolean = !veseinTalUmatar(day)

    fun isMashivHaruachStart(day: HebrewDay): Boolean = day.month == HebrewMonth.TISHREI && day.dayOfMonth == 22

    fun isMashivHaruachEnd(day: HebrewDay): Boolean = day.month == HebrewMonth.NISAN && day.dayOfMonth == 15

    fun mashivHaruach(day: HebrewDay): Boolean {
        val start = HebrewDate.of(day.year, HebrewMonth.TISHREI, 22)
        val end = HebrewDate.of(day.year, HebrewMonth.NISAN, 15)
        return day.date > start && day.date < end
    }

    fun moridHatal(day: HebrewDay): Boolean = !mashivHaruach(day) || isMashivHaruachStart(day) || isMashivHaruachEnd(day)

    fun hallel(day: HebrewDay): Hallel {
        val d = day.dayOfMonth
        val recited = day.isRoshChodesh || day.isChanukah || when (day.month) {
            HebrewMonth.NISAN -> d in 15..(if (day.inIsrael) 21 else 22)
            HebrewMonth.SIVAN -> d == 6 || (d == 7 && !day.inIsrael)
            HebrewMonth.TISHREI -> d in 15..(if (day.inIsrael) 22 else 23)
            else -> false
        }
        if (!recited) {
            return Hallel.NONE
        }
        val partial = (day.isRoshChodesh && !day.isChanukah) ||
            (day.month == HebrewMonth.NISAN && d > (if (day.inIsrael) 15 else 16))
        return if (partial) Hallel.PARTIAL else Hallel.FULL
    }

    fun alHanissim(day: HebrewDay): Boolean = day.isPurim || day.isChanukah

    fun yaalehVeyavo(day: HebrewDay): Boolean =
        day.isPesach || day.isShavuos || day.isRoshHashanah || day.isYomKippur || day.isSukkos ||
            day.isShminiAtzeres || day.isSimchasTorah || day.isRoshChodesh

    private fun sivanTachanunResumes(day: HebrewDay): Int = when {
        !customs.omitWeekAfterShavuos -> 7
        !day.inIsrael && customs.omit13SivanOutsideIsrael -> 14
        else -> 13
    }

    private companion object {
        val KEEPS_TACHANUN_THE_DAY_BEFORE = setOf(
            Holiday.EREV_ROSH_HASHANAH, Holiday.EREV_YOM_KIPPUR, Holiday.PESACH_SHENI, Holiday.LAG_BAOMER,
        )
    }

    fun all(day: HebrewDay): TefillahSummary = TefillahSummary(
        tachanunAtShacharis = tachanunAtShacharis(day),
        tachanunAtMincha = tachanunAtMincha(day),
        veseinTalUmatar = veseinTalUmatar(day),
        veseinBerachah = veseinBerachah(day),
        isVeseinTalUmatarStart = isVeseinTalUmatarStart(day),
        veseinTalUmatarStartsTonight = veseinTalUmatarStartsTonight(day),
        mashivHaruach = mashivHaruach(day),
        moridHatal = moridHatal(day),
        isMashivHaruachStart = isMashivHaruachStart(day),
        isMashivHaruachEnd = isMashivHaruachEnd(day),
        hallel = hallel(day),
        alHanissim = alHanissim(day),
        yaalehVeyavo = yaalehVeyavo(day),
    )
}
