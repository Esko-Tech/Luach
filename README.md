# Luach

Luach is a Kotlin Multiplatform library for the Hebrew calendar and for the times of the halachic day
(zmanim). It is written in common Kotlin, so the same library runs on the JVM and
Android, iOS, macOS, Linux and Windows.

## Quick start

```kotlin
import com.eskotech.luach.format.LuachFormatter
import com.eskotech.luach.geo.Location
import com.eskotech.luach.zmanim.Zmanim
import kotlinx.datetime.TimeZone

val lakewood = Location(40.0959, -74.2177, TimeZone.of("America/New_York"), elevation = 15.0, name = "Lakewood, NJ")
val zmanim = Zmanim.today(lakewood)
val day = zmanim.hebrewDay
val format = LuachFormatter()

format.formatDate(day.date)              // 16 Cheshvan, 5787
format.formatHoliday(day)                // "" on an ordinary day, "Chanukah 3" on 27 Kislev
format.formatParsha(day.upcomingParsha)  // the parsha of the coming Shabbos
for ((zman, time) in zmanim.all()) {
    println("${format.formatZman(zman)}: ${format.formatTime(time, lakewood)}")
}
```

`all()` computes the standard list with the GRA's day, alos at 16.1° and tzais at 8.5°; pass `Shitos.MAGEN_AVRAHAM`
or your own `Shitos` for other opinions. A `Location` created with `inIsrael = true` makes every day computed for it
an Israeli day. Every time is a `kotlin.time.Instant?`, null on a date without that event.

## Targets

| Platform | Kotlin target                              |
|----------|--------------------------------------------|
| JVM, Android | `jvm`                                  |
| iOS      | `iosArm64`, `iosSimulatorArm64`, `iosX64`  |
| macOS    | `macosArm64`, `macosX64`                   |
| Linux    | `linuxX64`, `linuxArm64`                   |
| Windows  | `mingwX64`                                 |

The only dependency is [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime).

## What is included

| Package | Types |
|---------|-------|
| `com.eskotech.luach.calendar` | `HebrewDate`, `HebrewMonth`, `YearType`, `Molad`, `HebrewDay`, `Holiday`, `Parsha`, `DafYomi`, `BavliTractate`, `YerushalmiTractate`, `TefillahRules`, `TachanunCustoms`, `Hallel` |
| `com.eskotech.luach.zmanim` | `Zmanim`, `Zman`, `Shitos`, `DayEdge`, `HalachicDay`, `KiddushLevanaStart`, `KiddushLevanaEnd` |
| `com.eskotech.luach.solar` | `SolarDay`, `SolarModel`, `NoaaSolarModel`, `AlmanacSolarModel`, `ApparentHorizon` |
| `com.eskotech.luach.geo` | `Location`, `Geodesic`, `RhumbLine` |
| `com.eskotech.luach.format` | `LuachFormatter`, `Transliterations` |

## Installation

The library is published as `com.eskotech:luach`. In a Kotlin Multiplatform module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.eskotech:luach:0.1.0")
        }
    }
}
```

In a JVM or Android module:

```kotlin
dependencies {
    implementation("com.eskotech:luach:0.1.0")
}
```

Gradle picks the artifact for each target (`luach-jvm`, `luach-iosarm64`, ...) from the module metadata, so
the single coordinate above is all that is needed.

## Usage

### The Hebrew calendar

```kotlin
import com.eskotech.luach.calendar.HebrewDate
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.HebrewMonth
import com.eskotech.luach.format.LuachFormatter
import kotlinx.datetime.LocalDate

val date = HebrewDate.from(LocalDate(2026, 10, 13))      // 2 Cheshvan 5787
val pesach = HebrewDate.of(5787, HebrewMonth.NISAN, 15)  // LocalDate(2027, 4, 22)
val nextMonth = date.plusMonths(1)

val day = HebrewDay(date, inIsrael = false)
day.holiday          // Holiday? for the day, null on an ordinary day
day.parsha           // Parsha? on Shabbos, null on a weekday
day.isRoshChodesh
day.sefirasHaomer        // Int? during the omer
day.bavliDaf         // BavliDaf(tractate, page)

val formatter = LuachFormatter(hebrew = true)
formatter.formatDate(date)      // ב׳ חשון תשפ״ז
formatter.formatParsha(day)
```

`HebrewDate` is an immutable value: `plusDays`, `plusMonths`, `plusYears` and `with…` return new dates.
`HebrewDay` adds the meaning of the day (holidays, parsha, omer, Daf Yomi) for a choice of settings:
`inIsrael` and `mukefChoma`. `HebrewDay.today(timeZone)` is the current day.

### Zmanim

```kotlin
import com.eskotech.luach.geo.Location
import com.eskotech.luach.zmanim.DayEdge
import com.eskotech.luach.zmanim.HalachicDay
import com.eskotech.luach.zmanim.Shitos
import com.eskotech.luach.zmanim.Zmanim
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

