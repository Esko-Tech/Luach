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

import com.eskotech.luach.MILLISECOND_TOLERANCE
import com.eskotech.luach.Mismatches
import com.eskotech.luach.Places
import com.eskotech.luach.TEMPORAL_HOUR_TOLERANCE
import com.eskotech.luach.assertInstant
import com.eskotech.luach.calendar.HebrewDate
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.durationMismatch
import com.eskotech.luach.instantMismatch
import com.eskotech.luach.solar.AlmanacSolarModel
import com.eskotech.luach.solar.NoaaSolarModel
import com.eskotech.luach.solar.SolarModel
import com.eskotech.luach.zmanim.DayEdge.DawnInterval
import com.eskotech.luach.zmanim.DayEdge.Depression
import com.eskotech.luach.zmanim.DayEdge.Horizon
import com.eskotech.luach.zmanim.DayEdge.Minutes
import com.eskotech.luach.zmanim.DayEdge.ProportionalMinutes
import com.eskotech.luach.zmanim.DayEdge.Shifted
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class ZmanimTest {

    private val fixture = Zmanim(Places.LAKEWOOD, LocalDate(2017, 10, 17))

    private val geonim7 = Depression(7 + 5.0 / 60)

    private val baalHatanya = HalachicDay.degrees(1.583)

    private val ateretTorah = HalachicDay(ProportionalMinutes(72.0), Minutes(40.0))

    private fun time(label: String, expectedIso: String, actual: Instant?, tolerance: Duration = MILLISECOND_TOLERANCE): String? =
        instantMismatch(label, expectedIso, actual, tolerance)

    @Test
    fun goldenValuesForLakewood() {
        val k = fixture
        val rows = listOf(
            time("dawn 16.1", "2017-10-17T09:49:30.219906135Z", k.alos(Depression(16.1))),
            time("alos", "2017-10-17T09:49:30.219906135Z", k.alos),
            time("dawn 18", "2017-10-17T09:39:33.523030241Z", k.alos(Depression(18.0))),
            time("dawn 19", "2017-10-17T09:34:19.165263704Z", k.alos(Depression(19.0))),
            time("dawn 19.8", "2017-10-17T09:30:07.461349050Z", k.alos(Depression(19.8))),
            time("dawn 26", "2017-10-17T08:57:25.521762580Z", k.alos(Depression(26.0))),
            time("dawn 16.9", "2017-10-17T09:45:19.050001055Z", k.alos(Depression(16.9))),
            time("dawn 11.5", "2017-10-17T10:13:34.311221155Z", k.alos(Depression(11.5))),
            time("dawn 11", "2017-10-17T10:16:11.433376090Z", k.alos(Depression(11.0))),
            time("dawn 10.2", "2017-10-17T10:20:22.960720797Z", k.alos(Depression(10.2))),
            time("dawn 9.5", "2017-10-17T10:24:03.203710034Z", k.alos(Depression(9.5))),
            time("dawn 7.65", "2017-10-17T10:33:46.155182911Z", k.alos(Depression(7.65))),
            time("dusk 13.24", "2017-10-17T23:19:17.914866983Z", k.tzais(Depression(13.24))),
            time("13.5 minutes before 7.083", "2017-10-17T22:33:33.446858525Z", k.tzais(Shifted(geonim7, -13.5))),
            time("dusk -2.1", "2017-10-17T21:58:15.259245832Z", k.tzais(Depression(-2.1))),
            time("dusk -2.8", "2017-10-17T21:54:28.539077180Z", k.tzais(Depression(-2.8))),
            time("dusk -3.05", "2017-10-17T21:53:07.410874002Z", k.tzais(Depression(-3.05))),
            time("dusk 3.7", "2017-10-17T22:29:12.858867416Z", k.tzais(Depression(3.7))),
            time("dusk 3.8", "2017-10-17T22:29:44.614329739Z", k.tzais(Depression(3.8))),
            time("dusk 4.8", "2017-10-17T22:35:01.761613150Z", k.tzais(Depression(4.8))),
            time("dusk 5.95", "2017-10-17T22:41:05.633594744Z", k.tzais(Depression(5.95))),
            time("dusk 6", "2017-10-17T22:41:21.435143220Z", k.tzais(Depression(6.0))),
            time("dusk 6.45", "2017-10-17T22:43:43.582328133Z", k.tzais(Depression(6.45))),
            time("dusk 7.083", "2017-10-17T22:47:03.446858525Z", k.tzais(geonim7)),
            time("dusk 7.67", "2017-10-17T22:50:08.395485971Z", k.tzais(Depression(7.67))),
            time("dusk 8.5", "2017-10-17T22:54:29.772724455Z", k.tzais(Depression(8.5))),
            time("tzais", "2017-10-17T22:54:29.772724455Z", k.tzais),
            time("dusk 9.3", "2017-10-17T22:58:41.421314719Z", k.tzais(Depression(9.3))),
            time("dusk 9.75", "2017-10-17T23:01:02.865384009Z", k.tzais(Depression(9.75))),
            time("dusk 16.1", "2017-10-17T23:34:14.100766559Z", k.tzais(Depression(16.1))),
            time("dusk 18", "2017-10-17T23:44:09.707296295Z", k.tzais(Depression(18.0))),
            time("dusk 19.8", "2017-10-17T23:53:34.647710389Z", k.tzais(Depression(19.8))),
            time("dusk 26", "2017-10-18T00:26:11.906390602Z", k.tzais(Depression(26.0))),
            time("fixed local chatzos", "2017-10-17T16:56:57.605832Z", k.fixedLocalChatzos),
            time("half hour after fixed local chatzos", "2017-10-17T17:26:57.605832Z", k.fixedLocalChatzos + 30.minutes),
            time("half day", "2017-10-17T16:41:55.302308378Z", k.hourOfDay(6.0), TEMPORAL_HOUR_TOLERANCE),
            time("mincha gedola 16.1", "2017-10-17T17:16:13.988705527Z", k.minchaGedola(HalachicDay.MAGEN_AVRAHAM_16_1), TEMPORAL_HOUR_TOLERANCE),
            time("mincha gedola ateret torah", "2017-10-17T17:00:49.269815683Z", k.minchaGedola(ateretTorah), TEMPORAL_HOUR_TOLERANCE),
            time("mincha gedola baal hatanya", "2017-10-17T17:09:55.476611672Z", k.minchaGedola(baalHatanya), TEMPORAL_HOUR_TOLERANCE),
            time("mincha ketana 16.1", "2017-10-17T20:42:24.958920631Z", k.minchaKetana(HalachicDay.MAGEN_AVRAHAM_16_1), TEMPORAL_HOUR_TOLERANCE),
            time("mincha ketana 16.1 to 3.8", "2017-10-17T19:51:21.615491489Z", k.minchaKetana(HalachicDay.degrees(16.1, 3.8)), TEMPORAL_HOUR_TOLERANCE),
            time("mincha ketana baal hatanya", "2017-10-17T19:57:57.216239627Z", k.minchaKetana(baalHatanya), TEMPORAL_HOUR_TOLERANCE),
            time("plag 16.1 to 3.8", "2017-10-17T21:10:33.114910614Z", k.plagHamincha(HalachicDay.degrees(16.1, 3.8)), TEMPORAL_HOUR_TOLERANCE),
            time("plag 16.1 to 7.083", "2017-10-17T21:26:03.735717649Z", k.plagHamincha(HalachicDay(Depression(16.1), geonim7)), TEMPORAL_HOUR_TOLERANCE),
            time("plag 16.1", "2017-10-17T22:08:19.529843591Z", k.plagHamincha(HalachicDay.MAGEN_AVRAHAM_16_1), TEMPORAL_HOUR_TOLERANCE),
            time("plag 18", "2017-10-17T22:16:10.938101909Z", k.plagHamincha(HalachicDay.degrees(18.0)), TEMPORAL_HOUR_TOLERANCE),
            time("plag 19.8", "2017-10-17T22:23:38.065797743Z", k.plagHamincha(HalachicDay.degrees(19.8)), TEMPORAL_HOUR_TOLERANCE),
            time("plag 26", "2017-10-17T22:49:27.074658511Z", k.plagHamincha(HalachicDay.degrees(26.0)), TEMPORAL_HOUR_TOLERANCE),
            time("plag baal hatanya", "2017-10-17T21:07:57.941084608Z", k.plagHamincha(baalHatanya), TEMPORAL_HOUR_TOLERANCE),
            time("samuch le mincha ketana 16.1", "2017-10-17T20:08:03.130551447Z", k.samuchLeMinchaKetana(HalachicDay.MAGEN_AVRAHAM_16_1), TEMPORAL_HOUR_TOLERANCE),
            time("shema 16.1 to 7.083", "2017-10-17T13:03:53.526644232Z", k.sofZmanShema(HalachicDay(Depression(16.1), geonim7)), TEMPORAL_HOUR_TOLERANCE),
            time("shema baal hatanya", "2017-10-17T13:53:53.447045725Z", k.sofZmanShema(baalHatanya), TEMPORAL_HOUR_TOLERANCE),
            time("shema 16.1", "2017-10-17T13:15:41.190121239Z", k.sofZmanShema(HalachicDay.MAGEN_AVRAHAM_16_1), TEMPORAL_HOUR_TOLERANCE),
            time("shema 16.1 to fixed local chatzos", "2017-10-17T13:23:13.912869066Z", k.halfDayHour(3.0, k.alos(Depression(16.1)), k.fixedLocalChatzos), TEMPORAL_HOUR_TOLERANCE),
            time("shema 18", "2017-10-17T13:10:42.569096753Z", k.sofZmanShema(HalachicDay.degrees(18.0)), TEMPORAL_HOUR_TOLERANCE),
            time("shema 18 to fixed local chatzos", "2017-10-17T13:18:15.564431120Z", k.halfDayHour(3.0, k.alos(Depression(18.0)), k.fixedLocalChatzos), TEMPORAL_HOUR_TOLERANCE),
            time("shema 19.8", "2017-10-17T13:05:59.257939383Z", k.sofZmanShema(HalachicDay.degrees(19.8)), TEMPORAL_HOUR_TOLERANCE),
            time("tefillah baal hatanya", "2017-10-17T14:49:54.026921710Z", k.sofZmanTefillah(baalHatanya), TEMPORAL_HOUR_TOLERANCE),
            time("tefillah 16.1", "2017-10-17T14:24:24.846859607Z", k.sofZmanTefillah(HalachicDay.MAGEN_AVRAHAM_16_1), TEMPORAL_HOUR_TOLERANCE),
            time("tefillah 18", "2017-10-17T14:21:05.584452257Z", k.sofZmanTefillah(HalachicDay.degrees(18.0)), TEMPORAL_HOUR_TOLERANCE),
            time("tefillah 19.8", "2017-10-17T14:17:56.523469494Z", k.sofZmanTefillah(HalachicDay.degrees(19.8)), TEMPORAL_HOUR_TOLERANCE),
            durationMismatch("hour 16.1", "PT1H8M43.656738368S", k.shaahZmanis(HalachicDay.MAGEN_AVRAHAM_16_1), MILLISECOND_TOLERANCE),
            durationMismatch("hour 18", "PT1H10M23.015355504S", k.shaahZmanis(HalachicDay.degrees(18.0)), MILLISECOND_TOLERANCE),
            durationMismatch("hour 19.8", "PT1H11M57.265530111S", k.shaahZmanis(HalachicDay.degrees(19.8)), MILLISECOND_TOLERANCE),
            durationMismatch("hour 26", "PT1H17M23.865385668S", k.shaahZmanis(HalachicDay.degrees(26.0)), MILLISECOND_TOLERANCE),
            durationMismatch("hour 16.1 to 3.7", "PT1H3M18.553246773S", k.shaahZmanis(HalachicDay.degrees(16.1, 3.7)), MILLISECOND_TOLERANCE),
            durationMismatch("hour 16.1 to 3.8", "PT1H3M21.1995353S", k.shaahZmanis(HalachicDay.degrees(16.1, 3.8)), MILLISECOND_TOLERANCE),
            durationMismatch("hour 16.1 to 7.083", "PT1H4M47.768912699S", k.shaahZmanis(HalachicDay(Depression(16.1), geonim7)), MILLISECOND_TOLERANCE),
            durationMismatch("hour baal hatanya", "PT56M0.579875985S", k.shaahZmanis(baalHatanya), MILLISECOND_TOLERANCE),
        )
        val failures = rows.filterNotNull()
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        assertNull(k.molad)
        assertNull(k.kiddushLevanaStart(KiddushLevanaStart.THREE_DAYS))
        assertNull(k.kiddushLevanaStart(KiddushLevanaStart.SEVEN_DAYS))
        assertNull(k.kiddushLevanaEnd(KiddushLevanaEnd.HALF_MONTH))
        assertNull(k.kiddushLevanaEnd(KiddushLevanaEnd.FIFTEEN_DAYS))
    }

    private class Config(val model: SolarModel, val useElevation: Boolean, val candle: Double) {
        override fun toString(): String = "${model.name} elevation=$useElevation candle=$candle"
    }

    private val configs = listOf(NoaaSolarModel, AlmanacSolarModel).flatMap { model ->
        listOf(false, true).map { Config(model, it, if (it) 40.0 else 18.0) }
    }

    private val dates = listOf(
        LocalDate(2024, 3, 20), LocalDate(2024, 6, 20), LocalDate(2024, 9, 22), LocalDate(2024, 12, 21),
        LocalDate(2017, 10, 17), LocalDate(2026, 10, 22), LocalDate(2026, 10, 25), LocalDate(2026, 11, 10),
    )

    private fun calendars(): List<Zmanim> = Places.SOLAR_SET.flatMap { location ->
        dates.flatMap { date ->
            configs.map { Zmanim(location, date, it.model, useElevation = it.useElevation, hadlakasNeirosMinutes = it.candle) }
        }
    }

    @Test
    fun edgesAndDaysFollowTheirDefinitions() {
        val mismatches = Mismatches()
        var compared = 0
        for (k in calendars()) {
            val sunrise = if (k.useElevation) k.solar.sunrise else k.solar.seaLevelSunrise
            val sunset = if (k.useElevation) k.solar.sunset else k.solar.seaLevelSunset
            mismatches.check(sunrise != null && sunset != null) { "$k: base events missing" }
            if (sunrise == null || sunset == null) continue
            val hour = (sunset - sunrise) / 12
            fun check(label: String, actual: Instant?, expected: Instant?) {
                compared++
                val same = if (actual == null || expected == null) actual == expected else (actual - expected).absoluteValue <= 1.milliseconds
                mismatches.check(same) { "$k: $label = $actual, definition gives $expected" }
            }
            check("sunrise", k.hanetz, sunrise)
            check("sunset", k.shkiah, sunset)
            check("dawn horizon", k.alos(Horizon), sunrise)
            check("dusk horizon", k.tzais(Horizon), sunset)
            check("dawn 72", k.alos(Minutes(72.0)), sunrise - 72.minutes)
            check("dusk 72", k.tzais(Minutes(72.0)), sunset + 72.minutes)
            check("dawn -18", k.tzais(Minutes(-18.0)), sunset - 18.minutes)
            check("dawn 72 proportional", k.alos(ProportionalMinutes(72.0)), sunrise - hour * 1.2)
            check("dusk 90 proportional", k.tzais(ProportionalMinutes(90.0)), sunset + hour * 1.5)
            check("dawn 16.1", k.alos(Depression(16.1)), k.solar.sunriseAt(16.1))
            check("dusk 8.5", k.tzais(Depression(8.5)), k.solar.sunsetAt(8.5))
            check("shifted", k.tzais(Shifted(Depression(7.083), -13.5)), k.solar.sunsetAt(7.083)!! - 13.5.minutes)
            check("shifted dawn", k.alos(Shifted(Minutes(72.0), 10.0)), sunrise - 62.minutes)
            val alos19 = k.solar.sunriseAt(19.8)
            if (alos19 == null) {
                assertNull(k.tzais(DawnInterval(Depression(19.8), 5 / 18.0)))
                assertNull(k.alos(DawnInterval(Depression(19.8), 0.5)))
            } else {
                check("dawn interval", k.tzais(DawnInterval(Depression(19.8), 5 / 18.0)), sunset + (sunrise - alos19) * (5 / 18.0))
                check("dawn interval dawn", k.alos(DawnInterval(Depression(19.8), 0.5)), sunrise - (sunrise - alos19) * 0.5)
            }
            check("candle lighting", k.hadlakasNeiros, sunset - k.hadlakasNeirosMinutes.minutes)
            check("chatzos", k.chatzos, k.solar.solarNoon)
            check("solar midnight", k.chatzosHalaylah, k.solar.solarMidnight)
            check("fixed local chatzos", k.fixedLocalChatzos, k.date.atStartOfDayIn(kotlinx.datetime.TimeZone.UTC) + 12.hours - (k.location.longitude * 4).minutes)
            for (day in listOf(HalachicDay.GRA, HalachicDay.MAGEN_AVRAHAM_72, HalachicDay.MAGEN_AVRAHAM_16_1, ateretTorah, baalHatanya, HalachicDay.degrees(16.1, 3.8))) {
                val start = k.alos(day.alos)
                val end = k.tzais(day.tzais)
                if (start == null || end == null) {
                    compared++
                    mismatches.check(k.shaahZmanis(day) == null && k.sofZmanShema(day) == null && k.plagHamincha(day) == null) { "$k $day: nulls do not propagate" }
                    continue
                }
                val dayHour = (end - start) / 12
                compared++
                mismatches.check(k.shaahZmanis(day) == dayHour) { "$k $day: temporal hour ${k.shaahZmanis(day)} expected $dayHour" }
                check("$day shema", k.sofZmanShema(day), start + dayHour * 3)
                check("$day tefillah", k.sofZmanTefillah(day), start + dayHour * 4)
                check("$day chametz eating", k.sofZmanAchilasChametz(day), start + dayHour * 4)
                check("$day chametz burning", k.sofZmanBiurChametz(day), start + dayHour * 5)
                check("$day mincha gedola", k.minchaGedola(day), start + dayHour * 6.5)
                check("$day mincha gedola 30", k.minchaGedola(day, atLeastHalfHourAfterChatzos = true), maxOf(start + dayHour * 6.5, k.chatzos!! + 30.minutes))
                check("$day mincha gedola from chatzos", k.minchaGedolaFromChatzos(day), maxOf(k.chatzos!! + 30.minutes, k.chatzos!! + dayHour / 2))
                check("$day samuch", k.samuchLeMinchaKetana(day), start + dayHour * 9)
                check("$day mincha ketana", k.minchaKetana(day), start + dayHour * 9.5)
                check("$day plag", k.plagHamincha(day), start + dayHour * 10.75)
                check("$day end", k.hourOfDay(12.0, day), end)
                check("$day half day", k.halfDayHour(3.0, start, k.fixedLocalChatzos), start + (k.fixedLocalChatzos - start) / 6 * 3)
            }
            assertNull(k.halfDayHour(2.0, null, k.fixedLocalChatzos))
        }
        mismatches.assertNone("zmanim definitions")
        assertTrue(compared > 5_000, "compared $compared")
    }

    private fun window(zman: Instant, start: Instant, end: Instant, dawn: Instant?, dusk: Instant?, snapTo: Instant?): Instant? {
        if (zman < start || zman > end) {
            return null
        }
        return if (dawn != null && dusk != null && zman in dawn..dusk) snapTo else zman
    }

    @Test
    fun kiddushLevanaZmanimFollowTheMolad() {
        val mismatches = Mismatches()
        var date = LocalDate(2024, 1, 1)
        val end = LocalDate(2027, 1, 1)
        var snapped = 0
        while (date < end) {
            val k = Zmanim(Places.LAKEWOOD, date)
            val hebrew = HebrewDate.from(date)
            val start = date.atStartOfDayIn(Places.LAKEWOOD.timeZone)
            val finish = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(Places.LAKEWOOD.timeZone)
            val dawn = k.alos(Minutes(72.0))
            val dusk = k.tzais(Minutes(72.0))
            val molads = listOf(hebrew.minusMonths(1).molad, hebrew.molad, hebrew.plusMonths(1).molad)
            fun expected(times: List<Instant>, bounds: Boolean, snapTo: Instant?): Instant? =
                times.firstNotNullOfOrNull { window(it, start, finish, if (bounds) dawn else null, if (bounds) dusk else null, snapTo) }
            fun check(name: String, actual: Instant?, expected: Instant?) {
                mismatches.check(actual == expected) { "$date (${hebrew.day} ${hebrew.month}): $name $actual expected $expected" }
            }
            check("molad", k.molad, expected(molads.map { it.instant }, false, null))
            check("start 3", k.kiddushLevanaStart(KiddushLevanaStart.THREE_DAYS), expected(molads.map { it.earliestKiddushLevana3Days }, false, null))
            check("start 7", k.kiddushLevanaStart(KiddushLevanaStart.SEVEN_DAYS), expected(molads.map { it.earliestKiddushLevana7Days }, false, null))
            check("end half", k.kiddushLevanaEnd(KiddushLevanaEnd.HALF_MONTH), expected(molads.map { it.latestKiddushLevanaHalfMonth }, false, null))
            check("end 15", k.kiddushLevanaEnd(KiddushLevanaEnd.FIFTEEN_DAYS), expected(molads.map { it.latestKiddushLevana15Days }, false, null))
            val bounded3 = k.kiddushLevanaStart(KiddushLevanaStart.THREE_DAYS, dawn, dusk)
            check("start 3 bounded", bounded3, expected(molads.map { it.earliestKiddushLevana3Days }, true, dusk))
            check("start 7 bounded", k.kiddushLevanaStart(KiddushLevanaStart.SEVEN_DAYS, dawn, dusk), expected(molads.map { it.earliestKiddushLevana7Days }, true, dusk))
            check("end half bounded", k.kiddushLevanaEnd(KiddushLevanaEnd.HALF_MONTH, dawn, dusk), expected(molads.map { it.latestKiddushLevanaHalfMonth }, true, dawn))
            check("end 15 bounded", k.kiddushLevanaEnd(KiddushLevanaEnd.FIFTEEN_DAYS, dawn, dusk), expected(molads.map { it.latestKiddushLevana15Days }, true, dawn))
            if (bounded3 != null && bounded3 == dusk) snapped++
            date = date.plus(1, DateTimeUnit.DAY)
        }
        mismatches.assertNone("kiddush levana")
        assertTrue(snapped > 5, "daytime starts snapped to nightfall: $snapped")
    }

    @Test
    fun eachMoladBasedZmanAppearsOncePerMonth() {
        var date = LocalDate(2024, 1, 1)
        val end = LocalDate(2026, 1, 1)
        var molads = 0
        var starts3 = 0
        var starts7 = 0
        var ends15 = 0
        var endsHalf = 0
        while (date < end) {
            val k = Zmanim(Places.LAKEWOOD, date)
            if (k.molad != null) molads++
            if (k.kiddushLevanaStart(KiddushLevanaStart.THREE_DAYS) != null) starts3++
            if (k.kiddushLevanaStart(KiddushLevanaStart.SEVEN_DAYS) != null) starts7++
            if (k.kiddushLevanaEnd(KiddushLevanaEnd.FIFTEEN_DAYS) != null) ends15++
            if (k.kiddushLevanaEnd(KiddushLevanaEnd.HALF_MONTH) != null) endsHalf++
            date = date.plus(1, DateTimeUnit.DAY)
        }
        val months = 25
        for ((label, count) in listOf("molads" to molads, "starts 3" to starts3, "starts 7" to starts7, "ends 15" to ends15, "ends half" to endsHalf)) {
            assertTrue(count in months - 1..months + 1, "$label $count for about $months months")
        }
    }

    @Test
    fun rememberedMoladTimes() {
        assertInstant("molad", "2017-10-20T09:52:00.170666666Z", Zmanim(Places.LAKEWOOD, LocalDate(2017, 10, 20)).molad)
        assertInstant("start 3 days", "2017-10-23T09:52:00.170666666Z", Zmanim(Places.LAKEWOOD, LocalDate(2017, 10, 23)).kiddushLevanaStart(KiddushLevanaStart.THREE_DAYS))
        assertInstant("start 7 days", "2017-10-27T09:52:00.170666666Z", Zmanim(Places.LAKEWOOD, LocalDate(2017, 10, 27)).kiddushLevanaStart(KiddushLevanaStart.SEVEN_DAYS))
        assertInstant("end 15 days", "2017-10-05T21:07:56.837333333Z", Zmanim(Places.LAKEWOOD, LocalDate(2017, 10, 5)).kiddushLevanaEnd(KiddushLevanaEnd.FIFTEEN_DAYS))
        assertInstant("end half month", "2017-10-05T15:29:58.503333333Z", Zmanim(Places.LAKEWOOD, LocalDate(2017, 10, 5)).kiddushLevanaEnd(KiddushLevanaEnd.HALF_MONTH))
        val erevPesach = Zmanim(Places.LAKEWOOD, LocalDate(2017, 4, 10))
        assertTrue(erevPesach.sofZmanAchilasChametz() != null && erevPesach.sofZmanBiurChametz(HalachicDay.MAGEN_AVRAHAM_72) != null)
        assertTrue(erevPesach.sofZmanAchilasChametz()!! < erevPesach.sofZmanBiurChametz()!!)
    }

    @Test
    fun melachaDependsOnSunsetAndNightfall() {
        val friday = Zmanim(Places.LAKEWOOD, LocalDate(2026, 10, 23))
        val sunsetFriday = friday.shkiah!!
        assertFalse(friday.isAsurBemelacha(sunsetFriday - 1.minutes, friday.tzais))
        assertTrue(friday.isAsurBemelacha(sunsetFriday, friday.tzais))
        val shabbos = Zmanim(Places.LAKEWOOD, LocalDate(2026, 10, 24))
        val tzais = shabbos.tzais!!
        assertTrue(shabbos.isAsurBemelacha(tzais - 1.minutes, tzais))
        assertTrue(shabbos.isAsurBemelacha(tzais, tzais))
        assertFalse(shabbos.isAsurBemelacha(tzais + 1.minutes, tzais))
        assertFalse(shabbos.isAsurBemelacha(tzais - 1.minutes, null))
        val tuesday = Zmanim(Places.LAKEWOOD, LocalDate(2026, 10, 20))
        assertFalse(tuesday.isAsurBemelacha(tuesday.shkiah!!, tuesday.tzais))
        val erevShavuos = Zmanim(Places.LAKEWOOD, LocalDate(2025, 6, 1))
        assertTrue(erevShavuos.isAsurBemelacha(erevShavuos.shkiah!!, erevShavuos.tzais))
        assertTrue(erevShavuos.copy(location = Places.LAKEWOOD.copy(inIsrael = true)).isAsurBemelacha(erevShavuos.shkiah!!, erevShavuos.tzais))
        val secondDay = Zmanim(Places.LAKEWOOD, LocalDate(2025, 6, 3))
        assertTrue(secondDay.isAsurBemelacha(secondDay.hanetz!!, secondDay.tzais))
        assertFalse(secondDay.copy(location = Places.LAKEWOOD.copy(inIsrael = true)).isAsurBemelacha(secondDay.hanetz!!, secondDay.tzais))
        assertTrue(HebrewDay(LocalDate(2025, 6, 1)).hasHadlakasNeiros)
    }

    @Test
    fun polarDayAndNightGiveNullEverywhere() {
        val summer = Zmanim(Places.NORTHERN_NORWAY, LocalDate(2024, 6, 21))
        assertNull(summer.hanetz)
        assertNull(summer.shkiah)
        assertNull(summer.sofZmanShema())
        assertNull(summer.plagHamincha())
        assertNull(summer.alos(Minutes(72.0)))
        assertNull(summer.tzais(Minutes(72.0)))
        assertNull(summer.shaahZmanis())
        assertNull(summer.shaahZmanis(HalachicDay.MAGEN_AVRAHAM_72))
        assertNull(summer.alos(ProportionalMinutes(72.0)))
        assertNull(summer.tzais(DawnInterval(Depression(19.8), 0.5)))
        assertNull(summer.hadlakasNeiros)
        assertNull(summer.minchaGedola(atLeastHalfHourAfterChatzos = true))
        assertNull(summer.minchaGedolaFromChatzos(HalachicDay.GRA))
        assertTrue(summer.chatzos != null)
        val winter = Zmanim(Places.NORTHERN_NORWAY, LocalDate(2024, 12, 21))
        assertNull(winter.hanetz)
        assertNull(winter.shkiah)
        assertNull(winter.sofZmanTefillah(HalachicDay.MAGEN_AVRAHAM_72))
        assertTrue(winter.sofZmanTefillah(HalachicDay.MAGEN_AVRAHAM_16_1) != null)
    }

    @Test
    fun settingsAndCopies() {
        val original = Zmanim(Places.LAKEWOOD, LocalDate(2026, 10, 22), AlmanacSolarModel, useElevation = true, hadlakasNeirosMinutes = 22.5)
        val copy = original.copy(date = LocalDate(2026, 10, 23))
        assertEquals(LocalDate(2026, 10, 22), original.date)
        assertEquals(LocalDate(2026, 10, 23), copy.date)
        assertEquals(original.location, copy.location)
        assertTrue(copy.useElevation)
        assertEquals(22.5, copy.hadlakasNeirosMinutes)
        assertEquals(AlmanacSolarModel, copy.model)
        assertEquals(original, original.copy())
        val sea = original.copy(useElevation = false)
        assertTrue(sea.sofZmanShema() != original.sofZmanShema())
        assertEquals(sea.solar.seaLevelSunrise, sea.hanetz)
        assertEquals(original.solar.sunrise, original.hanetz)
        assertEquals(HalachicDay(Horizon, Horizon), HalachicDay.GRA)
        assertEquals(HalachicDay(Minutes(72.0), Minutes(72.0)), HalachicDay.MAGEN_AVRAHAM_72)
        assertEquals(HalachicDay(Depression(16.1), Depression(16.1)), HalachicDay.MAGEN_AVRAHAM_16_1)
        assertEquals(HalachicDay(ProportionalMinutes(90.0), ProportionalMinutes(90.0)), HalachicDay.proportionalMinutes(90.0))
        assertEquals(HalachicDay(Depression(16.1), Depression(3.8)), HalachicDay.degrees(16.1, 3.8))
    }

    @Test
    fun standardListMatchesTheIndividualZmanim() {
        val k = fixture
        val all = k.all()
        assertEquals(Zman.entries, all.keys.toList())
        assertEquals(k.alos(DayEdge.ALOS_16_1), all[Zman.ALOS])
        assertEquals(k.alos(DayEdge.MISHEYAKIR_11_5), all[Zman.MISHEYAKIR])
        assertEquals(k.hanetz, all[Zman.HANETZ])
        assertEquals(k.sofZmanShema(), all[Zman.SOF_ZMAN_SHEMA])
        assertEquals(k.sofZmanTefillah(), all[Zman.SOF_ZMAN_TEFILLAH])
        assertEquals(k.sofZmanAchilasChametz(), all[Zman.SOF_ZMAN_ACHILAS_CHAMETZ])
        assertEquals(k.sofZmanBiurChametz(), all[Zman.SOF_ZMAN_BIUR_CHAMETZ])
        assertEquals(k.chatzos, all[Zman.CHATZOS])
        assertEquals(k.minchaGedola(), all[Zman.MINCHA_GEDOLA])
        assertEquals(k.samuchLeMinchaKetana(), all[Zman.SAMUCH_LEMINCHA_KETANA])
        assertEquals(k.minchaKetana(), all[Zman.MINCHA_KETANA])
        assertEquals(k.plagHamincha(), all[Zman.PLAG_HAMINCHA])
        assertEquals(k.hadlakasNeiros, all[Zman.HADLAKAS_NEIROS])
        assertEquals(k.shkiah, all[Zman.SHKIAH])
        assertEquals(k.tzais(DayEdge.TZAIS_GEONIM_8_5), all[Zman.TZAIS])
        assertEquals(k.chatzosHalaylah, all[Zman.CHATZOS_HALAYLAH])
        for (zman in Zman.entries) assertEquals(all[zman], k.zman(zman), "$zman")
        val magenAvraham = k.all(Shitos.MAGEN_AVRAHAM)
        assertEquals(k.alos(Minutes(72.0)), magenAvraham[Zman.ALOS])
        assertEquals(k.sofZmanShema(HalachicDay.MAGEN_AVRAHAM_72), magenAvraham[Zman.SOF_ZMAN_SHEMA])
        assertEquals(k.plagHamincha(HalachicDay.MAGEN_AVRAHAM_72), magenAvraham[Zman.PLAG_HAMINCHA])
        assertEquals(k.tzais(Minutes(72.0)), magenAvraham[Zman.TZAIS])
        assertEquals(k.hanetz, magenAvraham[Zman.HANETZ])
        val custom = Shitos(alos = Depression(19.8), misheyakir = Depression(10.2), day = baalHatanya, tzais = Depression(6.0))
        assertEquals(k.alos(Depression(19.8)), k.zman(Zman.ALOS, custom))
        assertEquals(k.alos(Depression(10.2)), k.zman(Zman.MISHEYAKIR, custom))
        assertEquals(k.minchaKetana(baalHatanya), k.zman(Zman.MINCHA_KETANA, custom))
        assertEquals(k.tzais(Depression(6.0)), k.zman(Zman.TZAIS, custom))
        val summer = Zmanim(Places.NORTHERN_NORWAY, LocalDate(2024, 6, 21)).all()
        assertNull(summer[Zman.HANETZ])
        assertNull(summer[Zman.SOF_ZMAN_SHEMA])
        assertTrue(summer[Zman.CHATZOS] != null)
    }

    @Test
    fun namedEdgesDaysAndShitos() {
        val edges = listOf(
            DayEdge.ALOS_16_1 to Depression(16.1), DayEdge.ALOS_18 to Depression(18.0), DayEdge.ALOS_19_8 to Depression(19.8), DayEdge.ALOS_26 to Depression(26.0),
            DayEdge.ALOS_BAAL_HATANYA to Depression(16.9), DayEdge.ALOS_72 to Minutes(72.0), DayEdge.ALOS_90 to Minutes(90.0), DayEdge.ALOS_120 to Minutes(120.0),
            DayEdge.ALOS_72_ZMANIYOS to ProportionalMinutes(72.0),
            DayEdge.MISHEYAKIR_11_5 to Depression(11.5), DayEdge.MISHEYAKIR_11 to Depression(11.0), DayEdge.MISHEYAKIR_10_2 to Depression(10.2),
            DayEdge.MISHEYAKIR_9_5 to Depression(9.5), DayEdge.MISHEYAKIR_7_65 to Depression(7.65),
            DayEdge.TZAIS_GEONIM_3_7 to Depression(3.7), DayEdge.TZAIS_GEONIM_3_8 to Depression(3.8), DayEdge.TZAIS_GEONIM_5_95 to Depression(5.95),
            DayEdge.TZAIS_GEONIM_7_083 to geonim7, DayEdge.TZAIS_GEONIM_8_5 to Depression(8.5), DayEdge.TZAIS_BAAL_HATANYA to Depression(6.0),
            DayEdge.TZAIS_RABBEINU_TAM_13_24 to Depression(13.24), DayEdge.TZAIS_16_1 to Depression(16.1), DayEdge.TZAIS_18 to Depression(18.0),
            DayEdge.TZAIS_19_8 to Depression(19.8), DayEdge.TZAIS_26 to Depression(26.0), DayEdge.TZAIS_72 to Minutes(72.0), DayEdge.TZAIS_90 to Minutes(90.0),
            DayEdge.TZAIS_96 to Minutes(96.0), DayEdge.TZAIS_120 to Minutes(120.0), DayEdge.TZAIS_72_ZMANIYOS to ProportionalMinutes(72.0),
        )
        assertEquals(30, edges.size)
        for ((named, expected) in edges) assertEquals(expected, named)
        assertEquals(HalachicDay(Horizon, Horizon), HalachicDay.GRA)
        assertEquals(HalachicDay(Minutes(72.0), Minutes(72.0)), HalachicDay.MAGEN_AVRAHAM_72)
        assertEquals(HalachicDay(Minutes(90.0), Minutes(90.0)), HalachicDay.MAGEN_AVRAHAM_90)
        assertEquals(HalachicDay(Minutes(120.0), Minutes(120.0)), HalachicDay.MAGEN_AVRAHAM_120)
        assertEquals(HalachicDay(ProportionalMinutes(72.0), ProportionalMinutes(72.0)), HalachicDay.MAGEN_AVRAHAM_72_ZMANIYOS)
        assertEquals(HalachicDay(Depression(16.1), Depression(16.1)), HalachicDay.MAGEN_AVRAHAM_16_1)
        assertEquals(HalachicDay(Depression(18.0), Depression(18.0)), HalachicDay.MAGEN_AVRAHAM_18)
        assertEquals(HalachicDay(Depression(19.8), Depression(19.8)), HalachicDay.MAGEN_AVRAHAM_19_8)
        assertEquals(HalachicDay(Depression(26.0), Depression(26.0)), HalachicDay.MAGEN_AVRAHAM_26)
        assertEquals(HalachicDay.degrees(1.583), HalachicDay.BAAL_HATANYA)
        assertEquals(ateretTorah, HalachicDay.ATERET_TORAH)
        assertEquals(Shitos(DayEdge.ALOS_16_1, DayEdge.MISHEYAKIR_11_5, HalachicDay.GRA, DayEdge.TZAIS_GEONIM_8_5), Shitos.GRA)
        assertEquals(Shitos(DayEdge.ALOS_72, DayEdge.MISHEYAKIR_11_5, HalachicDay.MAGEN_AVRAHAM_72, DayEdge.TZAIS_72), Shitos.MAGEN_AVRAHAM)
        assertEquals(Shitos.GRA, Shitos())
    }

    @Test
    fun todayAndTheLocationFlags() {
        val today = Zmanim.today(Places.LAKEWOOD)
        assertEquals(Clock.System.todayIn(Places.LAKEWOOD.timeZone), today.date)
        assertEquals(Places.LAKEWOOD, today.location)
        assertFalse(today.hebrewDay.inIsrael)
        val elevated = Zmanim.today(Places.LAKEWOOD, AlmanacSolarModel, useElevation = true, hadlakasNeirosMinutes = 40.0)
        assertEquals(today.date, elevated.date)
        assertTrue(elevated.useElevation)
        assertEquals(40.0, elevated.hadlakasNeirosMinutes)
        assertEquals(AlmanacSolarModel, elevated.model)
        val jerusalem = Zmanim(Places.JERUSALEM, LocalDate(2025, 6, 3))
        assertTrue(jerusalem.hebrewDay.inIsrael)
        assertTrue(jerusalem.hebrewDay.mukefChoma)
        assertEquals(HebrewDay(LocalDate(2025, 6, 3), inIsrael = true, mukefChoma = true), jerusalem.hebrewDay)
        assertEquals(HebrewDay(LocalDate(2025, 6, 3)), Zmanim(Places.LAKEWOOD, LocalDate(2025, 6, 3)).hebrewDay)
        assertFalse(jerusalem.isAsurBemelacha(jerusalem.hanetz!!, jerusalem.tzais))
    }
}
