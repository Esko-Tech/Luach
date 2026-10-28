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
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.datetime.LocalDate

object AlmanacSolarModel : SolarModel {

    override val name: String = "Almanac for Computers sunrise and sunset"

    override fun sunrise(date: LocalDate, latitude: Double, longitude: Double, zenith: Double): Double? =
        crossing(date, latitude, longitude, zenith, rising = true)

    override fun sunset(date: LocalDate, latitude: Double, longitude: Double, zenith: Double): Double? =
        crossing(date, latitude, longitude, zenith, rising = false)

    override fun solarNoon(date: LocalDate, latitude: Double, longitude: Double): Double? {
        val horizon = ApparentHorizon.GEOMETRIC_HORIZON
        val rise = sunrise(date, latitude, longitude, horizon) ?: return null
        var set = sunset(date, latitude, longitude, horizon) ?: return null
        if (set < rise) {
            set += 24
        }
        return (rise + (set - rise) / 2).wrapToDay()
    }

    private fun crossing(date: LocalDate, latitude: Double, longitude: Double, zenith: Double, rising: Boolean): Double? {
        val longitudeHours = longitude / DEGREES_PER_HOUR
        val approximateDay = date.dayOfYear + ((if (rising) 6.0 else 18.0) - longitudeHours) / 24
        val meanAnomaly = 0.9856 * approximateDay - 3.289
        val trueLongitude = (meanAnomaly + 1.916 * sinDeg(meanAnomaly) + 0.020 * sinDeg(2 * meanAnomaly) + 282.634).wrapToCircle()
        val rightAscensionHours = rightAscension(trueLongitude) / DEGREES_PER_HOUR
        val sinDeclination = 0.39782 * sinDeg(trueLongitude)
        val cosDeclination = cosDeg(asinDeg(sinDeclination))
        val cosHourAngle = (cosDeg(zenith) - sinDeclination * sinDeg(latitude)) / (cosDeclination * cosDeg(latitude))
        if (cosHourAngle < -1 || cosHourAngle > 1) {
            return null
        }
        val hourAngle = if (rising) 360 - acosDeg(cosHourAngle) else acosDeg(cosHourAngle)
        val localMeanTime = hourAngle / DEGREES_PER_HOUR + rightAscensionHours - 0.06571 * approximateDay - 6.622
        return (localMeanTime - longitudeHours).wrapToDay()
    }

    private fun rightAscension(trueLongitude: Double): Double {
        val raw = deg(atan(0.91764 * tanDeg(trueLongitude)))
        val longitudeQuadrant = floor(trueLongitude / 90) * 90
        val ascensionQuadrant = floor(raw / 90) * 90
        return raw + longitudeQuadrant - ascensionQuadrant
    }

    private fun Double.wrapToCircle(): Double {
        var degrees = this % 360.0
        if (degrees < 0) {
            degrees += 360.0
        }
        return degrees
    }

    private fun sinDeg(degrees: Double): Double = sin(rad(degrees))

    private fun cosDeg(degrees: Double): Double = cos(rad(degrees))

    private fun tanDeg(degrees: Double): Double = tan(rad(degrees))

    private fun asinDeg(x: Double): Double = deg(asin(x))

    private fun acosDeg(x: Double): Double = deg(acos(x))

    private fun rad(degrees: Double): Double = degrees / 180.0 * PI

    private fun deg(radians: Double): Double = radians / PI * 180.0

    private const val DEGREES_PER_HOUR = 15.0
}
