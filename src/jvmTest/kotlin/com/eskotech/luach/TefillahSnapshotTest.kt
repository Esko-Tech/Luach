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

import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.TachanunCustoms
import com.eskotech.luach.calendar.TefillahRules
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals

class TefillahSnapshotTest {
    private val rules: List<Method> = TefillahRules::class.java.methods
        .filter { it.parameterTypes.contentEquals(arrayOf(HebrewDay::class.java)) }
        .sortedBy { it.name }

    private val flags: List<Method> = TachanunCustoms::class.java.methods
        .filter { it.name.startsWith("get") && it.returnType == java.lang.Boolean.TYPE && it.parameterCount == 0 }
        .sortedBy { it.name }

    @Test
    fun inventoryAndDefaults() {
        assertEquals(14, rules.size, "TefillahRules rules: ${rules.map { it.name }}")
        assertEquals(12, flags.size, "TachanunCustoms flags: ${flags.map { it.name }}")
        Snapshot.single("Tefillah.defaultCustoms") { s ->
            val customs = TachanunCustoms()
            for (flag in flags) s.record(flag.name, flag.invoke(customs))
        }.assertMatches()
    }

    private fun configurations(): List<Pair<String, TefillahRules>> {
        val d = TachanunCustoms()
        val flipped = listOf(
            "omitAfterSukkos" to d.copy(omitAfterSukkos = !d.omitAfterSukkos),
            "omitWeekAfterShavuos" to d.copy(omitWeekAfterShavuos = !d.omitWeekAfterShavuos),
            "omit13SivanOutsideIsrael" to d.copy(omit13SivanOutsideIsrael = !d.omit13SivanOutsideIsrael),
            "omitPesachSheni" to d.copy(omitPesachSheni = !d.omitPesachSheni),
            "omit15IyarOutsideIsrael" to d.copy(omit15IyarOutsideIsrael = !d.omit15IyarOutsideIsrael),
            "omitMinchaErevLagBaomer" to d.copy(omitMinchaErevLagBaomer = !d.omitMinchaErevLagBaomer),
            "omitShivasYemeiHamiluim" to d.copy(omitShivasYemeiHamiluim = !d.omitShivasYemeiHamiluim),
            "omitWeekOfHod" to d.copy(omitWeekOfHod = !d.omitWeekOfHod),
            "omitWeekOfPurim" to d.copy(omitWeekOfPurim = !d.omitWeekOfPurim),
            "omitFridays" to d.copy(omitFridays = !d.omitFridays),
            "omitSundays" to d.copy(omitSundays = !d.omitSundays),
            "omitMincha" to d.copy(omitMincha = !d.omitMincha),
        )
        assertEquals(flags.size, flipped.size)
        return listOf("default customs" to TefillahRules(d)) + flipped.map { (name, customs) -> "$name flipped" to TefillahRules(customs) }
    }

    @Test
    fun allRulesEveryDay1990To2060() {
        Snapshot.collect("Tefillah.allRulesEveryDay1990To2060", (1990..2060).toList()) { s, year ->
            val configurations = configurations()
            for (date in SampleDates.year(year)) {
                for (inIsrael in BOOLEANS) for (mukefChoma in BOOLEANS) {
                    val k = HebrewDay(date, inIsrael = inIsrael, mukefChoma = mukefChoma)
                    for ((settings, tefillahRules) in configurations) {
                        s.group = settings
                        for (rule in rules) s.recordOutcome(rule.name) { rule.invoke(tefillahRules, k) }
                    }
                }
            }
        }.assertMatches()
    }

    private companion object {
        val BOOLEANS = listOf(false, true)
    }
}
