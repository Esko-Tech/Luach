# Testing

Two layers, both run by `./gradlew jvmTest`; the first also runs on every native target. Hand-authored; the inventories are generated.

| Layer | Where | What it proves | Runs on |
| --- | --- | --- | --- |
| **Oracle and unit tests** | `src/commonTest` | The library returns the *right* value: every feature is checked against expected values independent of the library. | Every target (`jvmTest`, `linuxX64Test`, `linuxArm64Test`, `macosArm64Test`, `iosSimulatorArm64Test`, `mingwX64Test`), see `.github/workflows/ci.yml`. |
| **Snapshot tests** | `src/jvmTest` + `src/jvmTest/resources/snapshots` | Every result is unchanged: about 57 million values are recomputed and their fingerprints compared with the stored files. | JVM only (they use reflection, a thread pool and files). |

## Commands

```
./gradlew jvmTest                       # commonTest + snapshot tests on the JVM
./gradlew linuxX64Test                  # commonTest on Kotlin/Native (any native target task works the same way)
./gradlew allTests                      # every target the host can run
./gradlew jvmTest -PupdateSnapshots     # rewrite src/jvmTest/resources/snapshots from the current results
./gradlew jvmTest --tests "com.eskotech.luach.calendar.ParshaTest"
```

`JAVA_HOME` is optional: the Gradle daemon toolchain (`gradle/gradle-daemon-jvm.properties`) and `jvmToolchain(21)` resolve a JDK 21 through the Foojay resolver.

## The public API dump

`gradle/libs.versions.toml` applies the Kotlin binary-compatibility validator to the root project. `api/luach.api` is the dump of the library's public JVM API; `./gradlew apiCheck` (run by CI next to the JVM tests) fails when the compiled API differs from the dump, so an accidental change of a signature is caught before release. After an intended change, run `./gradlew apiDump` and commit the new dump together with the code. The `sample` project is excluded from the check.

## The oracle tests

The expected values come from independent sources inside the test code:

| Test | Oracle |
| --- | --- |
| `calendar/HebrewDateTest` | A table of the Gregorian dates of Rosh Hashanah 5770-5791 (the test-only object `ReferenceCalendar`) plus the month-length rules; every day of every year is converted in both directions and walked forward and back, month and year arithmetic, validation, and the molad recomputed from the lunation count. |
| `calendar/HolidayTest` | The holiday rules written out per date for four setting combinations; `holiday` and every boolean view for every day of 5770-5790; the Birkas Hachamah dates; the tekufa count; construction, `copy`, equality and `today`. |
| `calendar/ParshaTest` | Invariants over 5700-5900: Shabbosos without a reading are exactly the Yom Tov Shabbosos, every cycle reads every parsha once in order, weekdays have no parsha, anchored Shabbosos and special Shabbosos follow their rules, `upcomingParsha` is the next Shabbos with a reading, a list of remembered parshios. |
| `calendar/DafYomiTest` | The cycle start dates and the page counts: every day since 1975 (and the 12-page Shekalim cycles before) for Bavli, every day since 1980 for Yerushalmi, published cycle dates and anchors, the tractate tables, the `HebrewDay` overload. |
| `calendar/TefillahRulesTest` | Every rule re-derived from the holiday table for every day of 5770-5790 with default customs, the published vesein tal umatar start dates, each custom in isolation, and the `all` summary against the individual rules. |
| `format/LuachFormatterTest` | Literal expected strings for numbers, dates, weekdays, holidays, Rosh Chodesh, omer, the year signature, parsha, daf, zman names and times of day. |
| `zmanim/ZmanimTest` | Golden times for Lakewood on 2017-10-17 produced by an independent implementation; every edge and every fraction of the day recomputed from its definition for seven locations, eight dates, both models and both elevation settings; the kiddush levana windows against the molad; melacha; polar null propagation; settings and `copy`; the standard list against the individual members, the named edges, days and shitos, `today` and the location flags. |
| `solar/SolarModelTest` | A table of about 300 sunrise, sunset and noon times produced by an independent implementation of the two algorithms for seven locations; textbook values; model agreement away from the poles; the apparent-horizon formulas; solar elevation and azimuth. |
| `solar/SolarDayTest` | Twilight golden values, elevation and horizon effects, polar days, local-date placement for ordinary, date-line and zone-mismatched locations, solar noon against the day's midpoint, solar midnight, the depression search round trip, temporal hours. |
| `geo/LocationTest` | Hand-computed geodesic and rhumb-line values, coordinates from degrees-minutes-seconds, validation, equality and the `inIsrael` and `mukefChoma` flags. |
| `LuachSmokeTest` | The version constant has the `major.minor.patch` shape. |

