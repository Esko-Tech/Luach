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

import kotlin.math.floor
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

data class HebrewDay(
    val date: HebrewDate,
    val inIsrael: Boolean = false,
    val mukefChoma: Boolean = false,
) {
    constructor(date: LocalDate, inIsrael: Boolean = false, mukefChoma: Boolean = false) :
        this(HebrewDate.from(date), inIsrael, mukefChoma)

    constructor(instant: Instant, timeZone: TimeZone, inIsrael: Boolean = false, mukefChoma: Boolean = false) :
        this(HebrewDate.from(instant, timeZone), inIsrael, mukefChoma)

    val localDate: LocalDate
        get() = date.localDate

    val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek

    val month: HebrewMonth
        get() = date.month

    val dayOfMonth: Int
        get() = date.day

    val year: Int
        get() = date.year

    val holiday: Holiday?
        get() {
            val d = date.day
            val w = date.dayOfWeek
            return when (date.month) {
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
                HebrewMonth.TAMMUZ -> if (observedFast(17, d, w)) Holiday.SHIVA_ASAR_BETAMMUZ else null
                HebrewMonth.AV -> when {
                    observedFast(9, d, w) -> Holiday.TISHA_BEAV
                    d == 15 -> Holiday.TU_BEAV
                    else -> null
                }
                HebrewMonth.ELUL -> if (d == 29) Holiday.EREV_ROSH_HASHANAH else null
                HebrewMonth.TISHREI -> when {
                    d == 1 || d == 2 -> Holiday.ROSH_HASHANAH
                    observedFast(3, d, w) -> Holiday.TZOM_GEDALIAH
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
                HebrewMonth.CHESHVAN -> null
                HebrewMonth.KISLEV -> if (d >= 25) Holiday.CHANUKAH else null
                HebrewMonth.TEVET -> when {
                    d <= 2 || (d == 3 && date.yearType == YearType.DEFICIENT) -> Holiday.CHANUKAH
                    d == 10 -> Holiday.ASARAH_BETEVES
                    else -> null
                }
                HebrewMonth.SHEVAT -> if (d == 15) Holiday.TU_BISHVAT else null
                HebrewMonth.ADAR -> if (date.isMeuberes) {
                    when (d) {
                        14 -> Holiday.PURIM_KATAN
                        15 -> Holiday.SHUSHAN_PURIM_KATAN
                        else -> null
                    }
                } else {
                    purimMonth(d, w)
                }
                HebrewMonth.ADAR_II -> purimMonth(d, w)
            }
        }

    val isYomTov: Boolean
        get() {
            val h = holiday ?: return false
            return !h.isErev && h != Holiday.ISRU_CHAG && (!h.isTaanis || h == Holiday.YOM_KIPPUR)
        }

    val isYomTovAsurBemelacha: Boolean
        get() = holiday?.asurBemelacha == true

    val isAsurBemelacha: Boolean
        get() = dayOfWeek == DayOfWeek.SATURDAY || isYomTovAsurBemelacha

    val isErevYomTov: Boolean
        get() {
            val h = holiday
            return h?.isErev == true || h == Holiday.HOSHANA_RABBAH || (h == Holiday.CHOL_HAMOED_PESACH && date.day == 20)
        }

    val isErevYomTovSheni: Boolean
        get() = (month == HebrewMonth.TISHREI && dayOfMonth == 1) ||
            (!inIsrael && (
                (month == HebrewMonth.NISAN && (dayOfMonth == 15 || dayOfMonth == 21)) ||
                    (month == HebrewMonth.TISHREI && (dayOfMonth == 15 || dayOfMonth == 22)) ||
                    (month == HebrewMonth.SIVAN && dayOfMonth == 6)
                ))

    val hasHadlakasNeiros: Boolean
        get() = dayOfWeek == DayOfWeek.FRIDAY || isErevYomTov || isErevYomTovSheni

    val isTaanis: Boolean
        get() = holiday?.isTaanis == true

    val isAseresYemeiTeshuvah: Boolean
        get() = month == HebrewMonth.TISHREI && dayOfMonth <= 10

    val isPesach: Boolean
        get() = holiday == Holiday.PESACH || holiday == Holiday.CHOL_HAMOED_PESACH

    val isCholHamoedPesach: Boolean
        get() = holiday == Holiday.CHOL_HAMOED_PESACH

    val isShavuos: Boolean
        get() = holiday == Holiday.SHAVUOS

    val isRoshHashanah: Boolean
        get() = holiday == Holiday.ROSH_HASHANAH

    val isYomKippur: Boolean
        get() = holiday == Holiday.YOM_KIPPUR

    val isSukkos: Boolean
        get() = holiday == Holiday.SUKKOS || isCholHamoedSukkos

    val isHoshanaRabbah: Boolean
        get() = holiday == Holiday.HOSHANA_RABBAH

    val isShminiAtzeres: Boolean
        get() = holiday == Holiday.SHEMINI_ATZERES

    val isSimchasTorah: Boolean
        get() = holiday == Holiday.SIMCHAS_TORAH

    val isCholHamoedSukkos: Boolean
        get() = holiday == Holiday.CHOL_HAMOED_SUKKOS || holiday == Holiday.HOSHANA_RABBAH

    val isCholHamoed: Boolean
        get() = isCholHamoedPesach || isCholHamoedSukkos

    val isChanukah: Boolean
        get() = holiday == Holiday.CHANUKAH

    val isPurim: Boolean
        get() = holiday == if (mukefChoma) Holiday.SHUSHAN_PURIM else Holiday.PURIM

    val isTishaBeav: Boolean
        get() = holiday == Holiday.TISHA_BEAV

    val isIsruChag: Boolean
        get() = holiday == Holiday.ISRU_CHAG

    val isTaanisBechoros: Boolean
        get() = month == HebrewMonth.NISAN &&
            ((dayOfMonth == 14 && dayOfWeek != DayOfWeek.SATURDAY) || (dayOfMonth == 12 && dayOfWeek == DayOfWeek.THURSDAY))

    val isRoshChodesh: Boolean
        get() = (dayOfMonth == 1 && month != HebrewMonth.TISHREI) || dayOfMonth == 30

    val isErevRoshChodesh: Boolean
        get() = dayOfMonth == 29 && month != HebrewMonth.ELUL

    val isMacharChodesh: Boolean
        get() = dayOfWeek == DayOfWeek.SATURDAY && dayOfMonth >= 29

    val isShabbosMevorchim: Boolean
        get() = dayOfWeek == DayOfWeek.SATURDAY && dayOfMonth in 23..29 && month != HebrewMonth.ELUL

    val isYomKippurKatan: Boolean
        get() {
            if (month == HebrewMonth.ELUL || month == HebrewMonth.TISHREI || month == HebrewMonth.KISLEV || month == HebrewMonth.NISAN) {
                return false
            }
            return (dayOfMonth == 29 && dayOfWeek != DayOfWeek.FRIDAY && dayOfWeek != DayOfWeek.SATURDAY) ||
                (dayOfWeek == DayOfWeek.THURSDAY && (dayOfMonth == 27 || dayOfMonth == 28))
        }

    val isBehab: Boolean
        get() = (month == HebrewMonth.CHESHVAN || month == HebrewMonth.IYAR) &&
            ((dayOfWeek == DayOfWeek.MONDAY && dayOfMonth in 5..17) || (dayOfWeek == DayOfWeek.THURSDAY && dayOfMonth in 8..13))

    val isBirkasHachamah: Boolean
        get() = (date.epochDay - BIRKAS_HACHAMAH_REFERENCE).mod(SOLAR_CYCLE_DAYS) == 0L

    val dayOfChanukah: Int?
        get() = when {
            !isChanukah -> null
            month == HebrewMonth.KISLEV -> dayOfMonth - 24
            else -> dayOfMonth + HebrewDate.monthLength(year, HebrewMonth.KISLEV) - 24
        }

    val sefirasHaomer: Int?
        get() = when {
            month == HebrewMonth.NISAN && dayOfMonth >= 16 -> dayOfMonth - 15
            month == HebrewMonth.IYAR -> dayOfMonth + 15
            month == HebrewMonth.SIVAN && dayOfMonth < 6 -> dayOfMonth + 44
            else -> null
        }

    val parsha: Parsha?
        get() = TorahCycle.parshaOn(date, inIsrael)

    val upcomingParsha: Parsha
        get() {
            var candidate = copy(date = date.plusDays(if (dayOfWeek == DayOfWeek.SATURDAY) 7 else daysUntilShabbos()))
            while (true) {
                candidate.parsha?.let { return it }
                candidate = candidate.copy(date = candidate.date.plusDays(7))
            }
        }

    val specialShabbos: Parsha?
        get() {
            if (dayOfWeek != DayOfWeek.SATURDAY) {
                return null
            }
            val d = dayOfMonth
            val leap = date.isMeuberes
            val monthBeforePurim = if (leap) HebrewMonth.ADAR else HebrewMonth.SHEVAT
            val purimMonth = if (leap) HebrewMonth.ADAR_II else HebrewMonth.ADAR
            return when (month) {
                monthBeforePurim -> if (d == 25 || d == 27 || d == 29) Parsha.SHEKALIM else null
                purimMonth -> when (d) {
                    1 -> Parsha.SHEKALIM
                    8, 9, 11, 13 -> Parsha.ZACHOR
                    18, 20, 22, 23 -> Parsha.PARAH
                    25, 27, 29 -> Parsha.HACHODESH
                    else -> null
                }
                HebrewMonth.NISAN -> when {
                    d == 1 -> Parsha.HACHODESH
                    d in 8..14 -> Parsha.HAGADOL
                    else -> null
                }
                HebrewMonth.AV -> when {
                    d in 4..9 -> Parsha.CHAZON
                    d in 10..16 -> Parsha.NACHAMU
                    else -> null
                }
                HebrewMonth.TISHREI -> if (d in 3..8) Parsha.SHUVAH else null
                else -> null
            } ?: if (parsha == Parsha.BESHALACH) Parsha.SHIRAH else null
        }

    val daysFromTekufasTishrei: Int
        get() = floor((date.epochDay - HebrewDate.WEEK_ZERO_EPOCH_DAY) - 0.5 - (year - 1) * 365.25).toInt()

    val molad: Molad
        get() = date.molad

    val bavliDaf: BavliDaf
        get() = DafYomi.bavli(this)

    val yerushalmiDaf: YerushalmiDaf?
        get() = DafYomi.yerushalmi(this)

    fun plusDays(days: Int): HebrewDay = copy(date = date.plusDays(days))

    private fun daysUntilShabbos(): Int = (DayOfWeek.SATURDAY.ordinal - dayOfWeek.ordinal + 7) % 7

    private fun observedFast(nominalDay: Int, d: Int, w: DayOfWeek): Boolean =
        (d == nominalDay && w != DayOfWeek.SATURDAY) || (d == nominalDay + 1 && w == DayOfWeek.SUNDAY)

    private fun purimMonth(d: Int, w: DayOfWeek): Holiday? = when {
        (d == 13 && w != DayOfWeek.FRIDAY && w != DayOfWeek.SATURDAY) || (d == 11 && w == DayOfWeek.THURSDAY) -> Holiday.TAANIS_ESTHER
        d == 14 -> Holiday.PURIM
        d == 15 -> Holiday.SHUSHAN_PURIM
        else -> null
    }

    companion object {
        private val BIRKAS_HACHAMAH_REFERENCE: Long = LocalDate(2009, 4, 8).toEpochDays()

        private const val SOLAR_CYCLE_DAYS: Long = 28 * 365 + 7

        fun today(timeZone: TimeZone = TimeZone.currentSystemDefault(), inIsrael: Boolean = false, mukefChoma: Boolean = false): HebrewDay =
            HebrewDay(HebrewDate.today(timeZone), inIsrael, mukefChoma)
    }
}
