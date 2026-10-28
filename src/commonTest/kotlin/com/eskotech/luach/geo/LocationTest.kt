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

import com.eskotech.luach.Places
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.datetime.TimeZone

class LocationTest {

    private fun point(latitude: Double, longitude: Double): Location = Location(latitude, longitude, TimeZone.UTC)

    @Test
    fun geodesicDistancesAndBearings() {
        val origin = point(0.0, 0.0)
        assertEquals(111319.491, origin.geodesicTo(point(0.0, 1.0)).distance, 0.001)
        assertEquals(90.0, origin.geodesicTo(point(0.0, 1.0)).initialBearing, 1e-6)
        assertEquals(90.0, origin.geodesicTo(point(0.0, 1.0)).finalBearing, 1e-6)
        assertEquals(-90.0, origin.geodesicTo(point(0.0, -1.0)).initialBearing, 1e-6)
        assertEquals(110574.389, origin.geodesicTo(point(1.0, 0.0)).distance, 0.001)
        assertEquals(0.0, origin.geodesicTo(point(1.0, 0.0)).initialBearing, 1e-6)
        assertEquals(10001965.729, origin.geodesicTo(point(90.0, 0.0)).distance, 0.001)
        assertEquals(156899.568, origin.geodesicTo(point(1.0, 1.0)).distance, 0.001)
        assertEquals(45.188040, origin.geodesicTo(point(1.0, 1.0)).initialBearing, 1e-5)
        assertEquals(45.196767, origin.geodesicTo(point(1.0, 1.0)).finalBearing, 1e-5)

        val newYork = point(40.7128, -74.0060)
        val london = point(51.5074, -0.1278)
        assertEquals(5585233.579, newYork.geodesicTo(london).distance, 0.001)
        assertEquals(51.241229, newYork.geodesicTo(london).initialBearing, 1e-5)
        assertEquals(108.368998, newYork.geodesicTo(london).finalBearing, 1e-5)
        assertEquals(5585233.579, london.geodesicTo(newYork).distance, 0.001)
        assertEquals(108.368998 - 180.0, london.geodesicTo(newYork).initialBearing, 1e-5)

        val lakewood = Places.LAKEWOOD
        val jerusalem = Places.JERUSALEM
        assertEquals(9237082.240, lakewood.geodesicTo(jerusalem).distance, 0.001)
        assertEquals(53.868047, lakewood.geodesicTo(jerusalem).initialBearing, 1e-5)
        assertEquals(133.321554, lakewood.geodesicTo(jerusalem).finalBearing, 1e-5)
        assertEquals(12025129.190, Places.SYDNEY.geodesicTo(Places.LOS_ANGELES).distance, 0.001)
        assertEquals(60.940184, Places.SYDNEY.geodesicTo(Places.LOS_ANGELES).initialBearing, 1e-5)

        val coincident = lakewood.geodesicTo(lakewood.copy())
        assertEquals(0.0, coincident.distance)
        assertTrue(coincident.initialBearing.isNaN() && coincident.finalBearing.isNaN())

        val a = point(50.06632222222222, -5.71475)
        val b = point(58.64402222222222, -3.0700944444444445)
        assertEquals(9.14186191, a.geodesicTo(b).initialBearing, 1e-8)
        assertEquals(11.29720127, a.geodesicTo(b).finalBearing, 1e-8)
        assertEquals(969954.11445043, a.geodesicTo(b).distance, 1e-5)
    }

    @Test
    fun rhumbLineDistancesAndBearings() {
        val origin = point(0.0, 0.0)
        assertEquals(111319.491, origin.rhumbLineTo(point(0.0, 1.0)).distance, 0.001)
        assertEquals(90.0, origin.rhumbLineTo(point(0.0, 1.0)).bearing, 1e-6)
        assertEquals(111319.491, origin.rhumbLineTo(point(1.0, 0.0)).distance, 0.001)
        assertEquals(0.0, origin.rhumbLineTo(point(1.0, 0.0)).bearing, 1e-6)
        assertEquals(10018754.171, origin.rhumbLineTo(point(90.0, 0.0)).distance, 0.001)
        assertEquals(157425.537, origin.rhumbLineTo(point(1.0, 1.0)).distance, 0.001)
        assertEquals(44.998545, origin.rhumbLineTo(point(1.0, 1.0)).bearing, 1e-5)
        assertEquals(-90.0, origin.rhumbLineTo(point(0.0, -1.0)).bearing, 1e-6)
        val newYork = point(40.7128, -74.0060)
        val london = point(51.5074, -0.1278)
        assertEquals(5800611.512, newYork.rhumbLineTo(london).distance, 0.001)
        assertEquals(78.044081, newYork.rhumbLineTo(london).bearing, 1e-5)
        assertEquals(5800611.512, london.rhumbLineTo(newYork).distance, 0.001)
        assertEquals(9881224.408, Places.LAKEWOOD.rhumbLineTo(Places.JERUSALEM).distance, 0.001)
        assertEquals(95.349642, Places.LAKEWOOD.rhumbLineTo(Places.JERUSALEM).bearing, 1e-5)
        assertEquals(12080352.912, Places.SYDNEY.rhumbLineTo(Places.LOS_ANGELES).distance, 0.001)
        assertEquals(51.274394, Places.SYDNEY.rhumbLineTo(Places.LOS_ANGELES).bearing, 1e-5)
        val a = point(50.06638888888889, -5.714722222222222)
        val b = point(58.64388888888889, -3.07)
        assertEquals(10.14069288, a.rhumbLineTo(b).bearing, 1e-8)
        assertEquals(969995.8368008, a.rhumbLineTo(b).distance, 1e-5)
    }

