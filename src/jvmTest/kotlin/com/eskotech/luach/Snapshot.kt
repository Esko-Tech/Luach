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

import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.test.fail
import kotlin.time.Instant

class Fingerprint(var count: Long = 0, var exact: Long = 0, var coarse: Long = 0) {
    fun add(exactHash: Long, coarseHash: Long) {
        count++
        exact += exactHash
        coarse += coarseHash
    }

    fun add(other: Fingerprint) {
        count += other.count
        exact += other.exact
        coarse += other.coarse
    }
}

class SnapshotRecorder {
    internal val members = HashMap<String, Fingerprint>()
    internal val groups = HashMap<String, Fingerprint>()
    internal val total = Fingerprint()

    val tallies = HashMap<String, Long>()

    var group: String = DEFAULT_GROUP

    private var sampleHash = 0L
    private val positions = HashMap<String, Int>()

    fun startSample(key: String) {
        sampleHash = hashString(key)
        positions.clear()
        group = DEFAULT_GROUP
    }

    fun record(member: String, value: Any?) {
        val normalized = normalize(value)
        val position = positions.merge(member, 1, Int::plus)!!
        val key = mix(mix(sampleHash, member.hashCode().toLong()), position.toLong())
        val exact = mix(key, hashValue(normalized, coarse = false))
        val coarse = mix(key, hashValue(normalized, coarse = true))
        members.getOrPut(member) { Fingerprint() }.add(exact, coarse)
        groups.getOrPut(group) { Fingerprint() }.add(exact, coarse)
        total.add(exact, coarse)
        if (normalized is Threw) tally("$member threw ${normalized.exceptionClass}")
    }

    inline fun recordOutcome(member: String, block: () -> Any?) = record(member, outcome(block))

    fun tally(key: String) {
        tallies[key] = (tallies[key] ?: 0L) + 1
    }

    internal fun merge(other: SnapshotRecorder) {
        for ((key, fingerprint) in other.members) members.getOrPut(key) { Fingerprint() }.add(fingerprint)
        for ((key, fingerprint) in other.groups) groups.getOrPut(key) { Fingerprint() }.add(fingerprint)
        total.add(other.total)
        for ((key, count) in other.tallies) tallies[key] = (tallies[key] ?: 0L) + count
    }

    private companion object {
        const val DEFAULT_GROUP = "all"
    }
}

class Snapshot private constructor(val name: String, private val recorder: SnapshotRecorder) {
    val tallies: Map<String, Long> get() = recorder.tallies

