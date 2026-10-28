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

package com.eskotech.luach

import com.eskotech.luach.solar.AlmanacSolarModel
import com.eskotech.luach.solar.ApparentHorizon
import com.eskotech.luach.solar.NoaaSolarModel
import com.eskotech.luach.solar.SolarDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

class SolarSnapshotTest {
    private val getters = Getters.zeroArg(SolarDay::class.java)

    private fun day(place: Place, date: LocalDate, model: Model) = SolarDay(place.location(), date, model.model)

    @Test
    fun zeroArgGetters() {
        assertEquals(14, getters.size, "SolarDay zero-arg getters: ${getters.map { it.name }}")
        Snapshot.collect("SolarDay.zeroArgGetters", SampleDates.ZMANIM_SAMPLES) { s, (place, date) ->
            for (model in Model.entries) {
                s.group = "$place $model"
                s.recordGetters(getters, day(place, date, model))
            }
        }.assertMatches()
    }

    @Test
    fun eventsAroundTheDateLineStayOnTheRequestedDay() {
        for ((place, date) in listOf(SamplePlaces.APIA to LocalDate(2011, 12, 31), SamplePlaces.KIRITIMATI to LocalDate(1995, 1, 1))) {
            for (model in Model.entries) {
                val zone = TimeZone.of(place.zoneId)
                val k = day(place, date, model)
                for ((name, value) in listOf("sunrise" to k.sunrise, "seaLevelSunrise" to k.seaLevelSunrise, "sunset" to k.sunset, "seaLevelSunset" to k.seaLevelSunset)) {
                    val time = checkNotNull(value) { "$place $date $model $name is null" }
                    assertEquals(date, time.toLocalDateTime(zone).date, "$place $date $model $name left the requested day")
                }
            }
        }
    }

    @Test
    fun eventsAtDepressions() {
        Snapshot.collect("SolarDay.eventsAtDepressions", SampleDates.ZMANIM_SAMPLES) { s, (place, date) ->
            for (model in Model.entries) {
                s.group = "$place $model"
                val k = day(place, date, model)
                for (depression in DEPRESSIONS) for (atElevation in listOf(true, false)) {
                    s.recordOutcome("sunriseAt") { k.sunriseAt(depression, atElevation) }
                    s.recordOutcome("sunsetAt") { k.sunsetAt(depression, atElevation) }
                }
            }
        }.assertMatches()
    }

    @Test
    fun temporalHoursWithArbitraryBounds() {
        Snapshot.collect("SolarDay.temporalHoursWithArbitraryBounds", SampleDates.ZMANIM_SAMPLES) { s, (place, date) ->
            for (model in Model.entries) {
                s.group = "$place $model"
                val k = day(place, date, model)
                val bounds: List<Instant?> = listOf(k.seaLevelSunrise, k.seaLevelSunset, k.sunrise, k.sunset, k.astronomicalDawn, k.astronomicalDusk, k.solarNoon, null)
                for (start in bounds) for (end in bounds) {
                    s.recordOutcome("temporalHour") { SolarDay.temporalHour(start, end) }
                }
            }
        }.assertMatches()
    }

    @Test
    fun depressionSearch() {
        val cases = listOf(
            Triple(SamplePlaces.JERUSALEM, LocalDate(2024, 3, 20), 72.0),
            Triple(SamplePlaces.LAKEWOOD, LocalDate(2024, 6, 21), 50.0),
            Triple(SamplePlaces.SYDNEY, LocalDate(2024, 12, 21), -30.0),
            Triple(SamplePlaces.TROMSO, LocalDate(2024, 6, 1), 600.0),
        )
        Snapshot.single("SolarDay.depressionSearch") { s ->
            for ((place, date, minutes) in cases) for (model in Model.entries) {
                s.group = "$place $model"
                val k = day(place, date, model)
                s.recordOutcome("depressionBeforeSunrise") { k.depressionBeforeSunrise(minutes) }
                s.recordOutcome("depressionAfterSunset") { k.depressionAfterSunset(minutes) }
            }
        }.assertMatches()
    }

    private val modelSamples: List<Pair<Place, LocalDate>> = SamplePlaces.ALL.flatMap { place ->
        (SampleDates.LEAP_YEAR_DAYS + SampleDates.SPOT.filter { it.year != SampleDates.LEAP_YEAR }).map { place to it }
    }

    @Test
    fun modelUtcHours() {
        Snapshot.collect("SolarModel.utcHours", modelSamples) { s, (place, date) ->
            for (model in Model.entries) {
                s.group = "$place $model"
                val k = model.model
                for (zenith in ZENITHS) {
                    s.record("sunrise", k.sunrise(date, place.latitude, place.longitude, zenith))
                    s.record("sunset", k.sunset(date, place.latitude, place.longitude, zenith))
                }
                s.record("solarNoon", k.solarNoon(date, place.latitude, place.longitude))
            }
        }.assertMatches()
    }

    @Test
    fun horizonVariants() {
        val horizons = listOf(ApparentHorizon(0.5, 16 / 60.0, 6356.9), ApparentHorizon(34.478885263888294 / 60, 0.27, 6371.0), ApparentHorizon(0.0, 0.0, 6378.137))
        val dates = SampleDates.LEAP_YEAR_DAYS.filter { it.day % 5 == 1 } + SampleDates.SPOT
        Snapshot.single("SolarModel.horizonVariants") { s ->
            s.group = "defaults"
            s.record("name", NoaaSolarModel.name)
            s.record("name", AlmanacSolarModel.name)
            s.record("horizon", listOf(ApparentHorizon().refraction, ApparentHorizon().solarRadius, ApparentHorizon().earthRadius))
            for (horizon in horizons) for (place in SamplePlaces.ALL) for (date in dates) for (model in Model.entries) {
                s.group = "$model $horizon"
                val k = SolarDay(place.location(), date, model.model, horizon)
                s.record("sunrise", k.sunrise)
                s.record("seaLevelSunrise", k.seaLevelSunrise)
                s.record("sunset", k.sunset)
                s.record("seaLevelSunset", k.seaLevelSunset)
                s.record("solarNoon", k.solarNoon)
                s.record("dip", horizon.dip(place.elevation))
            }
        }.assertMatches()
    }

    @Test
    fun solarElevationAndAzimuth() {
        val dates = SampleDates.LEAP_YEAR_DAYS.filter { it.day == 1 || it.day == 21 } + SampleDates.SPOT
        Snapshot.collect("SolarModel.elevationAndAzimuth", SamplePlaces.ALL) { s, place ->
            for (date in dates) for (hour in 0..23) for ((minute, second) in listOf(0 to 0, 17 to 30, 45 to 59)) {
                val time = LocalDateTime(date.year, date.month.number, date.day, hour, minute, second)
                s.record("elevation", NoaaSolarModel.elevation(time, place.latitude, place.longitude))
                s.record("azimuth", NoaaSolarModel.azimuth(time, place.latitude, place.longitude))
            }
        }.assertMatches()
    }

    companion object {
        val DEPRESSIONS = listOf(
            -45.0, -10.0, -6.0, -1.0, -0.5, 0.0, 0.5, 0.8333, 1.0, 2.25, 3.0, 4.0, 5.5, 6.0, 7.0, 8.0, 9.0,
            10.0, 11.8, 12.0, 14.0, 16.0, 16.1, 17.0, 17.8, 18.0, 19.0, 19.8, 20.0, 22.0, 25.0, 30.0,
        )

        val ZENITHS = listOf(90.0, 90.8333, 96.0, 102.0, 108.0, 106.1, 109.8, 95.5)
    }
}
