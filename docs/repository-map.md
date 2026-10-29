# Repository map

## Project identity

- The Gradle root project is `luach`: one Kotlin Multiplatform module, published as `com.eskotech:luach` (group `com.eskotech`, artifact `luach`; the version is set once in `build.gradle.kts` and mirrored by the constant `Luach.VERSION`; `VersionTest` compares the two on every `jvmTest` run).
- Every type lives under `com.eskotech.luach`, in the subpackages `calendar` (the Hebrew calendar, Daf Yomi, tefillah rules), `zmanim` (the halachic day), `solar` (sunrise, sunset and the solar position models), `geo` (locations and distances) and `format` (Hebrew and transliterated text).
- The only library dependency is `kotlinx-datetime`, exposed as `api` because `LocalDate`, `DayOfWeek` and `TimeZone` are part of the public API (`Instant` and `Duration` are `kotlin.time` types from the standard library). Tests use `kotlin-test`.
- Targets: `jvm` (JVM 17 bytecode, JDK 21 toolchain), `iosArm64`, `iosSimulatorArm64`, `iosX64`, `macosArm64`, `linuxX64`, `linuxArm64`, `mingwX64`. `kotlin.native.ignoreDisabledTargets=true` lets a Linux or Windows host configure the Apple targets; a macOS host builds them.
- `src/commonMain` is the whole library, in common Kotlin. The exact Gradle facts are in the generated [`build-release.md`](build-release.md).

## Source sets

| Source set | Purpose |
| --- | --- |
| `src/commonMain` | The library: common Kotlin only. |
| `src/commonTest` | Unit tests and the oracle (correctness) tests. They run on every target (`jvmTest`, `linuxX64Test`, `macosArm64Test`, ...). |
| `src/jvmTest` | The snapshot tests, which need JDK reflection, thread pools and file I/O, and their stored fingerprints in `src/jvmTest/resources/snapshots`. |

## Packages and files

| Package | Files | Responsibility |
| --- | --- | --- |
| `com.eskotech.luach` | `Luach.kt` | The version constant. |
| `com.eskotech.luach.geo` | `Location.kt` | A validated latitude, longitude, elevation and time zone; coordinates from degrees-minutes-seconds; `Geodesic` and `RhumbLine` between two locations. |
| `com.eskotech.luach.solar` | `SolarModel.kt`, `NoaaSolarModel.kt`, `AlmanacSolarModel.kt`, `SolarDay.kt` | The solar position models (UTC hours of sunrise, sunset and solar noon for a zenith angle), the apparent-horizon correction, and `SolarDay`, which places those hours on the requested local day as `Instant`s. |
| `com.eskotech.luach.zmanim` | `DayEdge.kt`, `Zman.kt`, `Zmanim.kt` | The description of a halachic day (`DayEdge`, `HalachicDay`), the standard list and the opinions it is computed with (`Zman`, `Shitos`), and `Zmanim`, which turns them into times: dawn and dusk by any rule, the fractions of the day, chatzos, candle lighting, the molad-based zmanim, melacha. |
| `com.eskotech.luach.calendar` | `HebrewMonth.kt`, `HebrewDate.kt`, `Molad.kt`, `Holiday.kt`, `Parsha.kt`, `HebrewDay.kt`, `DafYomi.kt`, `Tefillah.kt` | Hebrew date arithmetic and conversions; the molad; holidays, the parsha schedule, special Shabbosos, omer and tekufa for a day under its settings; Daf Yomi Bavli and Yerushalmi; which prayers are said on a day. |
| `com.eskotech.luach.format` | `LuachFormatter.kt` | Hebrew and transliterated names of dates, months, weekdays, holidays, parshios and dafim; Hebrew numerals; the year signature. |

## Types

```
calendar: HebrewDate (immutable date) ─ HebrewMonth, YearType, Molad
          HebrewDay (date + settings) ─ Holiday, Parsha, BavliDaf, YerushalmiDaf
          DafYomi (object) ─ BavliTractate, YerushalmiTractate
          TefillahRules ─ TachanunCustoms, Hallel
zmanim:   Zmanim ─ DayEdge (sealed), HalachicDay, Zman, Shitos, KiddushLevanaStart, KiddushLevanaEnd
solar:    SolarDay ─ SolarModel (interface) ← NoaaSolarModel, AlmanacSolarModel; ApparentHorizon
geo:      Location ─ Geodesic, RhumbLine
format:   LuachFormatter ─ Transliterations
```

`Zmanim` is the type most applications use for times and `HebrewDay` for the calendar; `HebrewDate` alone is enough for conversions. Every type is a value: `HebrewDate` and `Molad` are immutable, and `Location`, `SolarDay`, `Zmanim`, `HebrewDay`, `LuachFormatter`, `ApparentHorizon` and `TachanunCustoms` are data classes whose `copy` is the way to change a setting.

## Other top-level files

