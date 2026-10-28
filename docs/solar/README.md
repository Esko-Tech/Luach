# Sunrise, sunset and the solar day

`SolarDay` (`src/commonMain/kotlin/com/eskotech/luach/solar/SolarDay.kt`) turns a `Location`, a `LocalDate` and a `SolarModel` into the sun's events of that civil day as `kotlin.time.Instant`s: sunrise, sunset, the three twilights, solar noon and midnight, mean solar noon, and the time at which the sun is any number of degrees below (or above) the horizon. Everything in [`../zmanim/README.md`](../zmanim/README.md) is built from these primitives. Hand-authored; the constants table at the end is generated.

## Mental model

A `SolarDay` is four values: `location`, `date` (the civil date in the location's time zone), `model` (default `NoaaSolarModel`) and `horizon` (default `ApparentHorizon()`). It is a data class, so `copy(date = ...)` moves to another day. Every event is computed in two steps:

1. The model returns the event as a **UTC hour of the day** (a `Double` in `[0, 24)`, or `null` when the sun does not reach that zenith on that day) for a UTC date, a latitude, a longitude and a zenith angle.
2. `SolarDay` places that hour on the right UTC date and returns an `Instant` (always UTC; the time zone only decides which solar day belongs to the civil date).

Every property access recomputes from scratch.

## Depression angles

Zmanim are stated as "the sun N degrees below the horizon", so `SolarDay` takes a **depression**: `sunriseAt(depression)` and `sunsetAt(depression)` compute the sun at `depression` degrees below the geometric horizon (zenith `90 + depression`); a negative depression is the sun above the horizon. The twilights are `sunriseAt(6.0)`, `sunriseAt(12.0)` and `sunriseAt(18.0)` (`civilDawn`, `nauticalDawn`, `astronomicalDawn`) and their dusk counterparts.

A depression of exactly `0.0` is special: it means the **apparent** sunrise or sunset, so the zenith is corrected by `ApparentHorizon.zenith(elevation)`: 90° plus the refraction (34′), the solar radius (16′) and the dip of the horizon seen from the elevation, `acos(R / (R + elevation / 1000))` with `R = earthRadius` (6356.9 km): 754 m give about 0.88°. Any other depression is used as given, without refraction or elevation. So:

- `sunrise` / `sunset` = `sunriseAt(0.0)` / `sunsetAt(0.0)`: apparent, corrected for the location's elevation.
- `seaLevelSunrise` / `seaLevelSunset` = `sunriseAt(0.0, atElevation = false)`: apparent, with refraction and radius only.
- `sunriseAt(16.1)`: geometric, the same for every value of `atElevation`.

`ApparentHorizon` is a data class whose `refraction`, `solarRadius` and `earthRadius` can be changed with `copy`; `ApparentHorizon(0.0, 0.0)` gives the geometric sunrise.

## Placing an hour on the right day

The models compute for a UTC date, while the request names a civil date in the location's zone. `SolarDay` picks the UTC date whose mean solar noon is nearest the location's civil noon: `localNoon` is the instant of 12:00 on `date` in `location.timeZone` (the start of the day plus twelve hours, so a date on which the clock skips an hour is still handled), and the UTC date is that of `localNoon + longitude × 4 minutes`. For Lakewood that is the same date; for Apia (171.75°W, UTC+13 or +14) it is the previous UTC date, because the clock there is a day ahead of the sun; for Baker Island (176.5°W, UTC−12) it is the requested date.

The returned hour is then placed on that UTC date and moved by a whole day if that brings it closer to the reference point of the event. Mean solar noon is 12:00 UTC minus `longitude × 4 minutes` on that UTC date, and every other event is placed relative to it: six hours before mean solar noon for a rising event, six hours after it for a setting event, mean solar noon itself for solar noon. So the events of a civil day bracket one solar noon, sunrise before sunset, however far the zone is from the longitude (a location at 180° in the UTC zone, a Los Angeles location in the Tokyo zone); a summer sunset at 00:30 of the next calendar day stays on the requested solar day and a dawn at 01:30 on the requested morning, at any longitude and in any zone, including the day after a date-line change (Samoa on 2011-12-31, Kiritimati on 1995-01-01).

`solarMidnight` is halfway between the day's solar noon and the next day's.

## Mean solar noon

`meanSolarNoon` is 12:00 local mean time: 12:00 UTC minus `longitude × 4 minutes`, on the UTC date chosen above. It depends only on the longitude and the date, so the zone's offset and daylight saving leave it unchanged, and it always has a value. `Zmanim.fixedLocalChatzos` is this value.

## Missing events and null

A model returns `null` on a day without the event: polar day or night for the horizon, or a deep depression in summer at high latitude. Every `Instant?` member of `SolarDay` is then `null`, `temporalHour` (a `Duration?`) is null when either sea-level event is null, and the zmanim layer propagates null into everything built on a missing event. The NOAA model always has a solar noon; the almanac model's noon is the midpoint of its sunrise and sunset and is null when they are.

## Temporal hours

`temporalHour` is `(seaLevelSunset − seaLevelSunrise) / 12` as a `kotlin.time.Duration?`; the companion `SolarDay.temporalHour(start, end)` divides any interval by twelve and returns null when either bound is null. Durations keep nanosecond precision.

## The depression for a minute offset

`depressionBeforeSunrise(minutes)` answers "how many degrees below the horizon is the sun `minutes` before sea-level sunrise today?", and `depressionAfterSunset(minutes)` the same after sunset. Negative minutes ask about the sun above the horizon after sunrise or before sunset. The search is a bisection over the geometric depression (60 steps): from the depression of the apparent horizon at sea level (0.833°, refraction plus the solar radius) up to 90° for a positive offset, and from −90° up to it for a negative one, so the few minutes between the apparent and the geometric sunrise get their small positive depression. The result is a geometric depression, ready for `sunriseAt` or `sunsetAt`, and it is `null` when the day has no sea-level event or the sun stays short of the depth or height the offset implies (a long offset in summer at high latitude). `SolarDayTest.depressionSearchRoundTrips` checks that `sunriseAt(depression)` lands within a second of the requested time.

## The two models

| | `NoaaSolarModel` (default) | `AlmanacSolarModel` |
| --- | --- | --- |
| `name` | `NOAA solar position` | `Almanac for Computers sunrise and sunset` |
| Solar position | Julian centuries since J2000: mean longitude and anomaly, equation of centre, apparent longitude, obliquity, declination, equation of time | Day of year: mean anomaly, true longitude, right ascension, declination |
| Sunrise and sunset | Two passes: a first estimate from solar noon, then a refinement at the estimated time | One pass from the approximate rise or set time |
| Solar noon | From the equation of time (two passes) | Midpoint of the geometric sunrise and sunset |
| Extra | `elevation(utcTime, latitude, longitude)` and `azimuth(...)` for a `LocalDateTime` read as UTC, with the solar position evaluated at that instant: the sun's altitude in degrees and its bearing clockwise from north in `[0, 360)` | none |

Both are stateless objects implementing `SolarModel` (`sunrise`, `sunset`, `solarNoon`, each with a date, a latitude, a longitude and, for the first two, a zenith). They agree within two minutes away from the poles (`SolarModelTest.modelsAgreeWithEachOtherAwayFromThePoles`), and each is compared with an independently produced reference table of about 300 times in `SolarModelTest`. Swap models with `SolarDay(location, date, model = AlmanacSolarModel)` or `zmanim.copy(model = AlmanacSolarModel)`.

## Constants and defaults

<!-- generated:solar-constants - written by docs/generate.py; to change it, edit the generator -->

| Class | Member | Type | Default or value |
| --- | --- | --- | --- |
| `ApparentHorizon` | `refraction` | `Double` | `34.0 / 60` |
| `ApparentHorizon` | `solarRadius` | `Double` | `16.0 / 60` |
| `ApparentHorizon` | `earthRadius` | `Double` | `6356.9` |
| `ApparentHorizon` | `GEOMETRIC_HORIZON` | `Double` | `90.0` |
| `SolarDay` | `model` | `SolarModel` | `NoaaSolarModel` |
| `SolarDay` | `horizon` | `ApparentHorizon` | `ApparentHorizon()` |
| `SolarDay` | `CIVIL_TWILIGHT` | `Double` | `6.0` |
| `SolarDay` | `NAUTICAL_TWILIGHT` | `Double` | `12.0` |
| `SolarDay` | `ASTRONOMICAL_TWILIGHT` | `Double` | `18.0` |
| `Zmanim` | `model` | `SolarModel` | `NoaaSolarModel` |
| `Zmanim` | `horizon` | `ApparentHorizon` | `ApparentHorizon()` |
| `Zmanim` | `useElevation` | `Boolean` | `false` |
| `Zmanim` | `hadlakasNeirosMinutes` | `Double` | `18.0` |
| `Molad` | `PARTS_PER_MINUTE` | `Long` | `18` |
| `Molad` | `PARTS_PER_HOUR` | `Long` | `60 * PARTS_PER_MINUTE` |
| `Molad` | `PARTS_PER_DAY` | `Long` | `24 * PARTS_PER_HOUR` |
| `Molad` | `LUNATION_PARTS` | `Long` | `29 * PARTS_PER_DAY + 12 * PARTS_PER_HOUR + 793` |

<!-- /generated:solar-constants -->
