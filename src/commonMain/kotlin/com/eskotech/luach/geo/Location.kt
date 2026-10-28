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

package com.eskotech.luach.geo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.datetime.TimeZone

data class Location(
    val latitude: Double,
    val longitude: Double,
    val timeZone: TimeZone,
    val elevation: Double = 0.0,
    val name: String? = null,
    val inIsrael: Boolean = false,
    val mukefChoma: Boolean = false,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude $latitude is outside -90..90" }
        require(longitude in -180.0..180.0) { "longitude $longitude is outside -180..180" }
        require(elevation.isFinite() && elevation >= 0.0) { "elevation $elevation must be a non-negative number of metres" }
    }

    fun geodesicTo(other: Location): Geodesic = Geodesic.between(this, other)

    fun rhumbLineTo(other: Location): RhumbLine = RhumbLine.between(this, other)

    companion object {
        fun coordinate(degrees: Int, minutes: Int, seconds: Double, direction: Char): Double {
            require(degrees >= 0 && minutes in 0..59 && seconds >= 0.0 && seconds < 60.0) {
                "$degrees° $minutes′ $seconds″ is not a valid sexagesimal angle"
            }
            val magnitude = degrees + minutes / 60.0 + seconds / 3600.0
            return when (direction.uppercaseChar()) {
                'N', 'E' -> magnitude
                'S', 'W' -> -magnitude
                else -> throw IllegalArgumentException("the direction must be N, S, E or W, not $direction")
            }
        }
    }
}

data class Geodesic(val distance: Double, val initialBearing: Double, val finalBearing: Double) {
    companion object {
        private const val EQUATORIAL_RADIUS = 6378137.0

        private const val POLAR_RADIUS = 6356752.3142

        private const val FLATTENING = 1 / 298.257223563

        private const val TOLERANCE = 1e-12

        private const val MAX_ITERATIONS = 20

        fun between(from: Location, to: Location): Geodesic {
            val deltaLongitude = toRadians(to.longitude - from.longitude)
            val reducedFrom = atan((1 - FLATTENING) * tan(toRadians(from.latitude)))
            val reducedTo = atan((1 - FLATTENING) * tan(toRadians(to.latitude)))
            val sinFrom = sin(reducedFrom)
            val cosFrom = cos(reducedFrom)
            val sinTo = sin(reducedTo)
            val cosTo = cos(reducedTo)

            var lambda = deltaLongitude
            var sinLambda = 0.0
            var cosLambda = 0.0
            var sinSigma = 0.0
            var cosSigma = 0.0
            var sigma = 0.0
            var cosSquaredAlpha = 0.0
            var cosTwiceSigmaMid = 0.0
            var iteration = 0
            while (true) {
                sinLambda = sin(lambda)
                cosLambda = cos(lambda)
                val across = cosTo * sinLambda
                val along = cosFrom * sinTo - sinFrom * cosTo * cosLambda
                sinSigma = sqrt(across * across + along * along)
                if (sinSigma == 0.0) {
                    return Geodesic(0.0, Double.NaN, Double.NaN)
                }
                cosSigma = sinFrom * sinTo + cosFrom * cosTo * cosLambda
                sigma = atan2(sinSigma, cosSigma)
                val sinAlpha = cosFrom * cosTo * sinLambda / sinSigma
                cosSquaredAlpha = 1 - sinAlpha * sinAlpha
                cosTwiceSigmaMid = if (cosSquaredAlpha == 0.0) 0.0 else cosSigma - 2 * sinFrom * sinTo / cosSquaredAlpha
                val c = FLATTENING / 16 * cosSquaredAlpha * (4 + FLATTENING * (4 - 3 * cosSquaredAlpha))
                val previous = lambda
                lambda = deltaLongitude + (1 - c) * FLATTENING * sinAlpha *
                    (sigma + c * sinSigma * (cosTwiceSigmaMid + c * cosSigma * (2 * cosTwiceSigmaMid * cosTwiceSigmaMid - 1)))
                iteration++
                if (abs(lambda - previous) <= TOLERANCE) {
                    break
                }
                if (iteration >= MAX_ITERATIONS) {
                    return Geodesic(Double.NaN, Double.NaN, Double.NaN)
                }
            }

            val uSquared = cosSquaredAlpha * (EQUATORIAL_RADIUS * EQUATORIAL_RADIUS - POLAR_RADIUS * POLAR_RADIUS) / (POLAR_RADIUS * POLAR_RADIUS)
            val a = 1 + uSquared / 16384 * (4096 + uSquared * (-768 + uSquared * (320 - 175 * uSquared)))
            val b = uSquared / 1024 * (256 + uSquared * (-128 + uSquared * (74 - 47 * uSquared)))
            val deltaSigma = b * sinSigma * (
                cosTwiceSigmaMid + b / 4 * (
                    cosSigma * (2 * cosTwiceSigmaMid * cosTwiceSigmaMid - 1) -
                        b / 6 * cosTwiceSigmaMid * (4 * sinSigma * sinSigma - 3) * (4 * cosTwiceSigmaMid * cosTwiceSigmaMid - 3)
                    )
                )
            val distance = POLAR_RADIUS * a * (sigma - deltaSigma)
            val initial = toDegrees(atan2(cosTo * sinLambda, cosFrom * sinTo - sinFrom * cosTo * cosLambda))
            val final = toDegrees(atan2(cosFrom * sinLambda, cosFrom * sinTo * cosLambda - sinFrom * cosTo))
            return Geodesic(distance, initial, final)
        }
    }
}

data class RhumbLine(val distance: Double, val bearing: Double) {
    companion object {
        private const val SPHERE_RADIUS = 6378137.0

        fun between(from: Location, to: Location): RhumbLine {
            val fromLatitude = toRadians(from.latitude)
            val toLatitude = toRadians(to.latitude)
            var deltaLongitude = toRadians(to.longitude - from.longitude)
            val stretchedLatitude = ln(tan(toLatitude / 2 + PI / 4) / tan(fromLatitude / 2 + PI / 4))
            if (abs(deltaLongitude) > PI) {
                deltaLongitude = if (deltaLongitude > 0) deltaLongitude - 2 * PI else deltaLongitude + 2 * PI
            }
            val bearing = toDegrees(atan2(deltaLongitude, stretchedLatitude))
            val deltaLatitude = toLatitude - fromLatitude
            val ratio = deltaLatitude / stretchedLatitude
            val easting = if (ratio.isFinite()) ratio else cos(fromLatitude)
            val angle = sqrt(deltaLatitude * deltaLatitude + easting * easting * deltaLongitude * deltaLongitude)
            return RhumbLine(angle * SPHERE_RADIUS, bearing)
        }
    }
}

internal fun toRadians(degrees: Double): Double = degrees / 180.0 * PI

internal fun toDegrees(radians: Double): Double = radians / PI * 180.0