val lakewood = Location(40.0959, -74.2177, TimeZone.of("America/New_York"), elevation = 15.0, name = "Lakewood, NJ")
val zmanim = Zmanim(lakewood, LocalDate(2026, 10, 13))

zmanim.hanetz                                      // kotlin.time.Instant?
zmanim.alos(DayEdge.Depression(16.1))              // alos at 16.1° below the horizon
zmanim.alos(DayEdge.Minutes(72.0))                 // 72 fixed minutes before sunrise
zmanim.tzais(DayEdge.ProportionalMinutes(72.0))    // 72 proportional minutes after sunset
zmanim.sofZmanShema()                              // 3 temporal hours into the sunrise-to-sunset day
zmanim.sofZmanShema(HalachicDay.MAGEN_AVRAHAM_72)  // the same for a 72-minute day
zmanim.plagHamincha(HalachicDay.degrees(16.1, 7.083))
zmanim.shaahZmanis(HalachicDay.MAGEN_AVRAHAM_16_1) // kotlin.time.Duration?
zmanim.hadlakasNeiros
zmanim.kiddushLevanaStart(KiddushLevanaStart.THREE_DAYS)
zmanim.all()                                       // Map<Zman, Instant?>, the whole list for the GRA's day
zmanim.all(Shitos.MAGEN_AVRAHAM)                   // the same list with 72 minutes for alos, the day and tzais
Zmanim.today(lakewood)                             // today in the location's zone
```

A day is described by its two edges. `DayEdge` is a depression angle, a number of fixed or proportional
minutes before sunrise or after sunset, a shift of another edge, or a fraction of the morning interval
between a dawn and sunrise. `HalachicDay` pairs a dawn edge with a dusk edge, and every zman that is a
fraction of the day (`sofZmanShema`, `sofZmanTefillah`, `minchaGedola`, `minchaKetana`, `plagHamincha`,
`hourOfDay(hours, day)`) takes one. The presets are named after the opinions: `HalachicDay.GRA`,
`HalachicDay.MAGEN_AVRAHAM_72`, `HalachicDay.MAGEN_AVRAHAM_16_1` and others, and `DayEdge` carries the usual
alos, misheyakir and tzais edges as constants such as `DayEdge.ALOS_72` and `DayEdge.TZAIS_GEONIM_8_5`.

A zman missing on a given day (sunrise above the Arctic Circle in winter, a 26° dawn in
summer at high latitude) is `null`, and so is everything built on it. Durations are `kotlin.time.Duration?`.

## Sample

`sample/` is a small command-line program that prints today's luach for a location: the date in both scripts,
holiday, parsha, daf, the tefillah summary and the standard zmanim list.

```
./gradlew :sample:run                                                   # Lakewood, NJ
./gradlew :sample:run --args="31.7784 35.2066 Asia/Jerusalem 754 israel mukefchoma"
```

## Documentation

The `docs/` directory describes every part of the library; [docs/README.md](docs/README.md) is the index.
The per-area pages (`geo`, `solar`, `zmanim`, `calendar`, `formatting`, `daf_yomi`, `tefillah`, `testing`,
`publishing`) are hand-written and cite the source files; `docs/build-release.md`, the `docs/reference/`
inventories and the tables marked as generated inside the pages are produced by `python3 docs/generate.py`
and refreshed by `.github/workflows/docs-regenerate.yml` on every push to the default branch.

## Building and testing

```
./gradlew jvmTest          # unit tests + snapshot tests
./gradlew linuxX64Test     # the common unit tests on Kotlin/Native
./gradlew allTests         # every target the host can run
```

The tests in `src/commonTest` check every feature against expected values independent of the library:
every day of 5770 to 5790 is converted both ways from a table of Rosh Hashanah dates, holidays,
tefillah rules and special Shabbosos are derived from the halachic rules, Daf Yomi is derived from the cycle
start dates and tractate lengths, the parsha schedule is checked for order, completeness and its fixed
anchors over 200 years, every zman is compared with its definition, and the solar models are compared with
reference times produced by an independent implementation. They run on every target.

The snapshot tests in `src/jvmTest` recompute about 57 million values and compare fingerprints of them
with the files in `src/jvmTest/resources/snapshots`. A failure names the members and the locations or date
ranges whose results changed. After an intended change, regenerate the snapshots with
`./gradlew jvmTest -PupdateSnapshots` and review the diff.

## License

Luach is free software under the [GNU General Public License, version 3](LICENSE), with one
[additional permission](ADDITIONAL-PERMISSIONS.md): a covered work may be distributed through the Apple App
Store and Apple's other application distribution services despite their terms of use.

Contributions are welcome under the [Contributor License Agreement](CLA.md); see
[CONTRIBUTING.md](CONTRIBUTING.md).
