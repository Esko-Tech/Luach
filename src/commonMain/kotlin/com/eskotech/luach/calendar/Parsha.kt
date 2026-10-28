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

enum class Parsha(val first: Parsha? = null, val second: Parsha? = null, val isSpecialShabbos: Boolean = false) {
    BERESHIT,
    NOACH,
    LECH_LECHA,
    VAYERA,
    CHAYEI_SARAH,
    TOLDOT,
    VAYETZE,
    VAYISHLACH,
    VAYESHEV,
    MIKETZ,
    VAYIGASH,
    VAYECHI,
    SHEMOT,
    VAERA,
    BO,
    BESHALACH,
    YITRO,
    MISHPATIM,
    TERUMAH,
    TETZAVEH,
    KI_TISA,
    VAYAKHEL,
    PEKUDEI,
    VAYIKRA,
    TZAV,
    SHEMINI,
    TAZRIA,
    METZORA,
    ACHAREI_MOT,
    KEDOSHIM,
    EMOR,
    BEHAR,
    BECHUKOTAI,
    BAMIDBAR,
    NASO,
    BEHAALOTCHA,
    SHELACH,
    KORACH,
    CHUKAT,
    BALAK,
    PINCHAS,
    MATOT,
    MASEI,
    DEVARIM,
    VAETCHANAN,
    EKEV,
    REEH,
    SHOFTIM,
    KI_TETZE,
    KI_TAVO,
    NITZAVIM,
    VAYELECH,
    HAAZINU,
    VEZOT_HABERACHAH,
    VAYAKHEL_PEKUDEI(VAYAKHEL, PEKUDEI),
    TAZRIA_METZORA(TAZRIA, METZORA),
    ACHAREI_MOT_KEDOSHIM(ACHAREI_MOT, KEDOSHIM),
    BEHAR_BECHUKOTAI(BEHAR, BECHUKOTAI),
    CHUKAT_BALAK(CHUKAT, BALAK),
    MATOT_MASEI(MATOT, MASEI),
    NITZAVIM_VAYELECH(NITZAVIM, VAYELECH),
    SHEKALIM(isSpecialShabbos = true),
    ZACHOR(isSpecialShabbos = true),
    PARAH(isSpecialShabbos = true),
    HACHODESH(isSpecialShabbos = true),
    SHUVAH(isSpecialShabbos = true),
    SHIRAH(isSpecialShabbos = true),
    HAGADOL(isSpecialShabbos = true),
    CHAZON(isSpecialShabbos = true),
    NACHAMU(isSpecialShabbos = true);

    val isDouble: Boolean
        get() = first != null

    companion object {
        val WEEKLY: List<Parsha> = entries.filter { it.first == null && !it.isSpecialShabbos && it != VEZOT_HABERACHAH }

        val DOUBLES: List<Parsha> = entries.filter { it.isDouble }

        val SPECIAL_SHABBOSOS: List<Parsha> = entries.filter { it.isSpecialShabbos }
    }
}

internal object TorahCycle {

    private const val PARSHIOS_THROUGH_BAMIDBAR = 34

    private const val PARSHIOS_THROUGH_DEVARIM = 44

    private const val PARSHIOS_FROM_VAETCHANAN_TO_NITZAVIM = 7

    fun parshaOn(date: HebrewDate, inIsrael: Boolean): Parsha? {
        if (date.dayOfWeek != DayOfWeek.SATURDAY) {
            return null
        }
        val cycleYear = if (date.epochDay <= simchasTorah(date.year, inIsrael)) date.year - 1 else date.year
        return schedule(cycleYear, inIsrael)[date.epochDay]
    }

    fun schedule(year: Int, inIsrael: Boolean): Map<Long, Parsha> {
        val pesach = HebrewDate.of(year, HebrewMonth.NISAN, 15).epochDay
        val shavuos = HebrewDate.of(year, HebrewMonth.SIVAN, 6).epochDay
        val tishaBeav = HebrewDate.of(year, HebrewMonth.AV, 9).epochDay
        val nextRoshHashanah = HebrewDate.roshHashanaEpochDay(year + 1)
        val nextSimchasTorah = simchasTorah(year + 1, inIsrael)

        val withoutReading = HashSet<Long>()
        for (offset in 0 until if (inIsrael) 7 else 8) withoutReading += pesach + offset
        withoutReading += shavuos
        if (!inIsrael) withoutReading += shavuos + 1
        withoutReading += nextRoshHashanah
        withoutReading += nextRoshHashanah + 1
        withoutReading += nextRoshHashanah + 9
        for (offset in 14..21) withoutReading += nextRoshHashanah + offset

        val shabbosos = ArrayList<Long>()
        var shabbos = nextShabbosAfter(simchasTorah(year, inIsrael))
        while (shabbos < nextSimchasTorah) {
            if (shabbos !in withoutReading) shabbosos += shabbos
            shabbos += 7
        }

        val beforePesach = shabbosos.count { it < pesach }
        val beforeShavuos = shabbosos.count { it < shavuos }
        val throughChazon = shabbosos.count { it <= tishaBeav }
        val afterNextRoshHashanah = shabbosos.count { it > nextRoshHashanah }

        val combined = HashSet<Parsha>()
        var readThroughShavuos = beforeShavuos
        if (beforeShavuos < PARSHIOS_THROUGH_BAMIDBAR) {
            var needed = PARSHIOS_THROUGH_BAMIDBAR - beforeShavuos
            if (beforePesach < 25) {
                combined += Parsha.VAYAKHEL_PEKUDEI
                needed--
            }
            for (candidate in listOf(Parsha.TAZRIA_METZORA, Parsha.ACHAREI_MOT_KEDOSHIM, Parsha.BEHAR_BECHUKOTAI).take(needed)) {
                combined += candidate
            }
            readThroughShavuos = PARSHIOS_THROUGH_BAMIDBAR
        }
        val summerNeeded = (PARSHIOS_THROUGH_DEVARIM - readThroughShavuos) - (throughChazon - beforeShavuos)
        val summerCandidates = buildList {
            if (!inIsrael && isShabbos(shavuos + 1)) add(Parsha.CHUKAT_BALAK)
            add(Parsha.MATOT_MASEI)
        }
        for (candidate in summerCandidates.take(summerNeeded)) {
            combined += candidate
        }
        if (afterNextRoshHashanah == 1) {
            combined += Parsha.NITZAVIM_VAYELECH
        }

        val result = LinkedHashMap<Long, Parsha>()
        var index = 0
        for (day in shabbosos) {
            val single = Parsha.WEEKLY[index]
            val double = Parsha.DOUBLES.firstOrNull { it.first == single && it in combined }
            if (double != null) {
                result[day] = double
                index += 2
            } else {
                result[day] = single
                index += 1
            }
        }
        return result
    }

    private fun simchasTorah(year: Int, inIsrael: Boolean): Long =
        HebrewDate.of(year, HebrewMonth.TISHREI, if (inIsrael) 22 else 23).epochDay

    private fun nextShabbosAfter(epochDay: Long): Long {
        var candidate = epochDay + 1
        while (!isShabbos(candidate)) {
            candidate++
        }
        return candidate
    }

    private fun isShabbos(epochDay: Long): Boolean = (epochDay + 4).mod(7L) == 6L
}
