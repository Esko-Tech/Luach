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

package com.eskotech.luach.zmanim

import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.Molad
import com.eskotech.luach.geo.Location
import com.eskotech.luach.solar.ApparentHorizon
import com.eskotech.luach.solar.NoaaSolarModel
import com.eskotech.luach.solar.SolarDay
import com.eskotech.luach.solar.SolarModel
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

data class Zmanim(
    val location: Location,
    val date: LocalDate,
    val model: SolarModel = NoaaSolarModel,
    val horizon: ApparentHorizon = ApparentHorizon(),
    val useElevation: Boolean = false,
    val hadlakasNeirosMinutes: Double = 18.0,
) {
    val solar: SolarDay = SolarDay(location, date, model, horizon)

    val hebrewDay: HebrewDay
        get() = HebrewDay(date, location.inIsrael, location.mukefChoma)

    val hanetz: Instant?
        get() = if (useElevation) solar.sunrise else solar.seaLevelSunrise

    val shkiah: Instant?
        get() = if (useElevation) solar.sunset else solar.seaLevelSunset

    val chatzos: Instant?
        get() = solar.solarNoon

    val fixedLocalChatzos: Instant
        get() = solar.meanSolarNoon

    val chatzosHalaylah: Instant?
        get() = solar.solarMidnight

    val hadlakasNeiros: Instant?
        get() = shkiah?.minus(hadlakasNeirosMinutes.minutes)

    val alos: Instant?
        get() = alos(DayEdge.ALOS_16_1)

    val tzais: Instant?
        get() = tzais(DayEdge.TZAIS_GEONIM_8_5)

    fun alos(edge: DayEdge): Instant? = when (edge) {
        DayEdge.Horizon -> hanetz
        is DayEdge.Depression -> solar.sunriseAt(edge.degrees)
        is DayEdge.Minutes -> hanetz?.minus(edge.minutes.minutes)
        is DayEdge.ProportionalMinutes -> proportional(edge.minutes)?.let { hanetz?.minus(it) }
        is DayEdge.Shifted -> alos(edge.edge)?.plus(edge.minutes.minutes)
        is DayEdge.DawnInterval -> dawnInterval(edge)?.let { hanetz?.minus(it) }
    }

    fun tzais(edge: DayEdge): Instant? = when (edge) {
        DayEdge.Horizon -> shkiah
        is DayEdge.Depression -> solar.sunsetAt(edge.degrees)
        is DayEdge.Minutes -> shkiah?.plus(edge.minutes.minutes)
        is DayEdge.ProportionalMinutes -> proportional(edge.minutes)?.let { shkiah?.plus(it) }
        is DayEdge.Shifted -> tzais(edge.edge)?.plus(edge.minutes.minutes)
        is DayEdge.DawnInterval -> dawnInterval(edge)?.let { shkiah?.plus(it) }
    }

    fun shaahZmanis(day: HalachicDay = HalachicDay.GRA): Duration? =
        SolarDay.temporalHour(alos(day.alos), tzais(day.tzais))

    fun hourOfDay(hours: Double, day: HalachicDay = HalachicDay.GRA): Instant? {
        val start = alos(day.alos) ?: return null
        val hour = shaahZmanis(day) ?: return null
        return start + hour * hours
    }

    fun sofZmanShema(day: HalachicDay = HalachicDay.GRA): Instant? = hourOfDay(3.0, day)

    fun sofZmanTefillah(day: HalachicDay = HalachicDay.GRA): Instant? = hourOfDay(4.0, day)

    fun sofZmanAchilasChametz(day: HalachicDay = HalachicDay.GRA): Instant? = hourOfDay(4.0, day)

    fun sofZmanBiurChametz(day: HalachicDay = HalachicDay.GRA): Instant? = hourOfDay(5.0, day)

    fun minchaGedola(day: HalachicDay = HalachicDay.GRA, atLeastHalfHourAfterChatzos: Boolean = false): Instant? {
        val proportional = hourOfDay(6.5, day) ?: return null
        if (!atLeastHalfHourAfterChatzos) {
            return proportional
        }
        val halfHour = chatzos?.plus(30.minutes) ?: return null
        return maxOf(proportional, halfHour)
    }

    fun minchaGedolaFromChatzos(day: HalachicDay): Instant? {
        val noon = chatzos ?: return null
        val hour = shaahZmanis(day) ?: return null
        return maxOf(noon + 30.minutes, noon + hour / 2)
    }

    fun samuchLeMinchaKetana(day: HalachicDay = HalachicDay.GRA): Instant? = hourOfDay(9.0, day)

    fun minchaKetana(day: HalachicDay = HalachicDay.GRA): Instant? = hourOfDay(9.5, day)

    fun plagHamincha(day: HalachicDay = HalachicDay.GRA): Instant? = hourOfDay(10.75, day)

    fun halfDayHour(hours: Double, start: Instant?, end: Instant?): Instant? {
        if (start == null || end == null) {
            return null
        }
        return start + (end - start) / 6 * hours
    }

    fun isAsurBemelacha(at: Instant, tzais: Instant?): Boolean {
        val day = hebrewDay
        val afterShkiah = shkiah?.let { at >= it } ?: false
        if (day.hasHadlakasNeiros && afterShkiah) {
            return true
        }
        return day.isAsurBemelacha && tzais != null && at <= tzais
    }

    fun zman(zman: Zman, shitos: Shitos = Shitos()): Instant? = when (zman) {
        Zman.ALOS -> alos(shitos.alos)
        Zman.MISHEYAKIR -> alos(shitos.misheyakir)
        Zman.HANETZ -> hanetz
        Zman.SOF_ZMAN_SHEMA -> sofZmanShema(shitos.day)
        Zman.SOF_ZMAN_TEFILLAH -> sofZmanTefillah(shitos.day)
        Zman.SOF_ZMAN_ACHILAS_CHAMETZ -> sofZmanAchilasChametz(shitos.day)
        Zman.SOF_ZMAN_BIUR_CHAMETZ -> sofZmanBiurChametz(shitos.day)
        Zman.CHATZOS -> chatzos
        Zman.MINCHA_GEDOLA -> minchaGedola(shitos.day)
        Zman.SAMUCH_LEMINCHA_KETANA -> samuchLeMinchaKetana(shitos.day)
        Zman.MINCHA_KETANA -> minchaKetana(shitos.day)
        Zman.PLAG_HAMINCHA -> plagHamincha(shitos.day)
        Zman.HADLAKAS_NEIROS -> hadlakasNeiros
        Zman.SHKIAH -> shkiah
        Zman.TZAIS -> tzais(shitos.tzais)
        Zman.CHATZOS_HALAYLAH -> chatzosHalaylah
    }

    fun all(shitos: Shitos = Shitos()): Map<Zman, Instant?> = Zman.entries.associateWith { zman(it, shitos) }

    val molad: Instant?
        get() = moladsAround().map { it.instant }.firstOrNull { it in today }

    fun kiddushLevanaStart(rule: KiddushLevanaStart, alos: Instant? = null, tzais: Instant? = null): Instant? {
        val time = moladsAround().map {
            when (rule) {
                KiddushLevanaStart.THREE_DAYS -> it.earliestKiddushLevana3Days
                KiddushLevanaStart.SEVEN_DAYS -> it.earliestKiddushLevana7Days
            }
        }.firstOrNull { it in today } ?: return null
        return if (alos != null && tzais != null && time in alos..tzais) tzais else time
    }

    fun kiddushLevanaEnd(rule: KiddushLevanaEnd, alos: Instant? = null, tzais: Instant? = null): Instant? {
        val time = moladsAround().map {
            when (rule) {
                KiddushLevanaEnd.HALF_MONTH -> it.latestKiddushLevanaHalfMonth
                KiddushLevanaEnd.FIFTEEN_DAYS -> it.latestKiddushLevana15Days
            }
        }.firstOrNull { it in today } ?: return null
        return if (alos != null && tzais != null && time in alos..tzais) alos else time
    }

    private val today: ClosedRange<Instant>
        get() {
            val start = date.atStartOfDayIn(location.timeZone)
            val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(location.timeZone)
            return start..end
        }

    private fun moladsAround(): List<Molad> {
        val hebrewDate = hebrewDay.date
        return listOf(hebrewDate.minusMonths(1).molad, hebrewDate.molad, hebrewDate.plusMonths(1).molad)
    }

    private fun proportional(minutes: Double): Duration? = shaahZmanis()?.let { it * (minutes / 60) }

    private fun dawnInterval(edge: DayEdge.DawnInterval): Duration? {
        val start = alos(edge.dawn) ?: return null
        val end = hanetz ?: return null
        return (end - start) * edge.factor
    }

    companion object {
        fun today(
            location: Location,
            model: SolarModel = NoaaSolarModel,
            horizon: ApparentHorizon = ApparentHorizon(),
            useElevation: Boolean = false,
            hadlakasNeirosMinutes: Double = 18.0,
        ): Zmanim = Zmanim(location, Clock.System.todayIn(location.timeZone), model, horizon, useElevation, hadlakasNeirosMinutes)
    }
}