Shared helpers: `Mismatches` collects failures over a sweep and reports the count with the first 25 messages (`assertNone(label)`), so one run shows every mismatch; `Assertions.kt` compares instants and durations with a tolerance (2 ms for the golden values, 15 ms for times built on temporal hours); `Places` holds the fixed locations.

`VersionTest` in `src/jvmTest` compares `Luach.VERSION` with the Gradle version, which the `jvmTest` task passes as the `luach.version` system property, so a bump of one without the other fails the build.

## The snapshot tests

`Snapshot.kt` records values (instants, durations, doubles, longs, strings, lists, maps, or the exception class when a call throws) under a *member* name and a *group*, and folds them into three fingerprints per key: the number of values, an exact hash (instants and durations at millisecond precision, so that a last-bit floating-point difference between CPU architectures leaves it unchanged; doubles exact), and a coarse hash (instants rounded to the second, doubles to six significant digits). `SnapshotFixtures.kt` supplies the places (twenty, including the date-line and daylight-saving edge cases), the date samples (every day of 2024 for four places, spot dates for all, extra ranges for Rapa Iti 2027-28 and Anadyr 2071), the model and elevation configurations, the catalogue of day edges and halachic days that `ZmanimSnapshotTest` sweeps, and reflection-based getter discovery (`Getters.zeroArg`), which is how a newly added public getter is picked up automatically.

Each `*SnapshotTest` builds one `Snapshot` per scenario and calls `assertMatches()`:

- The stored file `src/jvmTest/resources/snapshots/<name>.txt` is parsed (`#` lines are comments; the `environment` line records the JVM that wrote it).
- Keys present in both are compared: on the same JVM the exact hash must match; on a different JVM the coarse hash is enough, and the test prints that it matched "to the second".
- A failure lists the changed, new and missing members and groups (up to 40 each), so the message names the members and the locations or date ranges whose results changed.
- With `-PupdateSnapshots` the file is rewritten instead (the Gradle task passes the directory as the `snapshots.update` system property and always reruns). Review the diff: a changed count means members appeared or disappeared; changed hashes with the same count mean values changed.

Coverage by file: every zero-argument getter of `SolarDay` and `Zmanim` for every sample, plus every catalogued edge and day, the hours of the day and the half-day grid; the models' raw UTC hours, horizon variants, solar elevation and azimuth; `Location` distances and validation; every `HebrewDate` and `HebrewDay` getter for every day from 1850 to 2150 (Israel and diaspora) plus a walled-city sweep 1990-2060, the arithmetic, the factories and every molad; every `TefillahRules` rule for every day from 1990 to 2060 under every custom; every formatter output for every day from 1900 to 2100; every daf to 2100.

## Inventories

<!-- generated:test-inventory - written by docs/generate.py; to change it, edit the generator -->

#### `commonTest` (84 tests)

