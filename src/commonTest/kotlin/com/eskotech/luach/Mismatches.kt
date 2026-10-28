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

import kotlin.test.fail

internal class Mismatches(private val limit: Int = 25) {
    private val samples = ArrayList<String>()

    var count: Int = 0
        private set

    fun check(condition: Boolean, message: () -> String) {
        if (!condition) {
            record(message())
        }
    }

    fun record(message: String) {
        count++
        if (samples.size < limit) {
            samples.add(message)
        }
    }

    fun assertNone(label: String) {
        if (count > 0) {
            fail("$label: $count mismatches\n" + samples.joinToString("\n"))
        }
    }
}
