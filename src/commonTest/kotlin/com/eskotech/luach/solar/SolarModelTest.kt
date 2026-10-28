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
import com.eskotech.luach.geo.Location
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class SolarModelTest {

    private fun location(name: String): Location = when (name) {
        "lakewood" -> Places.LAKEWOOD
        "jerusalem" -> Places.JERUSALEM
        "sydney" -> Places.SYDNEY
        "macapa" -> Places.MACAPA
        "suva" -> Places.SUVA
        "ushuaia" -> Places.USHUAIA
        "norway" -> Places.NORTHERN_NORWAY
        else -> throw IllegalArgumentException(name)
    }

    private fun hours(text: String): Double? = if (text == "NaN") null else text.toDouble()

    private fun matches(expected: Double?, actual: Double?): Boolean =
        if (expected == null || actual == null) expected == actual else abs(expected - actual) <= 1e-7

    private fun zenith(requested: Double, location: Location, atElevation: Boolean): Double =
        if (requested == 90.0) ApparentHorizon().zenith(if (atElevation) location.elevation else 0.0) else requested

    @Test
    fun modelsMatchIndependentReference() {
        val mismatches = Mismatches()
        var compared = 0
        for (row in REFERENCE) {
            val parts = row.split(" ")
            val location = location(parts[0])
            val date = LocalDate.parse(parts[1])
            if (parts[2] == "noon") {
                compared++
                val actual = NoaaSolarModel.solarNoon(date, location.latitude, location.longitude)
                mismatches.check(matches(hours(parts[4]), actual)) { "$row: noaa noon $actual" }
                continue
            }
            val zenith = zenith(parts[2].toDouble(), location, parts[3] == "elev")
            val rising = parts[4] == "rise"
            fun compute(model: SolarModel): Double? =
                if (rising) model.sunrise(date, location.latitude, location.longitude, zenith)
                else model.sunset(date, location.latitude, location.longitude, zenith)
            compared += 2
            val noaa = compute(NoaaSolarModel)
            val almanac = compute(AlmanacSolarModel)
            mismatches.check(matches(hours(parts[6]), noaa)) { "$row: noaa $noaa" }
            mismatches.check(matches(hours(parts[8]), almanac)) { "$row: almanac $almanac" }
        }
        assertTrue(compared > 300, "compared $compared")
        mismatches.assertNone("solar reference")
    }

    @Test
    fun modelsAgreeWithEachOtherAwayFromThePoles() {
        val mismatches = Mismatches()
        for (row in REFERENCE) {
            val parts = row.split(" ")
            if (parts[2] == "noon" || parts[0] == "norway") {
                continue
            }
            val location = location(parts[0])
            val date = LocalDate.parse(parts[1])
            val zenith = zenith(parts[2].toDouble(), location, true)
            val rising = parts[4] == "rise"
            fun compute(model: SolarModel): Double? =
                if (rising) model.sunrise(date, location.latitude, location.longitude, zenith)
                else model.sunset(date, location.latitude, location.longitude, zenith)
            val a = compute(NoaaSolarModel)
            val b = compute(AlmanacSolarModel)
            if (a == null && b == null) {
                continue
            }
            mismatches.check(a != null && b != null) { "$row: only one model has the event" }
            if (a == null || b == null) continue
            var difference = abs(a - b)
            if (difference > 12) {
                difference = 24 - difference
            }
            mismatches.check(difference * 60 <= 2.0) { "$row: noaa $a almanac $b differ by ${difference * 60} minutes" }
        }
        mismatches.assertNone("model agreement")
    }

    @Test
    fun textbookValues() {
        val apparent = ApparentHorizon().zenith(0.0)
        assertEquals(12.123957659, NoaaSolarModel.solarNoon(LocalDate(2024, 3, 20), 0.0, 0.0)!!, 1e-6)
        assertEquals(6.067179247, NoaaSolarModel.sunrise(LocalDate(2024, 3, 20), 0.0, 0.0, apparent)!!, 1e-6)
        assertEquals(18.175792584, NoaaSolarModel.sunset(LocalDate(2024, 3, 20), 0.0, 0.0, apparent)!!, 1e-6)
        assertEquals(11.725149780, NoaaSolarModel.solarNoon(LocalDate(2024, 11, 3), 0.0, 0.0)!!, 1e-6)
        assertEquals(12.237091956, NoaaSolarModel.solarNoon(LocalDate(2024, 2, 11), 0.0, 0.0)!!, 1e-6)
        assertEquals(9.415016283, NoaaSolarModel.sunrise(LocalDate(2024, 6, 20), 40.7128, -74.0060, apparent)!!, 1e-6)
        assertEquals(0.510916288, NoaaSolarModel.sunset(LocalDate(2024, 6, 20), 40.7128, -74.0060, apparent)!!, 1e-6)
        assertEquals(12.280481626, NoaaSolarModel.sunrise(LocalDate(2024, 12, 21), 40.7128, -74.0060, apparent)!!, 1e-6)
        assertEquals(21.534500835, NoaaSolarModel.sunset(LocalDate(2024, 12, 21), 40.7128, -74.0060, apparent)!!, 1e-6)
        assertEquals(10.70891556, NoaaSolarModel.sunrise(LocalDate(2017, 10, 17), Places.WOODCLIFF_LAKE.latitude, Places.WOODCLIFF_LAKE.longitude, 96.0)!!, 1e-8)
        assertEquals(10.17562194, NoaaSolarModel.sunrise(LocalDate(2017, 10, 17), Places.WOODCLIFF_LAKE.latitude, Places.WOODCLIFF_LAKE.longitude, 102.0)!!, 1e-8)
        assertEquals(9.64406486, NoaaSolarModel.sunrise(LocalDate(2017, 10, 17), Places.WOODCLIFF_LAKE.latitude, Places.WOODCLIFF_LAKE.longitude, 108.0)!!, 1e-8)
        assertEquals(19.27827196, NoaaSolarModel.sunrise(LocalDate(2020, 2, 29), Places.SYDNEY.latitude, Places.SYDNEY.longitude, 96.0)!!, 1e-8)
        assertEquals(10.70056986, AlmanacSolarModel.sunrise(LocalDate(2017, 10, 17), Places.WOODCLIFF_LAKE.latitude, Places.WOODCLIFF_LAKE.longitude, 96.0)!!, 1e-8)
        assertEquals(10.16784691, AlmanacSolarModel.sunrise(LocalDate(2017, 10, 17), Places.WOODCLIFF_LAKE.latitude, Places.WOODCLIFF_LAKE.longitude, 102.0)!!, 1e-8)
        assertEquals(9.63665090, AlmanacSolarModel.sunrise(LocalDate(2017, 10, 17), Places.WOODCLIFF_LAKE.latitude, Places.WOODCLIFF_LAKE.longitude, 108.0)!!, 1e-8)
        assertEquals(19.28346936, AlmanacSolarModel.sunrise(LocalDate(2020, 2, 29), Places.SYDNEY.latitude, Places.SYDNEY.longitude, 96.0)!!, 1e-8)
        val norway = Places.NORTHERN_NORWAY
        assertNull(NoaaSolarModel.sunrise(LocalDate(2017, 6, 21), norway.latitude, norway.longitude, apparent))
        assertNull(NoaaSolarModel.sunset(LocalDate(2017, 6, 21), norway.latitude, norway.longitude, apparent))
        assertNull(AlmanacSolarModel.sunrise(LocalDate(2017, 6, 21), norway.latitude, norway.longitude, apparent))
        assertNull(AlmanacSolarModel.sunset(LocalDate(2017, 6, 21), norway.latitude, norway.longitude, apparent))
        assertNull(AlmanacSolarModel.solarNoon(LocalDate(2017, 6, 21), norway.latitude, norway.longitude))
        assertTrue(NoaaSolarModel.solarNoon(LocalDate(2017, 6, 21), norway.latitude, norway.longitude) != null)
        assertEquals("NOAA solar position", NoaaSolarModel.name)
        assertEquals("Almanac for Computers sunrise and sunset", AlmanacSolarModel.name)
    }

    @Test
    fun apparentHorizon() {
        val horizon = ApparentHorizon()
        assertEquals(34 / 60.0, horizon.refraction)
        assertEquals(16 / 60.0, horizon.solarRadius)
        assertEquals(6356.9, horizon.earthRadius)
        assertEquals(0.0, horizon.dip(0.0))
        val dip754 = acos(6356.9 / (6356.9 + 0.754)) * 180 / PI
        assertEquals(dip754, horizon.dip(754.0), 1e-12)
        assertEquals(0.8826, dip754, 0.001)
        assertEquals(90 + 50 / 60.0, horizon.zenith(0.0), 1e-12)
        assertEquals(90 + 50 / 60.0 + dip754, horizon.zenith(754.0), 1e-12)
        val geometric = ApparentHorizon(refraction = 0.0, solarRadius = 0.0)
        assertEquals(90.0, geometric.zenith(0.0))
        val bigger = horizon.copy(earthRadius = 6371.0)
        assertEquals(acos(6371.0 / (6371.0 + 0.754)) * 180 / PI, bigger.dip(754.0), 1e-12)
        assertEquals(90.0, ApparentHorizon.GEOMETRIC_HORIZON)
    }

    @Test
    fun solarElevationAndAzimuth() {
        val lakewood = Places.LAKEWOOD
        val noon = LocalDateTime(2024, 6, 20, 16, 58, 36)
        assertEquals(73.36, NoaaSolarModel.elevation(noon, lakewood.latitude, lakewood.longitude), 0.1)
        assertEquals(180.0, NoaaSolarModel.azimuth(noon, lakewood.latitude, lakewood.longitude), 0.5)
        val winterNoon = LocalDateTime(2024, 12, 21, 16, 55, 0)
        assertEquals(26.5, NoaaSolarModel.elevation(winterNoon, lakewood.latitude, lakewood.longitude), 0.1)
        val midnight = LocalDateTime(2024, 6, 20, 4, 58, 36)
        assertTrue(NoaaSolarModel.elevation(midnight, lakewood.latitude, lakewood.longitude) < -20)
        val morning = LocalDateTime(2024, 6, 20, 11, 0, 0)
        val afternoon = LocalDateTime(2024, 6, 20, 22, 0, 0)
        assertTrue(NoaaSolarModel.azimuth(morning, lakewood.latitude, lakewood.longitude) in 60.0..120.0)
        assertTrue(NoaaSolarModel.azimuth(afternoon, lakewood.latitude, lakewood.longitude) in 240.0..300.0)
        val sydneyTransit = NoaaSolarModel.solarNoon(LocalDate(2024, 12, 21), Places.SYDNEY.latitude, Places.SYDNEY.longitude)!!
        val sydneyNoon = LocalDateTime(2024, 12, 21, sydneyTransit.toInt(), ((sydneyTransit % 1) * 60).toInt(), ((sydneyTransit * 3600) % 60).toInt())
        assertEquals(0.0, NoaaSolarModel.azimuth(sydneyNoon, Places.SYDNEY.latitude, Places.SYDNEY.longitude).let { if (it > 180) it - 360 else it }, 1.0)
        assertTrue(NoaaSolarModel.elevation(sydneyNoon, Places.SYDNEY.latitude, Places.SYDNEY.longitude) > 79.0)
    }

    @Test
    fun solarPositionAtTheExactInstant() {
        val expectations = listOf(
            Triple(Places.LAKEWOOD, LocalDateTime(2024, 3, 20, 23, 30, 0), -4.731554 to 274.434937),
            Triple(Places.LAKEWOOD, LocalDateTime(2024, 9, 22, 22, 15, 0), 6.465145 to 264.323352),
            Triple(Places.LAKEWOOD, LocalDateTime(2024, 3, 20, 16, 0, 0), 47.543688 to 155.821396),
            Triple(Places.JERUSALEM, LocalDateTime(2024, 3, 20, 14, 30, 0), 16.342371 to 259.761107),
            Triple(Places.JERUSALEM, LocalDateTime(2024, 9, 22, 15, 45, 0), -2.769317 to 271.658598),
            Triple(Places.SYDNEY, LocalDateTime(2024, 3, 20, 20, 0, 0), -0.644057 to 90.096580),
            Triple(Places.SYDNEY, LocalDateTime(2024, 9, 22, 21, 30, 0), 21.112327 to 75.172560),
        )
        for ((location, time, expected) in expectations) {
            assertEquals(expected.first, NoaaSolarModel.elevation(time, location.latitude, location.longitude), 0.005, "elevation ${location.name} $time")
            assertEquals(expected.second, NoaaSolarModel.azimuth(time, location.latitude, location.longitude), 0.005, "azimuth ${location.name} $time")
        }
        val lakewood = Places.LAKEWOOD
        val apparentHorizon = ApparentHorizon.GEOMETRIC_HORIZON - ApparentHorizon().zenith(0.0)
        for (hours in listOf(10.991145417, 23.159694402)) {
            val wholeSeconds = (hours * 3600).toInt()
            val instant = LocalDateTime(2024, 3, 20, wholeSeconds / 3600, wholeSeconds / 60 % 60, wholeSeconds % 60, ((hours * 3600 - wholeSeconds) * 1e9).toInt())
            assertEquals(apparentHorizon, NoaaSolarModel.elevation(instant, lakewood.latitude, lakewood.longitude), 0.01, "elevation at the reference horizon crossing $hours")
        }
    }

    private companion object {
        val REFERENCE: List<String> = listOf(
        "lakewood 2024-03-20 noon noaa 17.072274399",
        "lakewood 2024-03-20 90.0 elev rise noaa 10.980312645 usno 10.976974982",
        "lakewood 2024-03-20 90.0 elev set noaa 23.170548116 usno 23.175278475",
        "lakewood 2024-03-20 90.0 sea rise noaa 10.991145417 usno 10.987820315",
        "lakewood 2024-03-20 90.0 sea set noaa 23.159694402 usno 23.164432162",
        "lakewood 2024-03-20 106.1 elev rise noaa 9.648344047 usno 9.643099362",
        "lakewood 2024-03-20 106.1 elev set noaa 0.506021555 usno 0.510005714",
        "lakewood 2024-03-20 98.5 elev rise noaa 10.321907362 usno 10.317730538",
        "lakewood 2024-03-20 98.5 elev set noaa 23.830425531 usno 23.834753999",
        "lakewood 2024-03-20 87.2 elev rise noaa 11.307378874 usno 11.304406298",
        "lakewood 2024-03-20 87.2 elev set noaa 22.842888956 usno 22.847856968",
        "lakewood 2024-06-20 noon noaa 16.976730109",
        "lakewood 2024-06-20 90.0 elev rise noaa 9.453292402 usno 9.452688842",
        "lakewood 2024-06-20 90.0 elev set noaa 0.503846548 usno 0.503292795",
        "lakewood 2024-06-20 90.0 sea rise noaa 9.466109929 usno 9.465505336",
        "lakewood 2024-06-20 90.0 sea set noaa 0.491028799 usno 0.490476294",
        "lakewood 2024-06-20 106.1 elev rise noaa 7.681435702 usno 7.680886221",
        "lakewood 2024-06-20 106.1 elev set noaa 2.275754931 usno 2.275097330",
        "lakewood 2024-06-20 98.5 elev rise noaa 8.636266131 usno 8.635714270",
        "lakewood 2024-06-20 98.5 elev set noaa 1.320890294 usno 1.320268013",
        "lakewood 2024-06-20 87.2 elev rise noaa 9.833134538 usno 9.832498091",
        "lakewood 2024-06-20 87.2 elev set noaa 0.123998366 usno 0.123483326",
        "lakewood 2024-12-21 noon noaa 16.918929986",
        "lakewood 2024-12-21 90.0 elev rise noaa 12.250248244 usno 12.252712019",
        "lakewood 2024-12-21 90.0 elev set noaa 21.595947058 usno 21.598962232",
        "lakewood 2024-12-21 90.0 sea rise noaa 12.262830428 usno 12.265290476",
        "lakewood 2024-12-21 90.0 sea set noaa 21.583364863 usno 21.586384133",
        "lakewood 2024-12-21 106.1 elev rise noaa 10.805316391 usno 10.808227866",
        "lakewood 2024-12-21 106.1 elev set noaa 23.040881203 usno 23.043416655",
        "lakewood 2024-12-21 98.5 elev rise noaa 11.512505074 usno 11.515193105",
        "lakewood 2024-12-21 98.5 elev set noaa 22.333691098 usno 22.336463462",
        "lakewood 2024-12-21 87.2 elev rise noaa 12.637591715 usno 12.639942622",
        "lakewood 2024-12-21 87.2 elev set noaa 21.208603298 usno 21.211743714",
        "lakewood 2017-10-17 noon noaa 16.705244877",
        "lakewood 2017-10-17 90.0 elev rise noaa 11.153270654 usno 11.144695421",
        "lakewood 2017-10-17 90.0 elev set noaa 22.244109027 usno 22.256748976",
        "lakewood 2017-10-17 90.0 sea rise noaa 11.164347227 usno 11.155753953",
        "lakewood 2017-10-17 90.0 sea set noaa 22.233043012 usno 22.245681051",
        "lakewood 2017-10-17 106.1 elev rise noaa 9.825061086 usno 9.817925802",
        "lakewood 2017-10-17 106.1 elev set noaa 23.570583548 usno 23.584010597",
        "lakewood 2017-10-17 98.5 elev rise noaa 10.488371561 usno 10.480684751",
        "lakewood 2017-10-17 98.5 elev set noaa 22.908270203 usno 22.921160292",
        "lakewood 2017-10-17 87.2 elev rise noaa 11.489751505 usno 11.480574293",
        "lakewood 2017-10-17 87.2 elev set noaa 21.907927523 usno 21.920542036",
        "jerusalem 2024-03-20 noon noaa 9.784084083",
        "jerusalem 2024-03-20 90.0 elev rise noaa 3.647901412 usno 3.645609401",
        "jerusalem 2024-03-20 90.0 elev set noaa 15.923688005 usno 15.927372081",
        "jerusalem 2024-03-20 90.0 sea rise noaa 3.717063059 usno 3.714833972",
        "jerusalem 2024-03-20 90.0 sea set noaa 15.854427544 usno 15.858142583",
        "jerusalem 2024-03-20 106.1 elev rise noaa 2.513973565 usno 2.510453656",
        "jerusalem 2024-03-20 106.1 elev set noaa 17.059742120 usno 17.063014003",
        "jerusalem 2024-03-20 98.5 elev rise noaa 3.115286640 usno 3.112470191",
        "jerusalem 2024-03-20 98.5 elev set noaa 16.457169107 usno 16.460640819",
        "jerusalem 2024-03-20 87.2 elev rise noaa 4.001835847 usno 3.999855606",
        "jerusalem 2024-03-20 87.2 elev set noaa 15.569276272 usno 15.573128940",
        "jerusalem 2024-06-20 noon noaa 9.685937753",
        "jerusalem 2024-06-20 90.0 elev rise noaa 2.494215083 usno 2.493627067",
        "jerusalem 2024-06-20 90.0 elev set noaa 16.881391496 usno 16.880838519",
        "jerusalem 2024-06-20 90.0 sea rise noaa 2.573219019 usno 2.572623930",
        "jerusalem 2024-06-20 90.0 sea set noaa 16.802384690 usno 16.801840639",
        "jerusalem 2024-06-20 106.1 elev rise noaa 1.110798177 usno 1.110296406",
        "jerusalem 2024-06-20 106.1 elev set noaa 18.264880776 usno 18.264194466",
        "jerusalem 2024-06-20 98.5 elev rise noaa 1.868138231 usno 1.867599498",
        "jerusalem 2024-06-20 98.5 elev set noaa 17.507495070 usno 17.506875489",
        "jerusalem 2024-06-20 87.2 elev rise noaa 2.893803848 usno 2.893178469",
        "jerusalem 2024-06-20 87.2 elev set noaa 16.481789145 usno 16.481282296",
        "jerusalem 2024-12-21 noon noaa 9.626730855",
        "jerusalem 2024-12-21 90.0 elev rise noaa 4.517020917 usno 4.519582940",
        "jerusalem 2024-12-21 90.0 elev set noaa 14.744707410 usno 14.747580769",
        "jerusalem 2024-12-21 90.0 sea rise noaa 4.594746338 usno 4.597284308",
        "jerusalem 2024-12-21 90.0 sea set noaa 14.666981982 usno 14.669880074",
        "jerusalem 2024-12-21 106.1 elev rise noaa 3.296482897 usno 3.299432036",
        "jerusalem 2024-12-21 106.1 elev set noaa 15.965245588 usno 15.967723370",
        "jerusalem 2024-12-21 98.5 elev rise noaa 3.931800776 usno 3.934546418",
        "jerusalem 2024-12-21 98.5 elev set noaa 15.329927619 usno 15.332612832",
        "jerusalem 2024-12-21 87.2 elev rise noaa 4.919530006 usno 4.921968480",
        "jerusalem 2024-12-21 87.2 elev set noaa 14.342198282 usno 14.345198955",
        "jerusalem 2017-10-17 noon noaa 9.416604946",
        "jerusalem 2017-10-17 90.0 elev rise noaa 3.667102303 usno 3.661612510",
        "jerusalem 2017-10-17 90.0 elev set noaa 15.155313996 usno 15.165351936",
        "jerusalem 2017-10-17 90.0 sea rise noaa 3.737455296 usno 3.731881620",
        "jerusalem 2017-10-17 90.0 sea set noaa 15.085005702 usno 15.095035907",
        "jerusalem 2017-10-17 106.1 elev rise noaa 2.532014974 usno 2.527468634",
        "jerusalem 2017-10-17 106.1 elev set noaa 16.289430175 usno 16.299898534",
        "jerusalem 2017-10-17 98.5 elev rise noaa 3.129630413 usno 3.124677963",
        "jerusalem 2017-10-17 98.5 elev set noaa 15.692391515 usno 15.702558921",
        "jerusalem 2017-10-17 87.2 elev rise noaa 4.028544406 usno 4.022586379",
        "jerusalem 2017-10-17 87.2 elev set noaa 14.794087026 usno 14.804107563",
        "sydney 2024-03-20 noon noaa 2.045407141",
        "sydney 2024-03-20 90.0 elev rise noaa 19.952386076 usno 19.956442064",
        "sydney 2024-03-20 90.0 elev set noaa 8.124523558 usno 8.122194260",
        "sydney 2024-03-20 90.0 sea rise noaa 19.972049732 usno 19.976094879",
        "sydney 2024-03-20 90.0 sea set noaa 8.104889843 usno 8.102542266",
        "sydney 2024-03-20 106.1 elev rise noaa 18.737195171 usno 18.742154794",
        "sydney 2024-03-20 106.1 elev set noaa 9.337268305 usno 9.335951589",
        "sydney 2024-03-20 98.5 elev rise noaa 19.354923996 usno 19.359358801",
        "sydney 2024-03-20 98.5 elev set noaa 8.720946606 usno 8.719139232",
        "sydney 2024-03-20 87.2 elev rise noaa 20.263962180 usno 20.267856297",
        "sydney 2024-03-20 87.2 elev set noaa 7.813394623 usno 7.810765469",
        "sydney 2024-06-20 noon noaa 1.944501068",
        "sydney 2024-06-20 90.0 elev rise noaa 20.974936856 usno 20.974463639",
        "sydney 2024-06-20 90.0 elev set noaa 6.917574433 usno 6.916965992",
        "sydney 2024-06-20 90.0 sea rise noaa 20.997175586 usno 20.996699695",
        "sydney 2024-06-20 90.0 sea set noaa 6.895335677 usno 6.894729371",
        "sydney 2024-06-20 106.1 elev rise noaa 19.665963661 usno 19.665657792",
        "sydney 2024-06-20 106.1 elev set noaa 8.226548432 usno 8.225797466",
        "sydney 2024-06-20 98.5 elev rise noaa 20.316228748 usno 20.315837495",
        "sydney 2024-06-20 98.5 elev set noaa 7.576283176 usno 7.575606718",
        "sydney 2024-06-20 87.2 elev rise noaa 21.332093146 usno 21.331577867",
        "sydney 2024-06-20 87.2 elev set noaa 6.560417694 usno 6.559842007",
        "sydney 2024-12-21 noon noaa 1.883802063",
        "sydney 2024-12-21 90.0 elev rise noaa 18.658730313 usno 18.661598874",
        "sydney 2024-12-21 90.0 elev set noaa 9.117216483 usno 9.119680095",
        "sydney 2024-12-21 90.0 sea rise noaa 18.681285192 usno 18.684147451",
        "sydney 2024-12-21 90.0 sea set noaa 9.094660936 usno 9.097131388",
        "sydney 2024-12-21 106.1 elev rise noaa 17.157064684 usno 17.160305509",
        "sydney 2024-12-21 106.1 elev set noaa 10.618947736 usno 10.620986209",
        "sydney 2024-12-21 98.5 elev rise noaa 17.951104832 usno 17.954162447",
        "sydney 2024-12-21 98.5 elev set noaa 9.824866789 usno 9.827121354",
        "sydney 2024-12-21 87.2 elev rise noaa 19.011488844 usno 19.014257274",
        "sydney 2024-12-21 87.2 elev set noaa 8.764448200 usno 8.767019795",
        "sydney 2017-10-17 noon noaa 1.677470624",
        "sydney 2017-10-17 90.0 elev rise noaa 19.173886249 usno 19.184561738",
        "sydney 2017-10-17 90.0 elev set noaa 8.186812160 usno 8.180338469",
        "sydney 2017-10-17 90.0 sea rise noaa 19.193938601 usno 19.204615684",
        "sydney 2017-10-17 90.0 sea set noaa 8.166713600 usno 8.160268495",
        "sydney 2017-10-17 106.1 elev rise noaa 17.909533314 usno 17.920513455",
        "sydney 2017-10-17 106.1 elev set noaa 9.455075273 usno 9.445992211",
        "sydney 2017-10-17 98.5 elev rise noaa 18.558897541 usno 18.569617340",
        "sydney 2017-10-17 98.5 elev set noaa 8.803426199 usno 8.795904413",
        "sydney 2017-10-17 87.2 elev rise noaa 19.490303598 usno 19.501026239",
        "sydney 2017-10-17 87.2 elev set noaa 7.869707809 usno 7.863649456",
        "macapa 2024-03-20 noon noaa 15.527884048",
        "macapa 2024-03-20 90.0 elev rise noaa 9.462803946 usno 9.463579604",
        "macapa 2024-03-20 90.0 elev set noaa 21.588024705 usno 21.588825958",
        "macapa 2024-03-20 90.0 sea rise noaa 9.471100159 usno 9.471877559",
        "macapa 2024-03-20 90.0 sea set noaa 21.579728382 usno 21.580527865",
        "macapa 2024-03-20 106.1 elev rise noaa 8.453531281 usno 8.454094244",
        "macapa 2024-03-20 106.1 elev set noaa 22.597313675 usno 22.598328932",
        "macapa 2024-03-20 98.5 elev rise noaa 8.960093884 usno 8.960763788",
        "macapa 2024-03-20 98.5 elev set noaa 22.090742127 usno 22.091650301",
        "macapa 2024-03-20 87.2 elev rise noaa 9.713272758 usno 9.714100987",
        "macapa 2024-03-20 87.2 elev set noaa 21.337552687 usno 21.338300412",
        "macapa 2024-06-20 noon noaa 15.431788156",
        "macapa 2024-06-20 90.0 elev rise noaa 9.362080012 usno 9.361539931",
        "macapa 2024-06-20 90.0 elev set noaa 21.505125571 usno 21.504558064",
        "macapa 2024-06-20 90.0 sea rise noaa 9.371125754 usno 9.370584564",
        "macapa 2024-06-20 90.0 sea set noaa 21.496079777 usno 21.495513413",
        "macapa 2024-06-20 106.1 elev rise noaa 8.258596921 usno 8.258189622",
        "macapa 2024-06-20 106.1 elev set noaa 22.608616554 usno 22.607910642",
        "macapa 2024-06-20 98.5 elev rise noaa 8.813524437 usno 8.813051111",
        "macapa 2024-06-20 98.5 elev set noaa 22.053684678 usno 22.053047984",
        "macapa 2024-06-20 87.2 elev rise noaa 9.635184784 usno 9.634611157",
        "macapa 2024-06-20 87.2 elev set noaa 21.232019274 usno 21.231486295",
        "macapa 2024-12-21 noon noaa 15.373689814",
        "macapa 2024-12-21 90.0 elev rise noaa 9.307145297 usno 9.309865720",
        "macapa 2024-12-21 90.0 elev set noaa 21.448500214 usno 21.451185020",
        "macapa 2024-12-21 90.0 sea rise noaa 9.316192757 usno 9.318910286",
        "macapa 2024-12-21 90.0 sea set noaa 21.439452823 usno 21.442140573",
        "macapa 2024-12-21 106.1 elev rise noaa 8.203539788 usno 8.206612192",
        "macapa 2024-12-21 106.1 elev set noaa 22.552095445 usno 22.554423506",
        "macapa 2024-12-21 98.5 elev rise noaa 8.758506591 usno 8.761402353",
        "macapa 2024-12-21 98.5 elev set noaa 21.997134318 usno 21.999641087",
        "macapa 2024-12-21 87.2 elev rise noaa 9.580306870 usno 9.582939931",
        "macapa 2024-12-21 87.2 elev set noaa 21.175340629 usno 21.178114412",
        "macapa 2017-10-17 noon noaa 15.160757724",
        "macapa 2017-10-17 90.0 elev rise noaa 9.095577701 usno 9.098042670",
        "macapa 2017-10-17 90.0 elev set noaa 21.222576164 usno 21.224997576",
        "macapa 2017-10-17 90.0 sea rise noaa 9.103986822 usno 9.106448577",
        "macapa 2017-10-17 90.0 sea set noaa 21.214162471 usno 21.216587281",
        "macapa 2017-10-17 106.1 elev rise noaa 8.072230985 usno 8.075054845",
        "macapa 2017-10-17 106.1 elev set noaa 22.246591477 usno 22.248534801",
        "macapa 2017-10-17 98.5 elev rise noaa 8.585984855 usno 8.588635024",
        "macapa 2017-10-17 98.5 elev set noaa 21.732471835 usno 21.734673251",
        "macapa 2017-10-17 87.2 elev rise noaa 9.349460148 usno 9.351825282",
        "macapa 2017-10-17 87.2 elev set noaa 20.968561180 usno 20.971082431",
        "suva 2024-03-20 noon noaa 0.230272536",
        "suva 2024-03-20 90.0 elev rise noaa 18.161865450 usno 18.164243627",
        "suva 2024-03-20 90.0 elev set noaa 6.289387005 usno 6.288653751",
        "suva 2024-03-20 90.0 sea rise noaa 18.167389059 usno 18.169766331",
        "suva 2024-03-20 90.0 sea set noaa 6.283867492 usno 6.283131138",
        "suva 2024-03-20 106.1 elev rise noaa 17.094382771 usno 17.097019588",
        "suva 2024-03-20 106.1 elev set noaa 7.355863921 usno 7.355683848",
        "suva 2024-03-20 98.5 elev rise noaa 17.629177673 usno 17.631661609",
        "suva 2024-03-20 98.5 elev set noaa 6.821631456 usno 6.821184097",
        "suva 2024-03-20 87.2 elev rise noaa 18.422327186 usno 18.424666215",
        "suva 2024-03-20 87.2 elev set noaa 6.029108704 usno 6.028225624",
        "suva 2024-06-20 noon noaa 0.128719666",
        "suva 2024-06-20 90.0 elev rise noaa 18.603668994 usno 18.603170950",
        "suva 2024-06-20 90.0 elev set noaa 5.657331674 usno 5.656755731",
        "suva 2024-06-20 90.0 sea rise noaa 18.609735571 usno 18.609236779",
        "suva 2024-06-20 90.0 sea set noaa 5.651265034 usno 5.650689777",
        "suva 2024-06-20 106.1 elev rise noaa 17.453601086 usno 17.453246637",
        "suva 2024-06-20 106.1 elev set noaa 6.807413331 usno 6.806701183",
        "suva 2024-06-20 98.5 elev rise noaa 18.024546293 usno 18.024120272",
        "suva 2024-06-20 98.5 elev set noaa 6.236460780 usno 6.235817625",
        "suva 2024-06-20 87.2 elev rise noaa 18.891273241 usno 18.890739859",
        "suva 2024-06-20 87.2 elev set noaa 5.369724589 usno 5.369180657",
        "suva 2024-12-21 noon noaa 0.067670933",
        "suva 2024-12-21 90.0 elev rise noaa 17.454586491 usno 17.457371316",
        "suva 2024-12-21 90.0 elev set noaa 6.689058970 usno 6.691636031",
        "suva 2024-12-21 90.0 sea rise noaa 17.460685095 usno 17.463468086",
        "suva 2024-12-21 90.0 sea set noaa 6.682960236 usno 6.685539222",
        "suva 2024-12-21 106.1 elev rise noaa 16.242877044 usno 16.246013094",
        "suva 2024-12-21 106.1 elev set noaa 7.900801847 usno 7.903003956",
        "suva 2024-12-21 98.5 elev rise noaa 16.859077954 usno 16.862039117",
        "suva 2024-12-21 98.5 elev set noaa 7.284581833 usno 7.286972521",
        "suva 2024-12-21 87.2 elev rise noaa 17.740673758 usno 17.743372005",
        "suva 2024-12-21 87.2 elev set noaa 6.402965907 usno 6.405633515",
        "suva 2017-10-17 noon noaa 23.862231728",
        "suva 2017-10-17 90.0 elev rise noaa 17.594972267 usno 17.601508942",
        "suva 2017-10-17 90.0 elev set noaa 6.130327087 usno 6.128520982",
        "suva 2017-10-17 90.0 sea rise noaa 17.600575847 usno 17.607112027",
        "suva 2017-10-17 90.0 sea set noaa 6.124716133 usno 6.122914618",
        "suva 2017-10-17 106.1 elev rise noaa 16.503110045 usno 16.509869325",
        "suva 2017-10-17 106.1 elev set noaa 7.223999469 usno 7.221005501",
        "suva 2017-10-17 98.5 elev rise noaa 17.052469811 usno 17.059084158",
        "suva 2017-10-17 98.5 elev set noaa 6.673626727 usno 6.671310237",
        "suva 2017-10-17 87.2 elev rise noaa 17.858745409 usno 17.865265449",
        "suva 2017-10-17 87.2 elev set noaa 5.866223432 usno 5.864620000",
        "ushuaia 2024-03-20 noon noaa 16.676554257",
        "ushuaia 2024-03-20 90.0 elev rise noaa 10.572809680 usno 10.580500240",
        "ushuaia 2024-03-20 90.0 elev set noaa 22.756383689 usno 22.750567105",
        "ushuaia 2024-03-20 90.0 sea rise noaa 10.590663122 usno 10.598328909",
        "ushuaia 2024-03-20 90.0 sea set noaa 22.738587452 usno 22.732739884",
        "ushuaia 2024-03-20 106.1 elev rise noaa 8.768638028 usno 8.780089891",
        "ushuaia 2024-03-20 106.1 elev set noaa 0.551633626 usno 0.548487365",
        "ushuaia 2024-03-20 98.5 elev rise noaa 9.695626648 usno 9.704770934",
        "ushuaia 2024-03-20 98.5 elev set noaa 23.630144509 usno 23.625709524",
        "ushuaia 2024-03-20 87.2 elev rise noaa 11.011770853 usno 11.018893446",
        "ushuaia 2024-03-20 87.2 elev set noaa 22.318714828 usno 22.312091247",
        "ushuaia 2024-06-20 noon noaa 16.580868618",
        "ushuaia 2024-06-20 90.0 elev rise noaa 12.955012725 usno 12.954577502",
        "ushuaia 2024-06-20 90.0 elev set noaa 20.210302938 usno 20.209665616",
        "ushuaia 2024-06-20 90.0 sea rise noaa 12.978960828 usno 12.978524444",
        "ushuaia 2024-06-20 90.0 sea set noaa 20.186354877 usno 20.185718595",
        "ushuaia 2024-06-20 106.1 elev rise noaa 10.926952380 usno 10.926708573",
        "ushuaia 2024-06-20 106.1 elev set noaa 22.238357465 usno 22.237538389",
        "ushuaia 2024-06-20 98.5 elev rise noaa 11.887278850 usno 11.886925920",
        "ushuaia 2024-06-20 98.5 elev set noaa 21.278034401 usno 21.277319766",
        "ushuaia 2024-06-20 87.2 elev rise noaa 13.581574130 usno 13.581123534",
        "ushuaia 2024-06-20 87.2 elev set noaa 19.583742493 usno 19.583117083",
        "ushuaia 2024-12-21 noon noaa 16.522992076",
        "ushuaia 2024-12-21 90.0 elev rise noaa 7.835359046 usno 7.838516151",
        "ushuaia 2024-12-21 90.0 elev set noaa 1.218636026 usno 1.220855130",
        "ushuaia 2024-12-21 90.0 sea rise noaa 7.860780388 usno 7.863932036",
        "ushuaia 2024-12-21 90.0 sea set noaa 1.193217188 usno 1.195441219",
        "ushuaia 2024-12-21 106.1 elev rise noaa NaN usno NaN",
        "ushuaia 2024-12-21 106.1 elev set noaa NaN usno NaN",
        "ushuaia 2024-12-21 98.5 elev rise noaa 6.297255068 usno 6.300550524",
        "ushuaia 2024-12-21 98.5 elev set noaa 2.756450877 usno 2.758588593",
        "ushuaia 2024-12-21 87.2 elev rise noaa 8.425437737 usno 8.428455870",
        "ushuaia 2024-12-21 87.2 elev set noaa 0.628606777 usno 0.630954004",
        "ushuaia 2017-10-17 noon noaa 16.309499852",
        "ushuaia 2017-10-17 90.0 elev rise noaa 9.286293406 usno 9.305690946",
        "ushuaia 2017-10-17 90.0 elev set noaa 23.350925530 usno 23.333261126",
        "ushuaia 2017-10-17 90.0 sea rise noaa 9.304982222 usno 9.324379572",
        "ushuaia 2017-10-17 90.0 sea set noaa 23.332138458 usno 23.314538407",
        "ushuaia 2017-10-17 106.1 elev rise noaa 7.233331082 usno 7.255673185",
        "ushuaia 2017-10-17 106.1 elev set noaa 1.423403891 usno 1.391786303",
        "ushuaia 2017-10-17 98.5 elev rise noaa 8.336524888 usno 8.356461140",
        "ushuaia 2017-10-17 98.5 elev set noaa 0.307047364 usno 0.285017060",
        "ushuaia 2017-10-17 87.2 elev rise noaa 9.739484339 usno 9.758980893",
        "ushuaia 2017-10-17 87.2 elev set noaa 22.895567269 usno 22.879283485",
        "norway 2024-03-20 noon noaa 11.514370551",
        "norway 2024-03-20 90.0 elev rise noaa 5.342668607 usno 5.329598655",
        "norway 2024-03-20 90.0 elev set noaa 17.718815813 usno 17.732306869",
        "norway 2024-03-20 90.0 sea rise noaa 5.342668607 usno 5.329598655",
        "norway 2024-03-20 90.0 sea set noaa 17.718815813 usno 17.732306869",
        "norway 2024-03-20 106.1 elev rise noaa 1.870219072 usno 1.827603439",
        "norway 2024-03-20 106.1 elev set noaa 21.257573848 usno 21.262704679",
        "norway 2024-03-20 98.5 elev rise noaa 3.790889714 usno 3.770679742",
        "norway 2024-03-20 98.5 elev set noaa 19.285460469 usno 19.295461651",
        "norway 2024-03-20 87.2 elev rise noaa 6.055337669 usno 6.044489171",
        "norway 2024-03-20 87.2 elev set noaa 17.002065641 usno 17.017686221",
        "norway 2024-06-20 noon noaa 11.416841724",
        "norway 2024-06-20 90.0 elev rise noaa NaN usno NaN",
        "norway 2024-06-20 90.0 elev set noaa NaN usno NaN",
        "norway 2024-06-20 90.0 sea rise noaa NaN usno NaN",
        "norway 2024-06-20 90.0 sea set noaa NaN usno NaN",
        "norway 2024-06-20 106.1 elev rise noaa NaN usno NaN",
        "norway 2024-06-20 106.1 elev set noaa NaN usno NaN",
        "norway 2024-06-20 98.5 elev rise noaa NaN usno NaN",
        "norway 2024-06-20 98.5 elev set noaa NaN usno NaN",
        "norway 2024-06-20 87.2 elev rise noaa NaN usno NaN",
        "norway 2024-06-20 87.2 elev set noaa NaN usno NaN",
        "norway 2024-12-21 noon noaa 11.357968677",
        "norway 2024-12-21 90.0 elev rise noaa NaN usno NaN",
        "norway 2024-12-21 90.0 elev set noaa NaN usno NaN",
        "norway 2024-12-21 90.0 sea rise noaa NaN usno NaN",
        "norway 2024-12-21 90.0 sea set noaa NaN usno NaN",
        "norway 2024-12-21 106.1 elev rise noaa 6.567475104 usno 6.570450214",
        "norway 2024-12-21 106.1 elev set noaa 16.156783374 usno 16.159373646",
        "norway 2024-12-21 98.5 elev rise noaa 8.467116496 usno 8.469750469",
        "norway 2024-12-21 98.5 elev set noaa 14.257135385 usno 14.260177146",
        "norway 2024-12-21 87.2 elev rise noaa NaN usno NaN",
        "norway 2024-12-21 87.2 elev set noaa NaN usno NaN",
        "norway 2017-10-17 noon noaa 11.146996501",
        "norway 2017-10-17 90.0 elev rise noaa 6.769237065 usno 6.728376320",
        "norway 2017-10-17 90.0 elev set noaa 15.493744316 usno 15.529218340",
        "norway 2017-10-17 90.0 sea rise noaa 6.769237065 usno 6.728376320",
        "norway 2017-10-17 90.0 sea set noaa 15.493744316 usno 15.529218340",
        "norway 2017-10-17 106.1 elev rise noaa 3.681508407 usno 3.651866273",
        "norway 2017-10-17 106.1 elev set noaa 18.565125721 usno 18.607909209",
        "norway 2017-10-17 98.5 elev rise noaa 5.197370795 usno 5.165376611",
        "norway 2017-10-17 98.5 elev set noaa 17.059872139 usno 17.096273909",
        "norway 2017-10-17 87.2 elev rise noaa 7.614075734 usno 7.563724273",
        "norway 2017-10-17 87.2 elev set noaa 14.650797117 usno 14.688199441",
        )
    }
}
