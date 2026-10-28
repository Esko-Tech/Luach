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

enum class Holiday(
    val isTaanis: Boolean = false,
    val isErev: Boolean = false,
    val asurBemelacha: Boolean = false,
) {
    EREV_PESACH(isErev = true),
    PESACH(asurBemelacha = true),
    CHOL_HAMOED_PESACH,
    PESACH_SHENI,
    EREV_SHAVUOS(isErev = true),
    SHAVUOS(asurBemelacha = true),
    SHIVA_ASAR_BETAMMUZ(isTaanis = true),
    TISHA_BEAV(isTaanis = true),
    TU_BEAV,
    EREV_ROSH_HASHANAH(isErev = true),
    ROSH_HASHANAH(asurBemelacha = true),
    TZOM_GEDALIAH(isTaanis = true),
    EREV_YOM_KIPPUR(isErev = true),
    YOM_KIPPUR(isTaanis = true, asurBemelacha = true),
    EREV_SUKKOS(isErev = true),
    SUKKOS(asurBemelacha = true),
    CHOL_HAMOED_SUKKOS,
    HOSHANA_RABBAH,
    SHEMINI_ATZERES(asurBemelacha = true),
    SIMCHAS_TORAH(asurBemelacha = true),
    CHANUKAH,
    ASARAH_BETEVES(isTaanis = true),
    TU_BISHVAT,
    TAANIS_ESTHER(isTaanis = true),
    PURIM,
    SHUSHAN_PURIM,
    PURIM_KATAN,
    SHUSHAN_PURIM_KATAN,
    LAG_BAOMER,
    ISRU_CHAG,
}
