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

import com.eskotech.luach.calendar.BavliDaf
import com.eskotech.luach.calendar.HebrewDate
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.Molad
import com.eskotech.luach.calendar.TefillahSummary
import com.eskotech.luach.calendar.YerushalmiDaf
import com.eskotech.luach.geo.Geodesic
import com.eskotech.luach.geo.Location
import com.eskotech.luach.geo.RhumbLine
import com.eskotech.luach.solar.AlmanacSolarModel
import com.eskotech.luach.solar.NoaaSolarModel
import com.eskotech.luach.solar.SolarModel
import com.eskotech.luach.zmanim.DayEdge
import com.eskotech.luach.zmanim.HalachicDay
import com.eskotech.luach.zmanim.Zmanim
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

data class Place(val name: String, val latitude: Double, val longitude: Double, val elevation: Double, val zoneId: String) {
    fun location(): Location = Location(latitude, longitude, TimeZone.of(zoneId), elevation, name)

    override fun toString(): String = name
}

object SamplePlaces {
    val LAKEWOOD = Place("Lakewood NJ", 40.0959, -74.2177, 15.0, "America/New_York")
    val JERUSALEM = Place("Jerusalem", 31.778, 35.2354, 800.0, "Asia/Jerusalem")
    val APIA = Place("Apia Samoa", -13.8333, -171.75, 2.0, "Pacific/Apia")
    val KIRITIMATI = Place("Kiritimati", 1.8721, -157.4278, 3.0, "Pacific/Kiritimati")
    val TROMSO = Place("Tromso", 69.6496, 18.956, 100.0, "Europe/Oslo")
    val SYDNEY = Place("Sydney", -33.8688, 151.2093, 58.0, "Australia/Sydney")
    val HONOLULU = Place("Honolulu", 21.3069, -157.8583, 5.0, "Pacific/Honolulu")
    val LOS_ANGELES_GMT = Place("Los Angeles (GMT zone)", 34.0522, -118.2437, 71.0, "GMT")
    val ANCHORAGE = Place("Anchorage", 61.2181, -149.9003, 31.0, "America/Anchorage")
    val BAKER_ISLAND = Place("Baker Island", 0.1936, -176.4769, 0.0, "Etc/GMT+12")
    val DENVER = Place("Denver", 39.7392, -104.9903, 1609.0, "America/Denver")
    val RAPA_ITI = Place("Rapa Iti", -27.6167, -144.3333, 0.0, "Pacific/Tahiti")
    val ANADYR = Place("Anadyr", 64.7337, 177.4968, 10.0, "Asia/Anadyr")
    val SANTIAGO = Place("Santiago", -33.4489, -70.6693, 570.0, "America/Santiago")
    val KOLKATA = Place("Kolkata", 22.5726, 88.3639, 9.0, "Asia/Kolkata")
    val CHATHAM = Place("Chatham Islands", -43.9535, -176.5597, 0.0, "Pacific/Chatham")
    val LORD_HOWE = Place("Lord Howe Island", -31.5553, 159.0821, 0.0, "Australia/Lord_Howe")
    val KATHMANDU = Place("Kathmandu", 27.7172, 85.324, 1400.0, "Asia/Kathmandu")
    val ST_JOHNS = Place("St. John's", 47.5615, -52.7126, 0.0, "America/St_Johns")
    val MCMURDO = Place("McMurdo Station", -77.8419, 166.6863, 10.0, "Antarctica/McMurdo")

    val ALL: List<Place> = listOf(
        LAKEWOOD, JERUSALEM, APIA, KIRITIMATI, TROMSO, SYDNEY, HONOLULU, LOS_ANGELES_GMT, ANCHORAGE, BAKER_ISLAND,
        DENVER, RAPA_ITI, ANADYR, SANTIAGO, KOLKATA, CHATHAM, LORD_HOWE, KATHMANDU, ST_JOHNS, MCMURDO,
    )

    val EVERY_DAY: List<Place> = listOf(LAKEWOOD, JERUSALEM, TROMSO, APIA)
}

object SampleDates {
    const val LEAP_YEAR = 2024

