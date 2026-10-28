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

package com.eskotech.luach.zmanim

enum class Zman {
    ALOS,
    MISHEYAKIR,
    HANETZ,
    SOF_ZMAN_SHEMA,
    SOF_ZMAN_TEFILLAH,
    SOF_ZMAN_ACHILAS_CHAMETZ,
    SOF_ZMAN_BIUR_CHAMETZ,
    CHATZOS,
    MINCHA_GEDOLA,
    SAMUCH_LEMINCHA_KETANA,
    MINCHA_KETANA,
    PLAG_HAMINCHA,
    HADLAKAS_NEIROS,
    SHKIAH,
    TZAIS,
    CHATZOS_HALAYLAH,
}

data class Shitos(
    val alos: DayEdge = DayEdge.ALOS_16_1,
    val misheyakir: DayEdge = DayEdge.MISHEYAKIR_11_5,
    val day: HalachicDay = HalachicDay.GRA,
    val tzais: DayEdge = DayEdge.TZAIS_GEONIM_8_5,
) {
    companion object {
        val GRA: Shitos = Shitos()

        val MAGEN_AVRAHAM: Shitos = Shitos(alos = DayEdge.ALOS_72, day = HalachicDay.MAGEN_AVRAHAM_72, tzais = DayEdge.TZAIS_72)
    }
}
