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

package com.eskotech.luach.sample

import com.eskotech.luach.calendar.TefillahRules
import com.eskotech.luach.format.LuachFormatter
import com.eskotech.luach.geo.Location
import com.eskotech.luach.zmanim.Shitos
import com.eskotech.luach.zmanim.Zmanim
import kotlinx.datetime.TimeZone

fun main(args: Array<String>) {
    val location = parseLocation(args)
    val zmanim = Zmanim.today(location)
    val day = zmanim.hebrewDay
    val english = LuachFormatter()
    val hebrew = LuachFormatter(hebrew = true)
    val tefillah = TefillahRules().all(day)

    val place = location.name ?: "${location.latitude}, ${location.longitude}"
    println("$place (${location.timeZone.id}${if (location.inIsrael) ", Eretz Yisroel" else ""}), ${zmanim.date}")
    println("${english.formatWeekday(day.date)}, ${english.formatDate(day.date)}   ${hebrew.formatWeekday(day.date)} ${hebrew.formatDate(day.date)}")
    println()
    line("Holiday", english.formatHoliday(day), hebrew.formatHoliday(day))
    line("Rosh Chodesh", english.formatRoshChodesh(day), hebrew.formatRoshChodesh(day))
    line("Omer", english.formatOmer(day), hebrew.formatOmer(day))
    line("Parsha", english.formatParsha(day.upcomingParsha), hebrew.formatParsha(day.upcomingParsha))
    line("Special Shabbos", english.formatSpecialShabbos(day), hebrew.formatSpecialShabbos(day))
    line("Daf Yomi", english.formatDaf(day.bavliDaf), hebrew.formatDaf(day.bavliDaf))
    line("Yerushalmi", english.formatDaf(day.yerushalmiDaf), hebrew.formatDaf(day.yerushalmiDaf))
    println()
    println("Tefillah")
    line("Tachanun", "${yesNo(tefillah.tachanunAtShacharis)} at shacharis, ${yesNo(tefillah.tachanunAtMincha)} at mincha")
    line("Hallel", tefillah.hallel.name.lowercase())
    line("Vesein tal umatar", yesNo(tefillah.veseinTalUmatar) + if (tefillah.veseinTalUmatarStartsTonight) ", starts tonight" else "")
    line("Mashiv haruach", yesNo(tefillah.mashivHaruach))
    line("Yaaleh veyavo", yesNo(tefillah.yaalehVeyavo))
    line("Al hanissim", yesNo(tefillah.alHanissim))
    println()
    println("Zmanim")
    for ((zman, time) in zmanim.all(Shitos.GRA)) {
        line(english.formatZman(zman), english.formatTime(time, location).ifEmpty { "none" }, hebrew.formatZman(zman))
    }
}

private fun line(label: String, value: String, hebrew: String = "") {
    if (value.isEmpty() && hebrew.isEmpty()) {
        return
    }
    println("  ${label.padEnd(26)} ${value.padEnd(30)} $hebrew".trimEnd())
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun parseLocation(args: Array<String>): Location {
    if (args.isEmpty()) {
        return Location(40.0959, -74.2177, TimeZone.of("America/New_York"), 15.0, "Lakewood, NJ")
    }
    require(args.size >= 3) { "usage: latitude longitude timeZone [elevation] [israel] [mukefchoma]" }
    val flags = args.drop(3).map { it.lowercase() }
    val elevation = flags.firstNotNullOfOrNull { it.toDoubleOrNull() } ?: 0.0
    return Location(
        latitude = args[0].toDouble(),
        longitude = args[1].toDouble(),
        timeZone = TimeZone.of(args[2]),
        elevation = elevation,
        inIsrael = "israel" in flags,
        mukefChoma = "mukefchoma" in flags,
    )
}
