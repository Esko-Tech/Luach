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

enum class HebrewMonth(val number: Int) {
    NISAN(1),
    IYAR(2),
    SIVAN(3),
    TAMMUZ(4),
    AV(5),
    ELUL(6),
    TISHREI(7),
    CHESHVAN(8),
    KISLEV(9),
    TEVET(10),
    SHEVAT(11),
    ADAR(12),
    ADAR_II(13);

    companion object {
        fun of(number: Int): HebrewMonth =
            entries.getOrNull(number - 1) ?: throw IllegalArgumentException("there is no Hebrew month numbered $number")

        fun inYearOrder(meuberes: Boolean): List<HebrewMonth> = if (meuberes) LEAP_YEAR_ORDER else COMMON_YEAR_ORDER

        private val COMMON_YEAR_ORDER = listOf(TISHREI, CHESHVAN, KISLEV, TEVET, SHEVAT, ADAR, NISAN, IYAR, SIVAN, TAMMUZ, AV, ELUL)

        private val LEAP_YEAR_ORDER = listOf(TISHREI, CHESHVAN, KISLEV, TEVET, SHEVAT, ADAR, ADAR_II, NISAN, IYAR, SIVAN, TAMMUZ, AV, ELUL)
    }
}

enum class YearType {
    DEFICIENT,
    REGULAR,
    COMPLETE,
}