    @Test
    fun coordinatesFromDegreesMinutesSeconds() {
        assertEquals(40.0721083, Location.coordinate(40, 4, 19.59, 'N'), 1e-7)
        assertEquals(-33.8688, Location.coordinate(33, 52, 7.68, 'S'), 1e-7)
        assertEquals(-74.2400250, Location.coordinate(74, 14, 24.09, 'W'), 1e-7)
        assertEquals(151.2093, Location.coordinate(151, 12, 33.48, 'E'), 1e-7)
        assertEquals(151.2093, Location.coordinate(151, 12, 33.48, 'e'), 1e-7)
        assertFailsWith<IllegalArgumentException> { Location.coordinate(40, 0, 0.0, 'X') }
        assertFailsWith<IllegalArgumentException> { Location.coordinate(40, 60, 0.0, 'N') }
        assertFailsWith<IllegalArgumentException> { Location.coordinate(-1, 0, 0.0, 'N') }
        assertFailsWith<IllegalArgumentException> { Location(Location.coordinate(91, 0, 0.0, 'N'), 0.0, TimeZone.UTC) }
    }

    @Test
    fun validationAndEquality() {
        val lakewood = Location(40.0828, -74.2094, TimeZone.of("America/New_York"), 20.0, "Lakewood, NJ")
        assertEquals(40.0828, lakewood.latitude)
        assertEquals(-74.2094, lakewood.longitude)
        assertEquals(TimeZone.of("America/New_York"), lakewood.timeZone)
        assertEquals(20.0, lakewood.elevation)
        assertEquals("Lakewood, NJ", lakewood.name)
        assertEquals(0.0, point(1.0, 2.0).elevation)
        assertEquals(null, point(1.0, 2.0).name)

        assertFailsWith<IllegalArgumentException> { point(90.1, 0.0) }
        assertFailsWith<IllegalArgumentException> { point(-90.1, 0.0) }
        assertFailsWith<IllegalArgumentException> { point(0.0, 180.1) }
        assertFailsWith<IllegalArgumentException> { point(0.0, -180.1) }
        assertFailsWith<IllegalArgumentException> { point(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { Location(0.0, 0.0, TimeZone.UTC, -1.0) }
        assertFailsWith<IllegalArgumentException> { Location(0.0, 0.0, TimeZone.UTC, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { Location(0.0, 0.0, TimeZone.UTC, Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { lakewood.copy(latitude = 100.0) }
        assertEquals(90.0, point(90.0, -180.0).latitude)
        assertEquals(-180.0, point(90.0, -180.0).longitude)

        val same = Location(40.0828, -74.2094, TimeZone.of("America/New_York"), 20.0, "Lakewood, NJ")
        assertEquals(lakewood, same)
        assertEquals(lakewood.hashCode(), same.hashCode())
        assertNotEquals(lakewood, lakewood.copy(latitude = 41.0))
        assertNotEquals(lakewood, lakewood.copy(elevation = 5.0))
        assertNotEquals(lakewood, lakewood.copy(timeZone = TimeZone.UTC))
        assertEquals(5.0, lakewood.copy(elevation = 5.0).elevation)
        assertEquals(20.0, lakewood.elevation)
        assertTrue(lakewood.toString().contains("Lakewood, NJ") && lakewood.toString().contains("America/New_York"))
    }

    @Test
    fun israelAndWalledCityFlags() {
        val plain = point(31.78, 35.22)
        assertFalse(plain.inIsrael)
        assertFalse(plain.mukefChoma)
        val jerusalem = plain.copy(inIsrael = true, mukefChoma = true)
        assertTrue(jerusalem.inIsrael)
        assertTrue(jerusalem.mukefChoma)
        assertNotEquals(plain, jerusalem)
        assertEquals(jerusalem, Location(31.78, 35.22, TimeZone.UTC, inIsrael = true, mukefChoma = true))
        assertEquals(plain.geodesicTo(Places.LAKEWOOD), jerusalem.geodesicTo(Places.LAKEWOOD))
        assertTrue(Places.JERUSALEM.inIsrael && Places.JERUSALEM.mukefChoma)
        assertFalse(Places.LAKEWOOD.inIsrael || Places.LAKEWOOD.mukefChoma)
    }
}
