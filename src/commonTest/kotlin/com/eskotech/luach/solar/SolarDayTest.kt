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

import com.eskotech.luach.Mismatches
import com.eskotech.luach.Places
import com.eskotech.luach.assertInstant
import com.eskotech.luach.geo.Location
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class SolarDayTest {

    private val fixture = SolarDay(Places.LAKEWOOD, LocalDate(2017, 10, 17))

    @Test
    fun twilights() {
        assertInstant("civilDawn", "2017-10-17T10:42:27.439221886Z", fixture.civilDawn)
        assertInstant("nauticalDawn", "2017-10-17T10:10:57.242471901Z", fixture.nauticalDawn)
        assertInstant("astronomicalDawn", "2017-10-17T09:39:33.523030241Z", fixture.astronomicalDawn)
        assertInstant("civilDusk", "2017-10-17T22:41:21.435143220Z", fixture.civilDusk)
        assertInstant("nauticalDusk", "2017-10-17T23:12:49.151356447Z", fixture.nauticalDusk)
        assertInstant("astronomicalDusk", "2017-10-17T23:44:09.707296295Z", fixture.astronomicalDusk)
        assertEquals(fixture.civilDawn, fixture.sunriseAt(6.0))
        assertEquals(fixture.civilDusk, fixture.sunsetAt(6.0))
        assertEquals(fixture.sunrise, fixture.sunriseAt(0.0))
        assertEquals(fixture.seaLevelSunset, fixture.sunsetAt(0.0, atElevation = false))
        assertEquals(6.0, SolarDay.CIVIL_TWILIGHT)
        assertEquals(12.0, SolarDay.NAUTICAL_TWILIGHT)
        assertEquals(18.0, SolarDay.ASTRONOMICAL_TWILIGHT)
    }

    @Test
    fun elevationOnlyMovesTheHorizonEvents() {
        assertTrue(fixture.sunrise!! < fixture.seaLevelSunrise!!)
        assertTrue(fixture.sunset!! > fixture.seaLevelSunset!!)
        assertEquals(fixture.sunriseAt(16.1), fixture.sunriseAt(16.1, atElevation = false))
        val flat = SolarDay(Places.LAKEWOOD.copy(elevation = 0.0), LocalDate(2017, 10, 17))
        assertEquals(flat.sunrise, fixture.seaLevelSunrise)
        assertEquals(flat.sunset, fixture.seaLevelSunset)
        val geometric = fixture.copy(horizon = ApparentHorizon(refraction = 0.0, solarRadius = 0.0, earthRadius = 6356.9))
        assertEquals(geometric.seaLevelSunrise, fixture.sunriseAt(0.0).let { fixture.copy(horizon = ApparentHorizon(0.0, 0.0)).seaLevelSunrise })
        assertTrue(geometric.seaLevelSunrise!! > fixture.seaLevelSunrise!!)
    }

    @Test
    fun polarDayHasNoHorizonEvents() {
        val summer = SolarDay(Places.NORTHERN_NORWAY, LocalDate(2017, 6, 21))
        assertNull(summer.sunrise)
        assertNull(summer.sunset)
        assertNull(summer.temporalHour)
        assertTrue(summer.solarNoon != null)
        assertNull(summer.copy(model = AlmanacSolarModel).solarNoon)
        val winter = SolarDay(Places.NORTHERN_NORWAY, LocalDate(2024, 12, 21))
        assertNull(winter.sunrise)
        assertTrue(winter.sunriseAt(16.1) != null)
    }

    @Test
    fun swappingTheModelChangesTheResult() {
        assertNotEquals(fixture.sunset, fixture.copy(model = AlmanacSolarModel).sunset)
        assertTrue(abs((fixture.sunset!! - fixture.copy(model = AlmanacSolarModel).sunset!!).inWholeSeconds) < 120)
    }

    @Test
    fun textbookLocalTimes() {
        val newYork = Location(40.7128, -74.0060, TimeZone.of("America/New_York"))
        fun local(day: SolarDay, instant: Instant?): String = instant!!.toLocalDateTime(day.location.timeZone).toString()
        val summer = SolarDay(newYork, LocalDate(2024, 6, 20))
        assertEquals("2024-06-20T05:24", local(summer, summer.sunrise).substring(0, 16))
        assertEquals("2024-06-20T20:30", local(summer, summer.sunset).substring(0, 16))
        val winter = SolarDay(newYork, LocalDate(2024, 12, 21))
        assertEquals("2024-12-21T07:16", local(winter, winter.sunrise).substring(0, 16))
        assertEquals("2024-12-21T16:32", local(winter, winter.sunset).substring(0, 16))
        val jerusalem = SolarDay(Places.JERUSALEM, LocalDate(2024, 6, 21))
        assertEquals("2024-06-21T05:3", local(jerusalem, jerusalem.seaLevelSunrise).substring(0, 15))
        assertEquals("2024-06-21T19:4", local(jerusalem, jerusalem.seaLevelSunset).substring(0, 15))
    }

    @Test
    fun eventsFallOnTheRequestedLocalDate() {
        val mismatches = Mismatches()
        val places = Places.SOLAR_SET + listOf(Places.APIA, Places.KIRITIMATI, Places.BAKER_ISLAND)
        val dates = listOf(
            LocalDate(2024, 3, 20), LocalDate(2024, 6, 20), LocalDate(2024, 12, 21), LocalDate(2017, 10, 17),
            LocalDate(2011, 12, 31), LocalDate(1995, 1, 1), LocalDate(2024, 3, 10), LocalDate(2024, 11, 3),
        )
        for (location in places) {
            for (date in dates) {
                val day = SolarDay(location, date)
                fun check(label: String, instant: Instant?) {
                    val local = instant?.toLocalDateTime(location.timeZone)
                    mismatches.check(local != null && local.date == date) { "${location.name} $date $label: $instant is $local" }
                }
                check("sunrise", day.sunrise)
                check("sunset", day.sunset)
                check("seaLevelSunrise", day.seaLevelSunrise)
                check("seaLevelSunset", day.seaLevelSunset)
                check("solarNoon", day.solarNoon)
                check("meanSolarNoon", day.meanSolarNoon)
                check("civilDawn", day.civilDawn)
                check("civilDusk", day.civilDusk)
                val sunrise = day.sunrise ?: continue
                val sunset = day.sunset ?: continue
                mismatches.check(sunrise < day.solarNoon!! && day.solarNoon!! < sunset) { "${location.name} $date: order" }
                mismatches.check(day.civilDawn!! < sunrise && sunset < day.civilDusk!!) { "${location.name} $date: twilight order" }
                mismatches.check((sunrise < day.seaLevelSunrise!!) == (location.elevation > 0)) { "${location.name} $date: elevation makes sunrise earlier" }
            }
        }
        mismatches.assertNone("local dates")
    }

    @Test
    fun solarNoonIsNearTheMidpointOfTheDay() {
        val mismatches = Mismatches()
        for (location in Places.SOLAR_SET) {
            for (date in listOf(LocalDate(2024, 3, 20), LocalDate(2024, 6, 20), LocalDate(2024, 9, 22), LocalDate(2024, 12, 21))) {
                for (model in listOf(NoaaSolarModel, AlmanacSolarModel)) {
                    val day = SolarDay(location, date, model)
                    val rise = day.seaLevelSunrise!!
                    val set = day.seaLevelSunset!!
                    val midpoint = rise + (set - rise) / 2
                    val tolerance = if (model == NoaaSolarModel) 90.seconds else 5.seconds
                    mismatches.check((day.solarNoon!! - midpoint).absoluteValue <= tolerance) { "${location.name} $date ${model.name}: noon ${day.solarNoon} midpoint $midpoint" }
                    mismatches.check((day.meanSolarNoon - day.solarNoon!!).absoluteValue <= 17.minutes) { "${location.name} $date: mean solar noon ${day.meanSolarNoon}" }
                }
            }
        }
        mismatches.assertNone("solar noon")
    }

    @Test
    fun solarMidnightIsHalfwayToTomorrowsNoon() {
        val mismatches = Mismatches()
        for (location in Places.SOLAR_SET) {
            for (date in listOf(LocalDate(2024, 3, 20), LocalDate(2024, 6, 20), LocalDate(2024, 12, 21))) {
                val day = SolarDay(location, date)
                val tomorrow = SolarDay(location, date.plus(1, DateTimeUnit.DAY))
                val expected = day.solarNoon!! + (tomorrow.solarNoon!! - day.solarNoon!!) / 2
                mismatches.check(day.solarMidnight == expected) { "${location.name} $date: midnight ${day.solarMidnight} expected $expected" }
            }
        }
        mismatches.assertNone("solar midnight")
    }

    @Test
    fun depressionSearchRoundTrips() {
        val mismatches = Mismatches()
        for (location in Places.SOLAR_SET) {
            for (date in listOf(LocalDate(2024, 3, 20), LocalDate(2024, 6, 20), LocalDate(2024, 12, 21), LocalDate(2017, 10, 17))) {
                val day = SolarDay(location, date)
                for (minutes in listOf(72.0, 30.0, -2.0, -20.0)) {
                    val dawn = day.depressionBeforeSunrise(minutes)
                    val dawnTarget = day.seaLevelSunrise!! - minutes.minutes
                    val dawnActual = day.sunriseAt(dawn!!, atElevation = false)!!
                    mismatches.check((dawnActual - dawnTarget).absoluteValue <= 1.seconds) { "${location.name} $date sunrise $minutes minutes = $dawn ($dawnActual vs $dawnTarget)" }
                    val dusk = day.depressionAfterSunset(minutes)
                    val duskTarget = day.seaLevelSunset!! + minutes.minutes
                    val duskActual = day.sunsetAt(dusk!!, atElevation = false)!!
                    mismatches.check((duskActual - duskTarget).absoluteValue <= 1.seconds) { "${location.name} $date sunset $minutes minutes = $dusk ($duskActual vs $duskTarget)" }
                }
            }
        }
        mismatches.assertNone("depression search")
        val apparentHorizon = ApparentHorizon().zenith(0.0) - ApparentHorizon.GEOMETRIC_HORIZON
        val justAfterSunrise = SolarDay(Places.LAKEWOOD, LocalDate(2024, 6, 20)).depressionBeforeSunrise(-2.0)!!
        assertTrue(justAfterSunrise > 0.0 && justAfterSunrise < apparentHorizon, "$justAfterSunrise")
        assertNull(SolarDay(Places.TROMSO, LocalDate(2024, 6, 1)).depressionAfterSunset(600.0))
        assertNull(SolarDay(Places.NORTHERN_NORWAY, LocalDate(2024, 6, 21)).depressionBeforeSunrise(10.0))
    }

    @Test
    fun eventsBracketOneSolarNoonInAnyZone() {
        val mismatches = Mismatches()
        val places = listOf(
            Location(0.0, 180.0, TimeZone.of("UTC"), 0.0, "180 in UTC"),
            Location(0.0, -180.0, TimeZone.of("UTC"), 0.0, "-180 in UTC"),
            Location(34.05, -118.24, TimeZone.of("Asia/Tokyo"), 0.0, "Los Angeles in the Tokyo zone"),
            Location(34.69, 135.5, TimeZone.of("America/Los_Angeles"), 0.0, "Osaka in the Los Angeles zone"),
            Places.LAKEWOOD, Places.APIA, Places.KIRITIMATI, Places.BAKER_ISLAND,
        )
        for (location in places) for (date in listOf(LocalDate(2024, 3, 20), LocalDate(2024, 6, 21), LocalDate(2024, 12, 21))) {
            val day = SolarDay(location, date)
            val sunrise = day.seaLevelSunrise!!
            val sunset = day.seaLevelSunset!!
            val noon = day.solarNoon!!
            mismatches.check(sunrise < noon && noon < sunset) { "${location.name} $date: $sunrise $noon $sunset" }
            mismatches.check((noon - day.meanSolarNoon).absoluteValue <= 20.minutes) { "${location.name} $date: noon $noon vs mean ${day.meanSolarNoon}" }
            mismatches.check(day.temporalHour!! > 30.minutes) { "${location.name} $date: ${day.temporalHour}" }
        }
        mismatches.assertNone("events bracket one solar noon")
    }

    @Test
    fun temporalHour() {
        assertEquals((fixture.seaLevelSunset!! - fixture.seaLevelSunrise!!) / 12, fixture.temporalHour)
        assertNull(SolarDay.temporalHour(null, fixture.sunset))
        assertNull(SolarDay.temporalHour(fixture.sunrise, null))
        assertEquals(1.minutes, SolarDay.temporalHour(Instant.parse("2024-06-20T12:00:00Z"), Instant.parse("2024-06-20T12:12:00Z")))
    }
}