| Path | Role |
| --- | --- |
| `README.md` | User-facing introduction: targets, installation, usage, documentation, testing. |
| `AGENTS.md` | The working rules for AI agents and contributors: project rules, the danger zones of every package, the test layers, the docs and the release flow. |
| `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` | The build. Facts are generated into [`build-release.md`](build-release.md). |
| `.github/workflows/ci.yml` | Tests on every push and pull request on Linux x64, macOS (plus the iOS simulator) and Windows, and compiles every target on macOS. |
| `.github/workflows/release.yml` | On a `v*` tag: runs every test the macOS host can run, then publishes to Maven Central. |
| `.github/workflows/docs-regenerate.yml` | On every push to the default branch: runs `docs/generate.py` and commits any change. |
| `LICENSE`, `ADDITIONAL-PERMISSIONS.md` | The GNU General Public License version 3, and the license notice with the App Store additional permission. |
| `CLA.md`, `CONTRIBUTING.md` | The Contributor License Agreement that every pull request accepts, and the contributor guide. |
| `.github/pull_request_template.md` | The pull request template, ending with the sentence that accepts the CLA. |
| `docs/` | This documentation. |
| `sample/` | A command-line program that prints today's luach for a location: the date, holiday, parsha, daf, the tefillah summary and the standard zmanim list. `./gradlew :sample:run`. |
| `api/luach.api` | The public JVM API dump checked by `./gradlew apiCheck`; refresh with `./gradlew apiDump` after an intended API change. |

## File inventory

The inventory below is generated from the files Git knows about (tracked files and untracked files outside the ignore rules) and documents every file that defines source, configuration, tests, documentation or build tooling.

<!-- generated:inventory - written by docs/generate.py; to change it, edit the generator -->

Files counted: `117`

By extension:

- `.kt`: `44`
- `.txt`: `35`
- `.md`: `21`
- `.kts`: `3`
- `.properties`: `3`
- `.yml`: `3`
- `[none]`: `3`
- `.api`: `1`
- `.bat`: `1`
- `.jar`: `1`
- `.py`: `1`
- `.toml`: `1`

#### Every counted file