    fun assertMatches() {
        val current = Table(environment(), entries())
        println("[snapshot] $name: ${recorder.total.count} values, ${recorder.members.size} members, ${recorder.groups.size} groups")
        for ((key, count) in tallies.toSortedMap()) println("[snapshot]   $key: $count")
        if (recorder.total.count == 0L) fail("[$name] no values were recorded")

        val updateDirectory = System.getProperty(UPDATE_PROPERTY)
        if (updateDirectory != null) {
            File(updateDirectory, "$name.txt").apply { parentFile.mkdirs() }.writeText(current.render(name))
            println("[snapshot]   written to $updateDirectory")
            return
        }
        val text = Snapshot::class.java.getResourceAsStream("/snapshots/$name.txt")?.use { it.reader().readText() }
            ?: fail("[$name] there is no snapshot file snapshots/$name.txt. $REGENERATE")
        val stored = Table.parse(text)

        val keys = (stored.entries.keys + current.entries.keys).toSortedSet()
        val missing = keys.filter { it !in current.entries }
        val added = keys.filter { it !in stored.entries }
        val both = keys.filter { it in current.entries && it in stored.entries }
        fun changed(key: String, coarse: Boolean): Boolean {
            val was = stored.entries.getValue(key)
            val now = current.entries.getValue(key)
            return was.count != now.count || (if (coarse) was.coarse != now.coarse else was.exact != now.exact)
        }
        val exactChanged = both.filter { changed(it, coarse = false) }
        val coarseChanged = both.filter { changed(it, coarse = true) }
        val sameKeys = missing.isEmpty() && added.isEmpty()
        val strict = stored.environment == current.environment
        when {
            sameKeys && exactChanged.isEmpty() -> println("[snapshot]   matches")
            sameKeys && coarseChanged.isEmpty() && !strict -> println(
                "[snapshot]   matches to the second (doubles to 6 significant digits); the snapshot was generated on " +
                    "${stored.environment}, this JVM is ${current.environment}",
            )
            else -> fail(buildString {
                append("[$name] the results differ from the snapshot ")
                append("(${current.entries.getValue(TOTAL).count} values now, ${stored.entries[TOTAL]?.count} in the snapshot).\n")
                fun list(label: String, keys: List<String>) {
                    if (keys.isEmpty()) return
                    append("$label (${keys.size}): ")
                    append(keys.take(MAX_REPORTED).joinToString(", ") { it.substringAfter('\t') })
                    if (keys.size > MAX_REPORTED) append(", ...")
                    append('\n')
                }
                val changed = if (strict) exactChanged else coarseChanged
                list("Changed members", changed.filter { it.startsWith(MEMBER) })
                list("Changed groups", changed.filter { it.startsWith(GROUP) })
                list("New members", added.filter { it.startsWith(MEMBER) })
                list("New groups", added.filter { it.startsWith(GROUP) })
                list("Members no longer recorded", missing.filter { it.startsWith(MEMBER) })
                list("Groups no longer recorded", missing.filter { it.startsWith(GROUP) })
                if (sameKeys && coarseChanged.isEmpty()) {
                    append("Every value still matches to the second (doubles to 6 significant digits), so the differences ")
                    append("are smaller than that. A different JDK build can also cause this (the snapshot was generated on ")
                    append("${stored.environment}).\n")
                }
                append(REGENERATE)
            })
        }
    }

    private fun entries(): Map<String, Fingerprint> = buildMap {
        put(TOTAL, recorder.total)
        for ((key, fingerprint) in recorder.members) put("$MEMBER\t$key", fingerprint)
        for ((key, fingerprint) in recorder.groups) put("$GROUP\t$key", fingerprint)
    }

    private class Table(val environment: String, val entries: Map<String, Fingerprint>) {
        fun render(name: String): String = buildString {
            append("# Snapshot \"$name\" of the Luach JVM tests (see Snapshot.kt).\n")
            append("# kind, key, number of values, exact fingerprint, coarse fingerprint. ")
            append("Regenerate with ./gradlew jvmTest -PupdateSnapshots\n")
            append("environment\t$environment\n")
            val order = compareBy<String> { if (it == TOTAL) 0 else if (it.startsWith(MEMBER)) 1 else 2 }.thenBy { it }
            for ((key, fingerprint) in entries.toSortedMap(order)) {
                append("$key\t${fingerprint.count}\t${hex(fingerprint.exact)}\t${hex(fingerprint.coarse)}\n")
            }
        }

        companion object {
            fun parse(text: String): Table {
                var environment = ""
                val entries = LinkedHashMap<String, Fingerprint>()
                for (line in text.lineSequence()) {
                    if (line.isBlank() || line.startsWith("#")) continue
                    val fields = line.split('\t')
                    if (fields[0] == "environment") {
                        environment = fields[1]
                        continue
                    }
                    val key = fields.dropLast(3).joinToString("\t")
                    val (count, exact, coarse) = fields.takeLast(3)
                    entries[key] = Fingerprint(count.toLong(), exact.toULong(16).toLong(), coarse.toULong(16).toLong())
                }
                return Table(environment, entries)
            }

            private fun hex(value: Long): String = value.toULong().toString(16).padStart(16, '0')
        }
    }

