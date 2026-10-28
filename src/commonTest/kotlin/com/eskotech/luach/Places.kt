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
import kotlinx.datetime.TimeZone

internal object Places {
    val LAKEWOOD = Location(40.0721087, -74.2400243, TimeZone.of("America/New_York"), 15.0, "Lakewood, NJ")

    val WOODCLIFF_LAKE = Location(41.1181036, -74.0840691, TimeZone.of("America/New_York"), 167.0, "Woodcliff Lake, NJ")

    val LOS_ANGELES = Location(34.0201613, -118.6919095, TimeZone.of("America/Los_Angeles"), 71.0, "Los Angeles, CA")

    val JERUSALEM = Location(31.7962994, 35.1053185, TimeZone.of("Asia/Jerusalem"), 754.0, "Jerusalem", inIsrael = true, mukefChoma = true)

    val NORTHERN_NORWAY = Location(70.1498248, 9.1456867, TimeZone.of("Europe/Oslo"), 0.0, "Northern Norway")

    val SYDNEY = Location(-33.8688, 151.2093, TimeZone.of("Australia/Sydney"), 58.0, "Sydney")

    val MACAPA = Location(0.0349, -51.0694, TimeZone.of("America/Belem"), 15.0, "Macapá")

    val SUVA = Location(-18.1416, 178.4419, TimeZone.of("Pacific/Fiji"), 6.0, "Suva")

    val USHUAIA = Location(-54.8019, -68.3030, TimeZone.of("America/Argentina/Ushuaia"), 23.0, "Ushuaia")

    val APIA = Location(-13.8333, -171.75, TimeZone.of("Pacific/Apia"), 2.0, "Apia")

    val KIRITIMATI = Location(1.8721, -157.4278, TimeZone.of("Pacific/Kiritimati"), 3.0, "Kiritimati")

    val BAKER_ISLAND = Location(0.1936, -176.4769, TimeZone.of("Etc/GMT+12"), 0.0, "Baker Island")

    val TROMSO = Location(69.6496, 18.956, TimeZone.of("Europe/Oslo"), 100.0, "Tromsø")

    fun inZone(location: Location, zoneId: String): Location = location.copy(timeZone = TimeZone.of(zoneId))

    val SOLAR_SET: List<Location> = listOf(LAKEWOOD, JERUSALEM, SYDNEY, MACAPA, SUVA, USHUAIA, LOS_ANGELES)
}
