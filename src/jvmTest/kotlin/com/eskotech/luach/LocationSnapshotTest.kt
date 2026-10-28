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

import com.eskotech.luach.geo.Location
import kotlin.test.Test
import kotlinx.datetime.TimeZone

class LocationSnapshotTest {
    private val points: List<Place> = SamplePlaces.ALL + listOf(
        Place("North Pole", 90.0, 0.0, 0.0, "UTC"),
        Place("South Pole", -90.0, 0.0, 0.0, "UTC"),
        Place("Null Island", 0.0, 0.0, 0.0, "UTC"),
        Place("Equator 90E", 0.0, 90.0, 0.0, "UTC"),
        Place("Equator 180", 0.0, 180.0, 0.0, "UTC"),
        Place("Equator -180", 0.0, -180.0, 0.0, "UTC"),
        Place("Equator 179.5", 0.0, 179.5, 0.0, "UTC"),
        Place("Near antipode of Null Island", 0.5, 179.7, 0.0, "UTC"),
        Place("Antipode of Lakewood", -40.0959, 105.7823, 0.0, "UTC"),
        Place("Lakewood again", 40.0959, -74.2177, 15.0, "America/New_York"),
    )

    @Test
    fun distancesAndBearings() {
        Snapshot.collect("Location.distancesAndBearings", points) { s, a ->
            s.group = a.name
            for (b in points) {
                s.record("geodesic", a.location().geodesicTo(b.location()))
                s.record("rhumbLine", a.location().rhumbLineTo(b.location()))
            }
        }.assertMatches()
    }

    @Test
    fun constructionAndValidation() {
        Snapshot.single("Location.constructionAndValidation") { s ->
            val values = listOf(
                Triple(90.0, 180.0, 0.0), Triple(-90.0, -180.0, 8848.0), Triple(90.0001, 0.0, 0.0), Triple(-90.0001, 0.0, 0.0),
                Triple(0.0, 180.0001, 0.0), Triple(0.0, -180.0001, 0.0), Triple(0.0, 0.0, -0.001), Triple(0.0, 0.0, Double.NaN),
                Triple(0.0, 0.0, Double.POSITIVE_INFINITY), Triple(Double.NaN, Double.NaN, 1.0), Triple(91.0, 181.0, -1.0),
            )
            for ((latitude, longitude, elevation) in values) {
                s.group = "decimal degrees"
                s.recordOutcome("constructor") { Location(latitude, longitude, TimeZone.UTC, elevation, "v") }
                s.recordOutcome("copy") { SamplePlaces.LAKEWOOD.location().copy(latitude = latitude) }
            }
            val dms = listOf(40 to 5, 0 to 0, 90 to 0, 89 to 59, 91 to 0, -1 to 0, 179 to 59, 180 to 0, 181 to 0)
            for ((degrees, minutes) in dms) for (seconds in listOf(0.0, 45.2, 60.1)) for (direction in listOf('N', 'S', 'E', 'W', 'X', 'n')) {
                s.group = "degrees, minutes, seconds"
                s.recordOutcome("coordinate") { Location.coordinate(degrees, minutes, seconds, direction) }
            }
            val base = SamplePlaces.LAKEWOOD
            val variants = listOf(base, base.copy(), base.copy(name = "other"), base.copy(latitude = 40.1), base.copy(longitude = -74.3), base.copy(elevation = 16.0), base.copy(zoneId = "America/Detroit"))
            for (a in variants) for (b in variants) {
                s.group = "equality"
                s.record("equals", a.location() == b.location())
                s.record("hashCode equal", a.location().hashCode() == b.location().hashCode())
            }
        }.assertMatches()
    }
}
