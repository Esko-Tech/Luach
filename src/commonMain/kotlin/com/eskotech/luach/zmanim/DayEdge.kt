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

sealed interface DayEdge {
    data object Horizon : DayEdge

    data class Depression(val degrees: Double) : DayEdge

    data class Minutes(val minutes: Double) : DayEdge

    data class ProportionalMinutes(val minutes: Double) : DayEdge

    data class Shifted(val edge: DayEdge, val minutes: Double) : DayEdge

    data class DawnInterval(val dawn: DayEdge, val factor: Double) : DayEdge

    companion object {
        val ALOS_16_1: DayEdge = Depression(16.1)

        val ALOS_18: DayEdge = Depression(18.0)

        val ALOS_19_8: DayEdge = Depression(19.8)

        val ALOS_26: DayEdge = Depression(26.0)

        val ALOS_BAAL_HATANYA: DayEdge = Depression(16.9)

        val ALOS_72: DayEdge = Minutes(72.0)

        val ALOS_90: DayEdge = Minutes(90.0)

        val ALOS_120: DayEdge = Minutes(120.0)

        val ALOS_72_ZMANIYOS: DayEdge = ProportionalMinutes(72.0)

        val MISHEYAKIR_11_5: DayEdge = Depression(11.5)

        val MISHEYAKIR_11: DayEdge = Depression(11.0)

        val MISHEYAKIR_10_2: DayEdge = Depression(10.2)

        val MISHEYAKIR_9_5: DayEdge = Depression(9.5)

        val MISHEYAKIR_7_65: DayEdge = Depression(7.65)

        val TZAIS_GEONIM_3_7: DayEdge = Depression(3.7)

        val TZAIS_GEONIM_3_8: DayEdge = Depression(3.8)

        val TZAIS_GEONIM_5_95: DayEdge = Depression(5.95)

        val TZAIS_GEONIM_7_083: DayEdge = Depression(7 + 5.0 / 60)

        val TZAIS_GEONIM_8_5: DayEdge = Depression(8.5)

        val TZAIS_BAAL_HATANYA: DayEdge = Depression(6.0)

        val TZAIS_RABBEINU_TAM_13_24: DayEdge = Depression(13.24)

        val TZAIS_16_1: DayEdge = Depression(16.1)

        val TZAIS_18: DayEdge = Depression(18.0)

        val TZAIS_19_8: DayEdge = Depression(19.8)

        val TZAIS_26: DayEdge = Depression(26.0)

        val TZAIS_72: DayEdge = Minutes(72.0)

        val TZAIS_90: DayEdge = Minutes(90.0)

        val TZAIS_96: DayEdge = Minutes(96.0)

        val TZAIS_120: DayEdge = Minutes(120.0)

        val TZAIS_72_ZMANIYOS: DayEdge = ProportionalMinutes(72.0)
    }
}

data class HalachicDay(val alos: DayEdge, val tzais: DayEdge) {
    companion object {
        val GRA: HalachicDay = HalachicDay(DayEdge.Horizon, DayEdge.Horizon)

        val MAGEN_AVRAHAM_72: HalachicDay = minutes(72.0)

        val MAGEN_AVRAHAM_90: HalachicDay = minutes(90.0)

        val MAGEN_AVRAHAM_120: HalachicDay = minutes(120.0)

        val MAGEN_AVRAHAM_72_ZMANIYOS: HalachicDay = proportionalMinutes(72.0)

        val MAGEN_AVRAHAM_16_1: HalachicDay = degrees(16.1)

        val MAGEN_AVRAHAM_18: HalachicDay = degrees(18.0)

        val MAGEN_AVRAHAM_19_8: HalachicDay = degrees(19.8)

        val MAGEN_AVRAHAM_26: HalachicDay = degrees(26.0)

        val BAAL_HATANYA: HalachicDay = degrees(1.583)

        val ATERET_TORAH: HalachicDay = HalachicDay(DayEdge.ALOS_72_ZMANIYOS, DayEdge.Minutes(40.0))

        fun minutes(minutes: Double): HalachicDay = HalachicDay(DayEdge.Minutes(minutes), DayEdge.Minutes(minutes))

        fun proportionalMinutes(minutes: Double): HalachicDay =
            HalachicDay(DayEdge.ProportionalMinutes(minutes), DayEdge.ProportionalMinutes(minutes))

        fun degrees(degrees: Double): HalachicDay = HalachicDay(DayEdge.Depression(degrees), DayEdge.Depression(degrees))

        fun degrees(alos: Double, tzais: Double): HalachicDay = HalachicDay(DayEdge.Depression(alos), DayEdge.Depression(tzais))
    }
}

enum class KiddushLevanaStart {
    THREE_DAYS,
    SEVEN_DAYS,
}

enum class KiddushLevanaEnd {
    HALF_MONTH,
    FIFTEEN_DAYS,
}