    companion object {
        const val UPDATE_PROPERTY = "snapshots.update"

        private const val TOTAL = "total"
        private const val MEMBER = "member"
        private const val GROUP = "group"
        private const val MAX_REPORTED = 40
        private const val REGENERATE =
            "If the change is intended, regenerate the snapshots with ./gradlew jvmTest -PupdateSnapshots and review the diff."

        fun <T> collect(
            name: String,
            items: List<T>,
            key: (T) -> String = { it.toString() },
            action: (SnapshotRecorder, T) -> Unit,
        ): Snapshot {
            val result = SnapshotRecorder()
            val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)
            val chunks = items.chunked(maxOf(1, (items.size + threads * 8 - 1) / (threads * 8)))
            val pool = Executors.newFixedThreadPool(threads)
            try {
                val futures = chunks.map { chunk ->
                    pool.submit(Callable {
                        SnapshotRecorder().also { recorder ->
                            for (item in chunk) {
                                recorder.startSample(key(item))
                                action(recorder, item)
                            }
                        }
                    })
                }
                for (future in futures) {
                    try {
                        result.merge(future.get())
                    } catch (e: ExecutionException) {
                        throw e.cause ?: e
                    }
                }
            } finally {
                pool.shutdownNow()
            }
            return Snapshot(name, result)
        }

        fun single(name: String, action: (SnapshotRecorder) -> Unit): Snapshot =
            collect(name, listOf(name)) { recorder, _ -> action(recorder) }

        private fun environment(): String =
            "${System.getProperty("os.arch")} / Java ${System.getProperty("java.specification.version")} / " +
                System.getProperty("java.vm.name")
    }
}

private val NULL_HASH = 0x5bd1e9955bd1e995uL.toLong()

private fun hashString(s: String): Long {
    var hash = 0xcbf29ce484222325uL.toLong()
    for (c in s) hash = (hash xor c.code.toLong()) * 0x100000001b3L
    return hash
}

private fun mix(a: Long, b: Long): Long {
    var x = a * 0x9e3779b97f4a7c15uL.toLong() + b
    x = (x xor (x ushr 33)) * 0xff51afd7ed558ccduL.toLong()
    x = (x xor (x ushr 33)) * 0xc4ceb9fe1a85ec53uL.toLong()
    return x xor (x ushr 33)
}

private fun bits(value: Double): Long = if (value.isNaN()) 0x7ff8000000000000L else value.toBits()

private fun roundedHash(value: Double): Long {
    if (value == 0.0 || value.isNaN() || value.isInfinite()) return bits(value)
    val exponent = floor(log10(abs(value))).toInt()
    return mix((value * 10.0.pow(5 - exponent)).roundToLong(), exponent.toLong())
}

private fun hashValue(value: Any?, coarse: Boolean): Long = when (value) {
    null -> NULL_HASH
    is Instant -> mix(1, if (coarse) Math.floorDiv(value.toEpochMilliseconds() + 500, 1000L) else value.toEpochMilliseconds())
    is Double -> mix(2, if (coarse) roundedHash(value) else bits(value))
    is Float -> mix(2, if (coarse) roundedHash(value.toDouble()) else bits(value.toDouble()))
    is Long -> mix(3, value)
    is Int -> mix(4, value.toLong())
    is Short -> mix(4, value.toLong())
    is Byte -> mix(4, value.toLong())
    is Boolean -> mix(5, if (value) 1L else 0L)
    is Char -> mix(6, value.code.toLong())
    is String -> mix(7, hashString(value))
    is Threw -> mix(8, hashString(value.exceptionClass))
    is List<*> -> value.fold(mix(9, value.size.toLong())) { hash, element -> mix(hash, hashValue(element, coarse)) }
    is Map<*, *> -> value.entries.fold(mix(10, value.size.toLong())) { hash, (k, v) ->
        mix(mix(hash, hashValue(k, coarse)), hashValue(v, coarse))
    }
    else -> error("cannot record a ${value::class.qualifiedName}; add it to normalize()")
}
