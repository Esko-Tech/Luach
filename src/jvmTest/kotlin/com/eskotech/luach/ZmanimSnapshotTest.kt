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

import com.eskotech.luach.zmanim.DayEdge
import com.eskotech.luach.zmanim.HalachicDay
import com.eskotech.luach.zmanim.KiddushLevanaEnd
import com.eskotech.luach.zmanim.KiddushLevanaStart
import com.eskotech.luach.zmanim.Shitos
import com.eskotech.luach.zmanim.Zman
import com.eskotech.luach.zmanim.Zmanim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class ZmanimSnapshotTest {
    private val getters = Getters.zeroArg(Zmanim::class.java)

    @Test
    fun zeroArgGetters() {
        assertEquals(10, getters.size, "Zmanim zero-arg getters: ${getters.map { it.name }}")
        Snapshot.collect("Zmanim.zeroArgGetters", SampleDates.ZMANIM_SAMPLES) { s, (place, date) ->
            for (config in ZmanimConfig.BASIC) {
                s.group = "$place $config"
                s.recordGetters(getters, config.zmanim(place, date))
            }
        }.assertMatches()
    }

    @Test
    fun edgesAndDays() {
        Snapshot.collect("Zmanim.edgesAndDays", SampleDates.ZMANIM_SAMPLES) { s, (place, date) ->
            for (config in ZmanimConfig.BASIC) {
                s.group = "$place $config"
                val k = config.zmanim(place, date)
                for ((name, edge) in ZmanimCatalogue.EDGES) {
                    s.recordOutcome("dawn $name") { k.alos(edge) }
                    s.recordOutcome("dusk $name") { k.tzais(edge) }
                }
                for ((name, day) in ZmanimCatalogue.DAYS) {
                    s.recordOutcome("temporalHour $name") { k.shaahZmanis(day) }
                    s.recordOutcome("sofZmanShema $name") { k.sofZmanShema(day) }
                    s.recordOutcome("sofZmanTefillah $name") { k.sofZmanTefillah(day) }
                    s.recordOutcome("sofZmanBiurChametz $name") { k.sofZmanBiurChametz(day) }
                    s.recordOutcome("minchaGedola $name") { k.minchaGedola(day) }
                    s.recordOutcome("minchaGedola at least 30 $name") { k.minchaGedola(day, atLeastHalfHourAfterChatzos = true) }
                    s.recordOutcome("minchaGedolaFromChatzos $name") { k.minchaGedolaFromChatzos(day) }
                    s.recordOutcome("samuchLeMinchaKetana $name") { k.samuchLeMinchaKetana(day) }
                    s.recordOutcome("minchaKetana $name") { k.minchaKetana(day) }
                    s.recordOutcome("plagHamincha $name") { k.plagHamincha(day) }
                }
            }
        }.assertMatches()
    }

    @Test
    fun hoursOfTheDay() {
        Snapshot.collect("Zmanim.hoursOfTheDay", SampleDates.SWEEP_SAMPLES) { s, (place, date) ->
            for (config in ZmanimConfig.BASIC) {
                s.group = "$place $config"
                val k = config.zmanim(place, date)
                for ((name, day) in ZmanimCatalogue.DAYS) for (hours in ZmanimCatalogue.HOURS) {
                    s.recordOutcome("hourOfDay($hours, $name)") { k.hourOfDay(hours, day) }
                }
                val halfDayBounds: List<Instant?> = listOf(
                    k.alos(DayEdge.Depression(18.0)), k.alos(DayEdge.Depression(16.1)), k.alos(DayEdge.Minutes(72.0)), k.hanetz,
                    k.fixedLocalChatzos, k.shkiah, k.tzais(DayEdge.Minutes(72.0)), null,
                )
                for (start in halfDayBounds) for (end in halfDayBounds) for (hours in ZmanimCatalogue.HALF_DAY_HOURS) {
                    s.recordOutcome("halfDayHour($hours)") { k.halfDayHour(hours, start, end) }
                }
            }
        }.assertMatches()
    }

    @Test
    fun hadlakasNeirosOffsets() {
        Snapshot.collect("Zmanim.hadlakasNeirosOffsets", SampleDates.SWEEP_SAMPLES) { s, (place, date) ->
            for (offset in listOf(0.0, 20.0, 22.5, 40.0, -5.0)) for (useElevation in listOf(false, true)) {
                val config = ZmanimConfig(useElevation = useElevation, hadlakasNeirosMinutes = offset)
                s.group = "$place $config"
                s.recordOutcome("hadlakasNeiros") { config.zmanim(place, date).hadlakasNeiros }
            }
        }.assertMatches()
    }

    @Test
    fun kiddushLevana() {
        val configs = listOf(ZmanimConfig(), ZmanimConfig(model = Model.ALMANAC, useElevation = true))
        val snapshot = Snapshot.collect("Zmanim.kiddushLevana", SampleDates.ZMANIM_SAMPLES) { s, (place, date) ->
            for (config in configs) {
                s.group = "$place $config"
                val k = config.zmanim(place, date)
                val dawn = k.alos(DayEdge.Minutes(72.0))
                val dusk = k.tzais(DayEdge.Minutes(72.0))
                for ((a, b) in listOf(dawn to dusk, null to null, dawn to null, null to dusk, dusk to dawn)) {
                    val outcomes = listOf(
                        "kiddushLevanaStart THREE_DAYS" to outcome { k.kiddushLevanaStart(KiddushLevanaStart.THREE_DAYS, a, b) },
                        "kiddushLevanaStart SEVEN_DAYS" to outcome { k.kiddushLevanaStart(KiddushLevanaStart.SEVEN_DAYS, a, b) },
                        "kiddushLevanaEnd HALF_MONTH" to outcome { k.kiddushLevanaEnd(KiddushLevanaEnd.HALF_MONTH, a, b) },
                        "kiddushLevanaEnd FIFTEEN_DAYS" to outcome { k.kiddushLevanaEnd(KiddushLevanaEnd.FIFTEEN_DAYS, a, b) },
                    )
                    for ((name, value) in outcomes) {
                        when {
                            value == null || value is Threw -> {}
                            value == a || value == b -> s.tally("$name returned the day bound")
                            else -> s.tally("$name returned the molad based time")
                        }
                        s.record(name, value)
                    }
                }
                s.record("molad", k.molad)
            }
        }
        snapshot.assertMatches()
        for (name in listOf("kiddushLevanaStart THREE_DAYS", "kiddushLevanaStart SEVEN_DAYS", "kiddushLevanaEnd HALF_MONTH", "kiddushLevanaEnd FIFTEEN_DAYS")) {
            assertTrue("$name returned the day bound" in snapshot.tallies, "$name never returned the day bound")
            assertTrue("$name returned the molad based time" in snapshot.tallies, "$name never returned the molad based time")
        }
    }

    @Test
    fun melacha() {
        val configs = listOf(ZmanimConfig(), ZmanimConfig(useElevation = true), ZmanimConfig(model = Model.ALMANAC))
        val samples = SamplePlaces.EVERY_DAY.flatMap { place -> SampleDates.LEAP_YEAR_DAYS.map { place to it } }
        Snapshot.collect("Zmanim.isAsurBemelacha", samples) { s, (place, date) ->
            for (config in configs) {
                s.group = "$place $config"
                val k = config.zmanim(place, date)
                val sunset = k.shkiah
                val start = startOfDay(k)
                val times = ArrayList<Instant>()
                for (hours in listOf(0, 6, 12, 18)) times += start + kotlin.time.Duration.parse("${hours}h")
                if (sunset != null) for (delta in listOf(-3_600_000L, -60_000L, -1L, 0L, 1L, 60_000L)) times += Instant.fromEpochMilliseconds(sunset.toEpochMilliseconds() + delta)
                k.tzais?.let { tzais -> for (delta in listOf(-1L, 0L, 1L)) times += Instant.fromEpochMilliseconds(tzais.toEpochMilliseconds() + delta) }
                for (inIsrael in listOf(false, true)) {
                    val flagged = k.copy(location = k.location.copy(inIsrael = inIsrael))
                    for (time in times) for (nightfall in listOf(k.tzais, k.tzais(DayEdge.Minutes(72.0)), null)) {
                        s.recordOutcome("isAsurBemelacha($inIsrael)") { flagged.isAsurBemelacha(time, nightfall) }
                    }
                }
            }
        }.assertMatches()
    }

    @Test
    fun catalogueIsStable() {
        Snapshot.single("Zmanim.catalogue") { s ->
            s.record("edges", ZmanimCatalogue.EDGES.map { it.first })
            s.record("days", ZmanimCatalogue.DAYS.map { it.first })
            s.record("presets", ZmanimCatalogue.PRESETS.map { it.toString() })
            s.record("named edges", ZmanimCatalogue.NAMED_EDGES.map { it.toString() })
            s.record("zmanim", Zman.entries.map { it.name })
            s.record("shitos", listOf(Shitos.GRA, Shitos.MAGEN_AVRAHAM).map { it.toString() })
        }.assertMatches()
    }

    @Test
    fun standardList() {
        val shitos = listOf(
            "gra" to Shitos.GRA,
            "magen avraham" to Shitos.MAGEN_AVRAHAM,
            "baal hatanya" to Shitos(DayEdge.ALOS_BAAL_HATANYA, DayEdge.MISHEYAKIR_10_2, HalachicDay.BAAL_HATANYA, DayEdge.TZAIS_BAAL_HATANYA),
        )
        Snapshot.collect("Zmanim.standardList", SampleDates.SWEEP_SAMPLES) { s, (place, date) ->
            for (config in ZmanimConfig.BASIC) {
                s.group = "$place $config"
                val k = config.zmanim(place, date)
                for ((name, shita) in shitos) {
                    for (zman in Zman.entries) s.recordOutcome("$zman $name") { k.zman(zman, shita) }
                    s.recordOutcome("all $name") { k.all(shita).values.toList() }
                }
            }
        }.assertMatches()
    }
}