| Path | Size | Kind |
| --- | ---: | --- |
| `.github/pull_request_template.md` | 10 lines | `.md` |
| `.github/workflows/ci.yml` | 99 lines | `.yml` |
| `.github/workflows/docs-regenerate.yml` | 65 lines | `.yml` |
| `.github/workflows/release.yml` | 59 lines | `.yml` |
| `.gitignore` | 99 lines | `[none]` |
| `ADDITIONAL-PERMISSIONS.md` | 25 lines | `.md` |
| `AGENTS.md` | 142 lines | `.md` |
| `CLA.md` | 74 lines | `.md` |
| `CONTRIBUTING.md` | 38 lines | `.md` |
| `LICENSE` | 674 lines | `[none]` |
| `README.md` | 195 lines | `.md` |
| `api/luach.api` | 1074 lines | `.api` |
| `build.gradle.kts` | 107 lines | `.kts` |
| `docs/README.md` | 29 lines | `.md` |
| `docs/build-release.md` | 157 lines | `.md` |
| `docs/calendar/README.md` | 244 lines | `.md` |
| `docs/daf_yomi/README.md` | 113 lines | `.md` |
| `docs/formatting/README.md` | 66 lines | `.md` |
| `docs/generate.py` | 1135 lines | `.py` |
| `docs/geo/README.md` | 22 lines | `.md` |
| `docs/publishing/README.md` | 49 lines | `.md` |
| `docs/reference/kotlin-files.md` | 63 lines | `.md` |
| `docs/reference/non-kotlin-files.md` | 83 lines | `.md` |
| `docs/reference/public-api.md` | 677 lines | `.md` |
| `docs/repository-map.md` | 207 lines | `.md` |
| `docs/solar/README.md` | 86 lines | `.md` |
| `docs/tefillah/README.md` | 68 lines | `.md` |
| `docs/testing/README.md` | 145 lines | `.md` |
| `docs/zmanim/README.md` | 160 lines | `.md` |
| `gradle.properties` | 4 lines | `.properties` |
| `gradle/gradle-daemon-jvm.properties` | 11 lines | `.properties` |
| `gradle/libs.versions.toml` | 14 lines | `.toml` |
| `gradle/wrapper/gradle-wrapper.jar` | 45457 bytes | `.jar` |
| `gradle/wrapper/gradle-wrapper.properties` | 6 lines | `.properties` |
| `gradlew` | 251 lines | `[none]` |
| `gradlew.bat` | 94 lines | `.bat` |
| `sample/build.gradle.kts` | 37 lines | `.kts` |
| `sample/src/main/kotlin/com/eskotech/luach/sample/Main.kt` | 89 lines | `.kt` |
| `settings.gradle.kts` | 42 lines | `.kts` |
| `src/commonMain/kotlin/com/eskotech/luach/Luach.kt` | 26 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/DafYomi.kt` | 193 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/HebrewDate.kt` | 245 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/HebrewDay.kt` | 331 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/HebrewMonth.kt` | 55 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/Holiday.kt` | 59 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/Molad.kt` | 105 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/Parsha.kt` | 207 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/calendar/Tefillah.kt` | 200 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/format/LuachFormatter.kt` | 392 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/geo/Location.kt` | 167 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/solar/AlmanacSolarModel.kt` | 101 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/solar/NoaaSolarModel.kt` | 173 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/solar/SolarDay.kt` | 163 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/solar/SolarModel.kt` | 60 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/zmanim/DayEdge.kt` | 143 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/zmanim/Zman.kt` | 54 lines | `.kt` |
| `src/commonMain/kotlin/com/eskotech/luach/zmanim/Zmanim.kt` | 223 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/Assertions.kt` | 57 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/LuachSmokeTest.kt` | 32 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/Mismatches.kt` | 50 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/Places.kt` | 57 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/DafYomiTest.kt` | 222 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/HebrewDateTest.kt` | 311 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/HolidayTest.kt` | 318 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/ParshaTest.kt` | 252 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/ReferenceCalendar.kt` | 108 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/TefillahRulesTest.kt` | 282 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/format/LuachFormatterTest.kt` | 301 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/geo/LocationTest.kt` | 165 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/solar/SolarDayTest.kt` | 232 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/solar/SolarModelTest.kt` | 525 lines | `.kt` |
| `src/commonTest/kotlin/com/eskotech/luach/zmanim/ZmanimTest.kt` | 481 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/CalendarSnapshotTest.kt` | 188 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/DafYomiSnapshotTest.kt` | 91 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/FormatterSnapshotTest.kt` | 186 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/LocationSnapshotTest.kt` | 80 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/Snapshot.kt` | 297 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/SnapshotFixtures.kt` | 272 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/SolarSnapshotTest.kt` | 174 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/TefillahSnapshotTest.kt` | 89 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/VersionTest.kt` | 31 lines | `.kt` |
| `src/jvmTest/kotlin/com/eskotech/luach/ZmanimSnapshotTest.kt` | 197 lines | `.kt` |
| `src/jvmTest/resources/snapshots/Calendar.arithmetic.txt` | 33 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Calendar.everyDay1850To2150.txt` | 87 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Calendar.factoriesAndStatics.txt` | 22 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Calendar.instantConstructors.txt` | 12 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Calendar.mukefChomaEveryDay1990To2060.txt` | 40 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Calendar.parshaSchedules.txt` | 11 lines | `.txt` |
| `src/jvmTest/resources/snapshots/DafYomi.bavliEveryDay1923To2100.txt` | 25 lines | `.txt` |
| `src/jvmTest/resources/snapshots/DafYomi.tractates.txt` | 8 lines | `.txt` |
| `src/jvmTest/resources/snapshots/DafYomi.yerushalmiEveryDay1980To2100.txt` | 19 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Formatter.dafFormats.txt` | 12 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Formatter.defaults.txt` | 28 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Formatter.everyDay1900To2100.txt` | 30 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Formatter.numbers.txt` | 13 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Formatter.times.txt` | 13 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Formatter.userSuppliedNames.txt` | 14 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Formatter.yearSignatures.txt` | 6 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Location.constructionAndValidation.txt` | 12 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Location.distancesAndBearings.txt` | 36 lines | `.txt` |
| `src/jvmTest/resources/snapshots/SolarDay.depressionSearch.txt` | 14 lines | `.txt` |
| `src/jvmTest/resources/snapshots/SolarDay.eventsAtDepressions.txt` | 46 lines | `.txt` |
| `src/jvmTest/resources/snapshots/SolarDay.temporalHoursWithArbitraryBounds.txt` | 45 lines | `.txt` |
| `src/jvmTest/resources/snapshots/SolarDay.zeroArgGetters.txt` | 58 lines | `.txt` |
| `src/jvmTest/resources/snapshots/SolarModel.elevationAndAzimuth.txt` | 7 lines | `.txt` |
| `src/jvmTest/resources/snapshots/SolarModel.horizonVariants.txt` | 19 lines | `.txt` |
| `src/jvmTest/resources/snapshots/SolarModel.utcHours.txt` | 47 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Tefillah.allRulesEveryDay1990To2060.txt` | 31 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Tefillah.defaultCustoms.txt` | 17 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.catalogue.txt` | 11 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.edgesAndDays.txt` | 404 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.hadlakasNeirosOffsets.txt` | 55 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.hoursOfTheDay.txt` | 306 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.isAsurBemelacha.txt` | 18 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.kiddushLevana.txt` | 49 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.standardList.txt` | 75 lines | `.txt` |
| `src/jvmTest/resources/snapshots/Zmanim.zeroArgGetters.txt` | 94 lines | `.txt` |

<!-- /generated:inventory -->