| File | Classes | Tests |
| --- | --- | ---: |
| `src/commonTest/kotlin/com/eskotech/luach/Assertions.kt` |  | 0 |
| `src/commonTest/kotlin/com/eskotech/luach/LuachSmokeTest.kt` | `LuachSmokeTest` | 1 |
| `src/commonTest/kotlin/com/eskotech/luach/Mismatches.kt` | `Mismatches` | 0 |
| `src/commonTest/kotlin/com/eskotech/luach/Places.kt` | `Places` | 0 |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/DafYomiTest.kt` | `DafYomiTest` | 7 |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/HebrewDateTest.kt` | `HebrewDateTest` | 11 |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/HolidayTest.kt` | `HolidayTest` | 8 |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/ParshaTest.kt` | `ParshaTest` | 8 |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/ReferenceCalendar.kt` | `ReferenceCalendar` | 0 |
| `src/commonTest/kotlin/com/eskotech/luach/calendar/TefillahRulesTest.kt` | `TefillahRulesTest` | 5 |
| `src/commonTest/kotlin/com/eskotech/luach/format/LuachFormatterTest.kt` | `LuachFormatterTest` | 11 |
| `src/commonTest/kotlin/com/eskotech/luach/geo/LocationTest.kt` | `LocationTest` | 5 |
| `src/commonTest/kotlin/com/eskotech/luach/solar/SolarDayTest.kt` | `SolarDayTest` | 11 |
| `src/commonTest/kotlin/com/eskotech/luach/solar/SolarModelTest.kt` | `SolarModelTest` | 6 |
| `src/commonTest/kotlin/com/eskotech/luach/zmanim/ZmanimTest.kt` | `ZmanimTest` | 11 |

#### `jvmTest` (40 tests)

| File | Classes | Tests |
| --- | --- | ---: |
| `src/jvmTest/kotlin/com/eskotech/luach/CalendarSnapshotTest.kt` | `CalendarSnapshotTest` | 7 |
| `src/jvmTest/kotlin/com/eskotech/luach/DafYomiSnapshotTest.kt` | `DafYomiSnapshotTest` | 5 |
| `src/jvmTest/kotlin/com/eskotech/luach/FormatterSnapshotTest.kt` | `FormatterSnapshotTest` | 7 |
| `src/jvmTest/kotlin/com/eskotech/luach/LocationSnapshotTest.kt` | `LocationSnapshotTest` | 2 |
| `src/jvmTest/kotlin/com/eskotech/luach/Snapshot.kt` | `Fingerprint`, `SnapshotRecorder`, `Snapshot` | 0 |
| `src/jvmTest/kotlin/com/eskotech/luach/SnapshotFixtures.kt` | `SamplePlaces`, `SampleDates`, `ZmanimCatalogue`, `Getter`, `Getters` | 0 |
| `src/jvmTest/kotlin/com/eskotech/luach/SolarSnapshotTest.kt` | `SolarSnapshotTest` | 8 |
| `src/jvmTest/kotlin/com/eskotech/luach/TefillahSnapshotTest.kt` | `TefillahSnapshotTest` | 2 |
| `src/jvmTest/kotlin/com/eskotech/luach/VersionTest.kt` | `VersionTest` | 1 |
| `src/jvmTest/kotlin/com/eskotech/luach/ZmanimSnapshotTest.kt` | `ZmanimSnapshotTest` | 8 |

Total: 124 tests.

<!-- /generated:test-inventory -->

<!-- generated:snapshot-inventory - written by docs/generate.py; to change it, edit the generator -->

| Snapshot | Values | Members | Groups |
| --- | ---: | ---: | ---: |
| `Calendar.arithmetic.txt` | 63,644 | 27 | 2 |
| `Calendar.everyDay1850To2150.txt` | 8,795,040 | 52 | 31 |
| `Calendar.factoriesAndStatics.txt` | 64,880 | 13 | 5 |
| `Calendar.instantConstructors.txt` | 36,960 | 2 | 6 |
| `Calendar.mukefChomaEveryDay1990To2060.txt` | 1,452,248 | 28 | 8 |
| `Calendar.parshaSchedules.txt` | 805 | 4 | 3 |
| `DafYomi.bavliEveryDay1923To2100.txt` | 129,522 | 2 | 19 |
| `DafYomi.tractates.txt` | 3 | 3 | 1 |
| `DafYomi.yerushalmiEveryDay1980To2100.txt` | 50,472 | 2 | 13 |
| `Formatter.dafFormats.txt` | 6,720 | 3 | 5 |
| `Formatter.defaults.txt` | 103 | 23 | 1 |
| `Formatter.everyDay1900To2100.txt` | 21,143,232 | 8 | 18 |
| `Formatter.numbers.txt` | 80,200 | 1 | 8 |
| `Formatter.times.txt` | 1,636 | 5 | 4 |
| `Formatter.userSuppliedNames.txt` | 245,440 | 8 | 2 |
| `Formatter.yearSignatures.txt` | 2,739 | 1 | 1 |
| `Location.constructionAndValidation.txt` | 282 | 5 | 3 |
| `Location.distancesAndBearings.txt` | 1,800 | 2 | 30 |
| `SolarDay.depressionSearch.txt` | 16 | 2 | 8 |
| `SolarDay.eventsAtDepressions.txt` | 588,032 | 2 | 40 |
| `SolarDay.temporalHoursWithArbitraryBounds.txt` | 294,016 | 1 | 40 |
| `SolarDay.zeroArgGetters.txt` | 64,316 | 14 | 40 |
| `SolarModel.elevationAndAzimuth.txt` | 181,440 | 2 | 1 |
| `SolarModel.horizonVariants.txt` | 84,963 | 8 | 7 |
| `SolarModel.utcHours.txt` | 263,840 | 3 | 40 |
| `Tefillah.allRulesEveryDay1990To2060.txt` | 18,879,224 | 14 | 13 |
| `Tefillah.defaultCustoms.txt` | 12 | 12 | 1 |
| `Zmanim.catalogue.txt` | 6 | 6 | 1 |
| `Zmanim.edgesAndDays.txt` | 2,940,160 | 320 | 80 |
| `Zmanim.hadlakasNeirosOffsets.txt` | 3,150 | 1 | 50 |
| `Zmanim.hoursOfTheDay.txt` | 1,069,740 | 282 | 20 |
| `Zmanim.isAsurBemelacha.txt` | 323,154 | 2 | 12 |
| `Zmanim.kiddushLevana.txt` | 96,474 | 5 | 40 |
| `Zmanim.standardList.txt` | 64,260 | 51 | 20 |
| `Zmanim.zeroArgGetters.txt` | 91,880 | 10 | 80 |
| **Total** | **57,020,409** |  |  |

<!-- /generated:snapshot-inventory -->
