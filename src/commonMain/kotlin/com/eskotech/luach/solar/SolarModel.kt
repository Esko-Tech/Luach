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

package com.eskotech.luach.solar

import kotlin.math.PI
import kotlin.math.acos
import kotlinx.datetime.LocalDate

data class ApparentHorizon(
    val refraction: Double = 34.0 / 60,
    val solarRadius: Double = 16.0 / 60,
    val earthRadius: Double = 6356.9,
) {
    fun dip(elevation: Double): Double = acos(earthRadius / (earthRadius + elevation / 1000)) * 180 / PI

    fun zenith(elevation: Double): Double = GEOMETRIC_HORIZON + refraction + solarRadius + dip(elevation)

    companion object {
        const val GEOMETRIC_HORIZON: Double = 90.0
    }
}

interface SolarModel {
    val name: String

    fun sunrise(date: LocalDate, latitude: Double, longitude: Double, zenith: Double): Double?

    fun sunset(date: LocalDate, latitude: Double, longitude: Double, zenith: Double): Double?

    fun solarNoon(date: LocalDate, latitude: Double, longitude: Double): Double?
}

internal fun Double.wrapToDay(): Double {
    var hours = this % 24.0
    if (hours < 0) {
        hours += 24.0
    }
    return hours
}

internal fun Double.orNullIfNaN(): Double? = if (isNaN()) null else this
