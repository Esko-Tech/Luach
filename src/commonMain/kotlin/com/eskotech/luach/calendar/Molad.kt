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

package com.eskotech.luach.calendar

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class Molad internal constructor(internal val partsFromWeekStart: Long) : Comparable<Molad> {

    val dayOfWeek: DayOfWeek
        get() = DayOfWeek.entries[((partsFromWeekStart / PARTS_PER_DAY + 6) % 7).toInt()]

    val hour: Int
        get() = ((partsFromWeekStart % PARTS_PER_DAY) / PARTS_PER_HOUR).toInt()

    val minute: Int
        get() = ((partsFromWeekStart % PARTS_PER_HOUR) / PARTS_PER_MINUTE).toInt()

    val parts: Int
        get() = (partsFromWeekStart % PARTS_PER_MINUTE).toInt()

    val partsOfDay: Int
        get() = (partsFromWeekStart % PARTS_PER_DAY).toInt()

    val clockHour: Int
        get() = (hour + 18) % 24

    val date: HebrewDate
        get() = HebrewDate.ofEpochDay(clockEpochDay)

    val instant: Instant
        get() {
            val clockDate = LocalDate.fromEpochDays(clockEpochDay)
            val partsAsMillis = parts * 10_000L / 3
            val local = LocalDateTime(
                clockDate.year, clockDate.month, clockDate.day,
                clockHour, minute, (partsAsMillis / 1000).toInt(), (partsAsMillis % 1000).toInt() * 1_000_000,
            )
            return local.toInstant(TimeZone.UTC) - (TEMPLE_MOUNT_LONGITUDE * 4 * 60_000).toLong().milliseconds
        }

    val earliestKiddushLevana3Days: Instant
        get() = instant + 72.hours

    val earliestKiddushLevana7Days: Instant
        get() = instant + 168.hours

    val latestKiddushLevanaHalfMonth: Instant
        get() = instant + HALF_LUNATION

    val latestKiddushLevana15Days: Instant
        get() = instant + 360.hours

    private val clockEpochDay: Long
        get() = HebrewDate.WEEK_ZERO_EPOCH_DAY + partsFromWeekStart / PARTS_PER_DAY + (if (hour >= 6) 1 else 0)

    override fun compareTo(other: Molad): Int = partsFromWeekStart.compareTo(other.partsFromWeekStart)

    override fun equals(other: Any?): Boolean = other is Molad && other.partsFromWeekStart == partsFromWeekStart

    override fun hashCode(): Int = partsFromWeekStart.hashCode()

    override fun toString(): String = "Molad(${dayOfWeek.name.lowercase()} ${hour}h ${minute}m ${parts}p)"

    companion object {
        const val PARTS_PER_MINUTE: Long = 18

        const val PARTS_PER_HOUR: Long = 60 * PARTS_PER_MINUTE

        const val PARTS_PER_DAY: Long = 24 * PARTS_PER_HOUR

        const val LUNATION_PARTS: Long = 29 * PARTS_PER_DAY + 12 * PARTS_PER_HOUR + 793

        internal const val FIRST_MOLAD_PARTS: Long = 1 * PARTS_PER_DAY + 5 * PARTS_PER_HOUR + 204

        private const val TEMPLE_MOUNT_LONGITUDE: Double = 35.2354

        private val HALF_LUNATION = (LUNATION_PARTS * 10_000 / 3 / 2).milliseconds
    }
}
