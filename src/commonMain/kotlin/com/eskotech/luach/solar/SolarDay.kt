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

import com.eskotech.luach.geo.Location
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.toLocalDateTime

data class SolarDay(
    val location: Location,
    val date: LocalDate,
    val model: SolarModel = NoaaSolarModel,
    val horizon: ApparentHorizon = ApparentHorizon(),
) {
    val localNoon: Instant = date.atStartOfDayIn(location.timeZone) + 12.hours

    private val utcDate: LocalDate = (localNoon + (location.longitude * 4).minutes).toLocalDateTime(TimeZone.UTC).date

    val sunrise: Instant?
        get() = sunriseAt(0.0)

    val sunset: Instant?
        get() = sunsetAt(0.0)

    val seaLevelSunrise: Instant?
        get() = sunriseAt(0.0, atElevation = false)

    val seaLevelSunset: Instant?
        get() = sunsetAt(0.0, atElevation = false)

    val civilDawn: Instant?
        get() = sunriseAt(CIVIL_TWILIGHT)

    val nauticalDawn: Instant?
        get() = sunriseAt(NAUTICAL_TWILIGHT)

    val astronomicalDawn: Instant?
        get() = sunriseAt(ASTRONOMICAL_TWILIGHT)

    val civilDusk: Instant?
        get() = sunsetAt(CIVIL_TWILIGHT)

    val nauticalDusk: Instant?
        get() = sunsetAt(NAUTICAL_TWILIGHT)

    val astronomicalDusk: Instant?
        get() = sunsetAt(ASTRONOMICAL_TWILIGHT)

    val solarNoon: Instant?
        get() = place(model.solarNoon(utcDate, location.latitude, location.longitude), meanSolarNoon)

    val solarMidnight: Instant?
        get() {
            val noon = solarNoon ?: return null
            val nextNoon = copy(date = date.plus(1, DateTimeUnit.DAY)).solarNoon ?: return null
            return noon + (nextNoon - noon) / 2
        }

    val meanSolarNoon: Instant
        get() = utcDate.atStartOfDayIn(TimeZone.UTC) + (12.0 - location.longitude / 15).hours

    val temporalHour: Duration?
        get() = temporalHour(seaLevelSunrise, seaLevelSunset)

    fun sunriseAt(depression: Double, atElevation: Boolean = true): Instant? =
        place(model.sunrise(utcDate, location.latitude, location.longitude, zenith(depression, atElevation)), meanSolarNoon - 6.hours)

    fun sunsetAt(depression: Double, atElevation: Boolean = true): Instant? =
        place(model.sunset(utcDate, location.latitude, location.longitude, zenith(depression, atElevation)), meanSolarNoon + 6.hours)

    fun depressionBeforeSunrise(minutes: Double): Double? {
        val target = (seaLevelSunrise ?: return null) - minutes.minutes
        return solve(minutes >= 0, target, ::geometricSunriseAt) { time -> time <= target }
    }

    fun depressionAfterSunset(minutes: Double): Double? {
        val target = (seaLevelSunset ?: return null) + minutes.minutes
        return solve(minutes >= 0, target, ::geometricSunsetAt) { time -> time >= target }
    }

    private fun geometricSunriseAt(depression: Double): Instant? =
        place(model.sunrise(utcDate, location.latitude, location.longitude, ApparentHorizon.GEOMETRIC_HORIZON + depression), meanSolarNoon - 6.hours)

    private fun geometricSunsetAt(depression: Double): Instant? =
        place(model.sunset(utcDate, location.latitude, location.longitude, ApparentHorizon.GEOMETRIC_HORIZON + depression), meanSolarNoon + 6.hours)

    private fun solve(belowHorizon: Boolean, target: Instant, event: (Double) -> Instant?, reachedAt: (Instant) -> Boolean): Double? {
        val horizonDepression = horizon.zenith(0.0) - ApparentHorizon.GEOMETRIC_HORIZON
        fun reached(depression: Double): Boolean = event(depression)?.let(reachedAt) ?: (depression > horizonDepression)
        var low = if (belowHorizon) horizonDepression else -SEARCH_LIMIT
        var high = if (belowHorizon) SEARCH_LIMIT else horizonDepression
        if (!reached(high)) {
            return null
        }
        repeat(SEARCH_STEPS) {
            val middle = (low + high) / 2
            if (reached(middle)) high = middle else low = middle
        }
        val found = event(high) ?: return null
        return if ((found - target).absoluteValue <= SEARCH_TOLERANCE) high else null
    }

    private fun zenith(depression: Double, atElevation: Boolean): Double =
        if (depression == 0.0) horizon.zenith(if (atElevation) location.elevation else 0.0) else ApparentHorizon.GEOMETRIC_HORIZON + depression

    private fun place(utcHours: Double?, nearest: Instant): Instant? {
        if (utcHours == null) {
            return null
        }
        val candidate = utcDate.atStartOfDayIn(TimeZone.UTC) + utcHours.hours
        return listOf(candidate - 1.days, candidate, candidate + 1.days).minBy { (it - nearest).absoluteValue }
    }

    companion object {
        const val CIVIL_TWILIGHT: Double = 6.0

        const val NAUTICAL_TWILIGHT: Double = 12.0

        const val ASTRONOMICAL_TWILIGHT: Double = 18.0

        private const val SEARCH_LIMIT = 90.0

        private const val SEARCH_STEPS = 60

        private val SEARCH_TOLERANCE = 5.seconds

        fun temporalHour(start: Instant?, end: Instant?): Duration? {
            if (start == null || end == null) {
                return null
            }
            return (end - start) / 12
        }
    }
}
