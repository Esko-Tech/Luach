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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

object NoaaSolarModel : SolarModel {

    override val name: String = "NOAA solar position"

    override fun sunrise(date: LocalDate, latitude: Double, longitude: Double, zenith: Double): Double? =
        horizonCrossing(date, latitude, longitude, zenith, rising = true)

    override fun sunset(date: LocalDate, latitude: Double, longitude: Double, zenith: Double): Double? =
        horizonCrossing(date, latitude, longitude, zenith, rising = false)

    override fun solarNoon(date: LocalDate, latitude: Double, longitude: Double): Double? =
        (noonMinutes(julianDay(date), longitude) / 60).wrapToDay().orNullIfNaN()

    fun elevation(utcTime: LocalDateTime, latitude: Double, longitude: Double): Double {
        val position = Position(utcTime, latitude, longitude)
        return deg(asin(sin(position.latitude) * sin(position.declination) + cos(position.latitude) * cos(position.declination) * cos(position.hourAngle)))
    }

    fun azimuth(utcTime: LocalDateTime, latitude: Double, longitude: Double): Double {
        val position = Position(utcTime, latitude, longitude)
        val azimuth = deg(atan2(sin(position.hourAngle), cos(position.hourAngle) * sin(position.latitude) - tan(position.declination) * cos(position.latitude)))
        return (azimuth + 180).wrapToCircle()
    }

    private class Position(utcTime: LocalDateTime, latitudeDegrees: Double, longitudeDegrees: Double) {
        val latitude = rad(latitudeDegrees)
        val declination: Double
        val hourAngle: Double

        init {
            val minutesOfDay = utcTime.hour * 60.0 + utcTime.minute + utcTime.second / 60.0 + utcTime.nanosecond / 60e9
            val centuries = julianCenturies(julianDay(utcTime.date) + minutesOfDay / MINUTES_PER_DAY)
            val trueSolarMinutes = minutesOfDay + equationOfTime(centuries) + 4 * longitudeDegrees
            val hourAngleDegrees = trueSolarMinutes / 4 - 180
            declination = rad(declination(centuries))
            hourAngle = rad(hourAngleDegrees)
        }
    }

    private fun horizonCrossing(date: LocalDate, latitude: Double, longitude: Double, zenith: Double, rising: Boolean): Double? {
        val day = julianDay(date)
        val noon = noonMinutes(day, longitude)
        val firstEstimate = crossingMinutes(julianCenturies(day + noon / MINUTES_PER_DAY), latitude, longitude, zenith, rising)
        if (firstEstimate.isNaN()) {
            return null
        }
        val refined = crossingMinutes(julianCenturies(day + firstEstimate / MINUTES_PER_DAY), latitude, longitude, zenith, rising)
        return (refined / 60).wrapToDay().orNullIfNaN()
    }

    private fun crossingMinutes(centuries: Double, latitude: Double, longitude: Double, zenith: Double, rising: Boolean): Double {
        val hourAngle = hourAngle(latitude, declination(centuries), zenith)
        val signedHourAngle = if (rising) hourAngle else -hourAngle
        return 720 - 4 * (longitude + signedHourAngle) - equationOfTime(centuries)
    }

    private fun noonMinutes(day: Double, longitude: Double): Double {
        val approximateCenturies = julianCenturies(day - longitude / 360)
        val firstEstimate = 720 - 4 * longitude - equationOfTime(approximateCenturies)
        val refinedCenturies = julianCenturies(day - 0.5 + firstEstimate / MINUTES_PER_DAY)
        return 720 - 4 * longitude - equationOfTime(refinedCenturies)
    }

    private fun hourAngle(latitude: Double, declination: Double, zenith: Double): Double {
        val phi = rad(latitude)
        val delta = rad(declination)
        return deg(acos(cos(rad(zenith)) / (cos(phi) * cos(delta)) - tan(phi) * tan(delta)))
    }

    private fun julianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.month.number
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val century = year / 100
        val gregorianCorrection = 2 - century + century / 4
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + date.day + gregorianCorrection - 1524.5
    }

    private fun julianCenturies(julianDay: Double): Double = (julianDay - J2000) / DAYS_PER_CENTURY

    private fun meanLongitude(t: Double): Double = (280.46646 + t * (36000.76983 + 0.0003032 * t)).wrapToCircle()

    private fun meanAnomaly(t: Double): Double = 357.52911 + t * (35999.05029 - 0.0001537 * t)

    private fun orbitEccentricity(t: Double): Double = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

    private fun equationOfCenter(t: Double): Double {
        val m = rad(meanAnomaly(t))
        return sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin(2 * m) * (0.019993 - 0.000101 * t) + sin(3 * m) * 0.000289
    }

    private fun apparentLongitude(t: Double): Double {
        val trueLongitude = meanLongitude(t) + equationOfCenter(t)
        return trueLongitude - 0.00569 - 0.00478 * sin(rad(moonNode(t)))
    }

    private fun moonNode(t: Double): Double = 125.04 - 1934.136 * t

    private fun obliquity(t: Double): Double {
        val seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))
        val mean = 23 + (26 + seconds / 60) / 60
        return mean + 0.00256 * cos(rad(moonNode(t)))
    }

    private fun declination(t: Double): Double =
        deg(asin(sin(rad(obliquity(t))) * sin(rad(apparentLongitude(t)))))

    private fun equationOfTime(t: Double): Double {
        val y = tan(rad(obliquity(t)) / 2).let { it * it }
        val l0 = rad(meanLongitude(t))
        val e = orbitEccentricity(t)
        val m = rad(meanAnomaly(t))
        val radians = y * sin(2 * l0) - 2 * e * sin(m) + 4 * e * y * sin(m) * cos(2 * l0) -
            0.5 * y * y * sin(4 * l0) - 1.25 * e * e * sin(2 * m)
        return deg(radians) * 4
    }

    private fun Double.wrapToCircle(): Double {
        var degrees = this % 360.0
        if (degrees < 0) {
            degrees += 360.0
        }
        return degrees
    }

    private fun rad(degrees: Double): Double = degrees / 180.0 * PI

    private fun deg(radians: Double): Double = radians / PI * 180.0

    private const val J2000 = 2451545.0

    private const val DAYS_PER_CENTURY = 36525.0

    private const val MINUTES_PER_DAY = 1440.0
}
