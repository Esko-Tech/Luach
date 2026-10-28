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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

internal val MILLISECOND_TOLERANCE: Duration = 2.milliseconds

internal val TEMPORAL_HOUR_TOLERANCE: Duration = 15.milliseconds

internal fun instantMismatch(label: String, expectedIso: String, actual: Instant?, tolerance: Duration): String? {
    val expected = Instant.parse(expectedIso)
    if (actual == null) {
        return "$label: expected <$expectedIso> but was <null>"
    }
    val difference = (actual - expected).absoluteValue
    return if (difference <= tolerance) null else "$label: expected <$expectedIso> (±$tolerance) but was <$actual>, $difference off"
}

internal fun durationMismatch(label: String, expectedIso: String, actual: Duration?, tolerance: Duration): String? {
    val expected = Duration.parseIsoString(expectedIso)
    if (actual == null) {
        return "$label: expected <$expectedIso> but was <null>"
    }
    val difference = (actual - expected).absoluteValue
    return if (difference <= tolerance) null else "$label: expected <$expectedIso> (±$tolerance) but was <$actual>, $difference off"
}

internal fun assertInstant(label: String, expectedIso: String, actual: Instant?, tolerance: Duration = MILLISECOND_TOLERANCE) {
    instantMismatch(label, expectedIso, actual, tolerance)?.let { fail(it) }
}

internal fun assertDuration(label: String, expectedIso: String, actual: Duration?, tolerance: Duration = MILLISECOND_TOLERANCE) {
    durationMismatch(label, expectedIso, actual, tolerance)?.let { fail(it) }
}