    fun days(from: LocalDate, through: LocalDate): List<LocalDate> {
        val result = ArrayList<LocalDate>()
        var date = from
        while (date <= through) {
            result += date
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    fun year(year: Int): List<LocalDate> = days(LocalDate(year, 1, 1), LocalDate(year, 12, 31))

    val LEAP_YEAR_DAYS: List<LocalDate> = year(LEAP_YEAR)

    val SPOT: List<LocalDate> = listOf(
        LocalDate(1901, 3, 1), LocalDate(1923, 9, 11), LocalDate(1950, 6, 21), LocalDate(1970, 1, 1),
        LocalDate(1975, 6, 24), LocalDate(1999, 12, 31), LocalDate(2000, 2, 29),
        LocalDate(2011, 12, 29), LocalDate(2011, 12, 31), LocalDate(1995, 1, 1),
        LocalDate(2018, 2, 3), LocalDate(2023, 3, 26), LocalDate(2023, 10, 29),
        LocalDate(2024, 3, 9), LocalDate(2024, 3, 10), LocalDate(2024, 3, 11),
        LocalDate(2024, 3, 20), LocalDate(2024, 3, 29), LocalDate(2024, 3, 31),
        LocalDate(2024, 4, 6), LocalDate(2024, 4, 7), LocalDate(2024, 6, 20), LocalDate(2024, 6, 21),
        LocalDate(2024, 9, 7), LocalDate(2024, 9, 8), LocalDate(2024, 9, 22), LocalDate(2024, 10, 6),
        LocalDate(2024, 10, 27), LocalDate(2024, 11, 3), LocalDate(2024, 12, 21), LocalDate(2026, 10, 13),
        LocalDate(2027, 12, 13), LocalDate(2028, 1, 10), LocalDate(2037, 12, 31), LocalDate(2050, 7, 15),
        LocalDate(2071, 1, 21), LocalDate(2100, 2, 28), LocalDate(2100, 3, 1), LocalDate(2150, 6, 30),
    )

    val EXTRA: Map<Place, List<LocalDate>> = mapOf(
        SamplePlaces.RAPA_ITI to days(LocalDate(2027, 12, 1), LocalDate(2028, 1, 31)),
        SamplePlaces.ANADYR to days(LocalDate(2071, 1, 1), LocalDate(2071, 2, 28)),
    )

    val ZMANIM_SAMPLES: List<Pair<Place, LocalDate>> = buildList {
        for (place in SamplePlaces.EVERY_DAY) for (date in LEAP_YEAR_DAYS) add(place to date)
        for (place in SamplePlaces.ALL) for (date in SPOT) {
            if (place !in SamplePlaces.EVERY_DAY || date.year != LEAP_YEAR) add(place to date)
        }
        for ((place, dates) in EXTRA) for (date in dates) add(place to date)
    }

    val SWEEP_SAMPLES: List<Pair<Place, LocalDate>> = buildList {
        val dates = LEAP_YEAR_DAYS.filter { it.day == 1 || it.day == 15 } + SPOT
        for (place in listOf(SamplePlaces.LAKEWOOD, SamplePlaces.JERUSALEM, SamplePlaces.TROMSO, SamplePlaces.KIRITIMATI, SamplePlaces.DENVER)) {
            for (date in dates) add(place to date)
        }
    }

    fun decade(date: LocalDate): String = "${date.year / 10 * 10}s"
}

enum class Model(val model: SolarModel) {
    NOAA(NoaaSolarModel),
    ALMANAC(AlmanacSolarModel),
}

data class ZmanimConfig(val model: Model = Model.NOAA, val useElevation: Boolean = false, val hadlakasNeirosMinutes: Double = 18.0) {
    fun zmanim(place: Place, date: LocalDate): Zmanim =
        Zmanim(place.location(), date, model.model, useElevation = useElevation, hadlakasNeirosMinutes = hadlakasNeirosMinutes)

    override fun toString(): String = "$model useElevation=$useElevation hadlakasNeiros=$hadlakasNeirosMinutes"

    companion object {
        val BASIC: List<ZmanimConfig> = Model.entries.flatMap { model -> listOf(false, true).map { ZmanimConfig(model, it) } }
    }
}

object ZmanimCatalogue {
    val DEPRESSIONS: List<Double> = listOf(
        16.1, 18.0, 19.0, 19.8, 26.0, 16.9, 11.5, 11.0, 10.2, 9.5, 7.65, 8.5, 7 + 5.0 / 60, 3.7, 3.8, 5.95, 3.65, 3.676, 4.61, 4.37,
        5.88, 4.8, 6.45, 7.67, 9.3, 9.75, 13.24, 6.0, 1.583, -2.1, -2.8, -3.05, 0.0, 45.0, 89.0,
    )

    val EDGES: List<Pair<String, DayEdge>> = buildList {
        add("horizon" to DayEdge.Horizon)
        for (degrees in DEPRESSIONS) add("depression $degrees" to DayEdge.Depression(degrees))
        for (minutes in listOf(60.0, 72.0, 90.0, 96.0, 120.0, 50.0, 58.5, -18.0, -16.875, -13.5, 0.0)) add("minutes $minutes" to DayEdge.Minutes(minutes))
        for (minutes in listOf(72.0, 90.0, 96.0, 120.0, -30.0)) add("proportional $minutes" to DayEdge.ProportionalMinutes(minutes))
        add("13.5 minutes before 7.083" to DayEdge.Shifted(DayEdge.Depression(7 + 5.0 / 60), -13.5))
        add("72 minutes shifted 10" to DayEdge.Shifted(DayEdge.Minutes(72.0), 10.0))
        add("dawn interval 19.8 x 5/18" to DayEdge.DawnInterval(DayEdge.Depression(19.8), 5 / 18.0))
    }

    val DAYS: List<Pair<String, HalachicDay>> = buildList {
        add("sunrise to sunset" to HalachicDay.GRA)
        for (minutes in listOf(60.0, 72.0, 90.0, 96.0, 120.0)) add("minutes $minutes" to HalachicDay.minutes(minutes))
        for (minutes in listOf(72.0, 90.0, 96.0, 120.0)) add("proportional $minutes" to HalachicDay.proportionalMinutes(minutes))
        for (degrees in listOf(16.1, 18.0, 19.8, 26.0, 1.583)) add("degrees $degrees" to HalachicDay.degrees(degrees))
        add("16.1 to 3.8" to HalachicDay.degrees(16.1, 3.8))
        add("16.1 to 3.7" to HalachicDay.degrees(16.1, 3.7))
        add("16.1 to 7.083" to HalachicDay.degrees(16.1, 7 + 5.0 / 60))
        add("16.1 to sunset" to HalachicDay(DayEdge.Depression(16.1), DayEdge.Horizon))
        add("ateret torah 40" to HalachicDay(DayEdge.ProportionalMinutes(72.0), DayEdge.Minutes(40.0)))
        add("ateret torah 25" to HalachicDay(DayEdge.ProportionalMinutes(72.0), DayEdge.Minutes(25.0)))
    }

    val PRESETS: List<HalachicDay> = listOf(
        HalachicDay.GRA, HalachicDay.MAGEN_AVRAHAM_72, HalachicDay.MAGEN_AVRAHAM_90, HalachicDay.MAGEN_AVRAHAM_120, HalachicDay.MAGEN_AVRAHAM_72_ZMANIYOS,
        HalachicDay.MAGEN_AVRAHAM_16_1, HalachicDay.MAGEN_AVRAHAM_18, HalachicDay.MAGEN_AVRAHAM_19_8, HalachicDay.MAGEN_AVRAHAM_26, HalachicDay.BAAL_HATANYA,
        HalachicDay.ATERET_TORAH,
    )

    val NAMED_EDGES: List<DayEdge> = listOf(
        DayEdge.ALOS_16_1, DayEdge.ALOS_18, DayEdge.ALOS_19_8, DayEdge.ALOS_26, DayEdge.ALOS_BAAL_HATANYA, DayEdge.ALOS_72, DayEdge.ALOS_90, DayEdge.ALOS_120,
        DayEdge.ALOS_72_ZMANIYOS, DayEdge.MISHEYAKIR_11_5, DayEdge.MISHEYAKIR_11, DayEdge.MISHEYAKIR_10_2, DayEdge.MISHEYAKIR_9_5, DayEdge.MISHEYAKIR_7_65,
        DayEdge.TZAIS_GEONIM_3_7, DayEdge.TZAIS_GEONIM_3_8, DayEdge.TZAIS_GEONIM_5_95, DayEdge.TZAIS_GEONIM_7_083, DayEdge.TZAIS_GEONIM_8_5, DayEdge.TZAIS_BAAL_HATANYA,
        DayEdge.TZAIS_RABBEINU_TAM_13_24, DayEdge.TZAIS_16_1, DayEdge.TZAIS_18, DayEdge.TZAIS_19_8, DayEdge.TZAIS_26, DayEdge.TZAIS_72, DayEdge.TZAIS_90,
        DayEdge.TZAIS_96, DayEdge.TZAIS_120, DayEdge.TZAIS_72_ZMANIYOS,
    )

    val HOURS: List<Double> = listOf(0.0, 0.5, 3.0, 4.0, 5.0, 6.0, 6.5, 9.0, 9.5, 10.75, 12.0, -1.25, 13.5)

    val HALF_DAY_HOURS: List<Double> = listOf(0.0, 1.0, 2.5, 3.0, 3.5, 4.0, 4.75, 6.0, 7.25)
}

data class Threw(val exceptionClass: String) {
    override fun toString(): String = "threw $exceptionClass"

    companion object {
        fun of(t: Throwable): Threw {
            val cause = if (t is InvocationTargetException) t.targetException else t
            return Threw(cause.javaClass.name)
        }
    }
}

inline fun outcome(block: () -> Any?): Any? =
    try {
        block()
    } catch (t: Throwable) {
        Threw.of(t)
    }

fun normalize(value: Any?): Any? = when (value) {
    null -> null
    is Enum<*> -> value.name
    is LocalDate -> value.toString()
    is TimeZone -> value.id
    is Duration -> value.inWholeMilliseconds
    is HebrewDate -> "HebrewDate(${value.year}-${value.month}-${value.day}, ${value.localDate}, ${value.dayOfWeek})"
    is HebrewDay -> "HebrewDay(${normalize(value.date)}, israel=${value.inIsrael}, mukefChoma=${value.mukefChoma})"
    is TefillahSummary -> listOf(
        value.tachanunAtShacharis, value.tachanunAtMincha, value.veseinTalUmatar, value.veseinBerachah, value.isVeseinTalUmatarStart,
        value.veseinTalUmatarStartsTonight, value.mashivHaruach, value.moridHatal, value.isMashivHaruachStart, value.isMashivHaruachEnd,
        value.hallel.name, value.alHanissim, value.yaalehVeyavo,
    )
    is Molad -> "Molad(${value.dayOfWeek} ${value.hour}h ${value.minute}m ${value.parts}p, ${value.date.localDate})"
    is BavliDaf -> "BavliDaf(${value.tractate}, ${value.page})"
    is YerushalmiDaf -> "YerushalmiDaf(${value.tractate}, ${value.page})"
    is Location -> listOf(value.name, value.latitude, value.longitude, value.elevation, value.timeZone.id, value.inIsrael, value.mukefChoma)
    is Geodesic -> listOf(value.distance, value.initialBearing, value.finalBearing)
    is RhumbLine -> listOf(value.distance, value.bearing)
    is Array<*> -> value.map { normalize(it) }
    is IntArray -> value.toList()
    is List<*> -> value.map { normalize(it) }
    is Map<*, *> -> value.entries.associate { normalize(it.key) to normalize(it.value) }
    is Pair<*, *> -> listOf(normalize(value.first), normalize(value.second))
    is Triple<*, *, *> -> listOf(normalize(value.first), normalize(value.second), normalize(value.third))
    else -> value
}

class Getter(val name: String, val method: Method)

object Getters {
    private val CONFIGURATION = setOf(
        "getLocation", "getDate", "getModel", "getHorizon", "getSolar", "getUseElevation", "getHadlakasNeirosMinutes",
        "getInIsrael", "getMukefChoma", "getLocalNoon", "hashCode",
    )

    fun zeroArg(type: Class<*>, returnTypes: Set<Class<*>>? = null, exclude: Set<String> = emptySet()): List<Getter> =
        type.methods.filter { m ->
            m.parameterCount == 0 && !Modifier.isStatic(m.modifiers) && !m.isBridge && !m.isSynthetic &&
                m.declaringClass != Any::class.java &&
                (m.name.startsWith("get") || m.name.startsWith("is") || m.name.startsWith("has")) &&
                m.name != "getClass" && m.name !in exclude && m.name !in CONFIGURATION &&
                (returnTypes == null || m.returnType in returnTypes)
        }.sortedBy { it.name }.map { Getter(it.name.substringBefore('-'), it) }
}

fun SnapshotRecorder.recordGetters(getters: List<Getter>, target: Any) {
    for (getter in getters) recordOutcome(getter.name) { getter.method.invoke(target) }
}

fun startOfDay(zmanim: Zmanim): Instant = zmanim.date.atStartOfDayIn(zmanim.location.timeZone)
