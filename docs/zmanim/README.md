# Zmanim

`Zmanim` (`src/commonMain/kotlin/com/eskotech/luach/zmanim/Zmanim.kt`) computes the times of the halachic day for a `Location` and a `LocalDate`. It takes the opinion as a value: a `DayEdge` describes how a dawn or dusk is derived from sunrise or sunset, and a `HalachicDay` pairs a dawn edge with a dusk edge. Hand-authored, except the member tables at the end.

## Mental model

Every zman is one of four things:

1. **An edge of the day**: `alos(edge)` or `tzais(edge)` for a `DayEdge`.
2. **A fraction of a day**: `hourOfDay(hours, day)` = `alos + hours × (tzais − alos) / 12` for a `HalachicDay`. Sof zman shema is 3 such hours, sof zman tefillah (and eating chametz) 4, burning chametz 5, mincha gedola 6.5, samuch le mincha ketana 9, mincha ketana 9.5 and plag hamincha 10.75; each opinion is just a different day.
3. **A time of the solar day**, independent of any opinion: `chatzos` (solar noon), `fixedLocalChatzos` (mean solar noon), `chatzosHalaylah`, `hadlakasNeiros`.
4. **A molad-based time** (the molad itself and the kiddush levana window), which the calendar computes and `Zmanim` clips to the civil day.

Null propagates: a missing edge (polar day, a 26° dawn in June at 70°N) is null, the temporal hour built on it is null, and every zman built on either is null (`ZmanimTest.polarDayAndNightGiveNullEverywhere`).

## Settings

`Zmanim(location, date, model = NoaaSolarModel, horizon = ApparentHorizon(), useElevation = false, hadlakasNeirosMinutes = 18.0)` is a data class; change a setting with `copy`. `Zmanim.today(location, ...)` takes the same settings and uses today's date in the location's zone. `hebrewDay` is the `HebrewDay` of the date with the location's `inIsrael` and `mukefChoma` flags.

- `useElevation` decides whether `hanetz` and `shkiah`, and therefore every edge measured from them, are the elevation-adjusted events or the sea-level events of the `SolarDay` (`solar`). Depression edges are independent of elevation ([`../solar/README.md`](../solar/README.md#depression-angles)).
- `hadlakasNeirosMinutes` (default 18) is subtracted from `shkiah` for `hadlakasNeiros`.
- `model` and `horizon` are passed to the `SolarDay`.

## Day edges

| `DayEdge` | Dawn | Dusk |
| --- | --- | --- |
| `Horizon` | `hanetz` | `shkiah` |
| `Depression(degrees)` | the sun `degrees` below the horizon before sunrise (`solar.sunriseAt`) | the same after sunset (`solar.sunsetAt`); negative degrees are above the horizon |
| `Minutes(minutes)` | `hanetz − minutes` | `shkiah + minutes`; negative minutes move towards the day |
| `ProportionalMinutes(minutes)` | `hanetz − minutes/60 × H` where `H` is the sunrise-to-sunset temporal hour | `shkiah + minutes/60 × H` |
| `Shifted(edge, minutes)` | `alos(edge) + minutes` | `tzais(edge) + minutes` |
| `DawnInterval(dawn, factor)` | `hanetz − factor × (hanetz − alos(alos))` | `shkiah + factor × (hanetz − alos(alos))`: the morning interval, scaled, applied after sunset |

`alos` and `tzais` are the conventional defaults `alos(DayEdge.ALOS_16_1)` and `tzais(DayEdge.TZAIS_GEONIM_8_5)`.

`DayEdge` carries named constants for the common opinions, so the usual edges are found by autocomplete: `ALOS_16_1`, `ALOS_18`, `ALOS_19_8`, `ALOS_26`, `ALOS_BAAL_HATANYA` (16.9°), `ALOS_72`, `ALOS_90`, `ALOS_120`, `ALOS_72_ZMANIYOS`; `MISHEYAKIR_11_5`, `MISHEYAKIR_11`, `MISHEYAKIR_10_2`, `MISHEYAKIR_9_5`, `MISHEYAKIR_7_65`; `TZAIS_GEONIM_3_7`, `TZAIS_GEONIM_3_8`, `TZAIS_GEONIM_5_95`, `TZAIS_GEONIM_7_083`, `TZAIS_GEONIM_8_5`, `TZAIS_BAAL_HATANYA` (6°), `TZAIS_RABBEINU_TAM_13_24`, `TZAIS_16_1`, `TZAIS_18`, `TZAIS_19_8`, `TZAIS_26`, `TZAIS_72`, `TZAIS_90`, `TZAIS_96`, `TZAIS_120`, `TZAIS_72_ZMANIYOS`. Each is just the `Depression`, `Minutes` or `ProportionalMinutes` value its name says.

## Halachic days

`HalachicDay(alos, tzais)` with the presets `GRA` (hanetz to shkiah), `MAGEN_AVRAHAM_72`, `MAGEN_AVRAHAM_90`, `MAGEN_AVRAHAM_120`, `MAGEN_AVRAHAM_72_ZMANIYOS`, `MAGEN_AVRAHAM_16_1`, `MAGEN_AVRAHAM_18`, `MAGEN_AVRAHAM_19_8`, `MAGEN_AVRAHAM_26`, `BAAL_HATANYA` (1.583° to 1.583°), `ATERET_TORAH` (72 proportional minutes to 40 minutes after shkiah) and the factories `minutes(n)`, `proportionalMinutes(n)`, `degrees(d)`, `degrees(alos, tzais)`. `shaahZmanis(day)` is `(tzais − alos) / 12` as a `Duration?`.

The fraction functions all take a `HalachicDay` (default `GRA`): `sofZmanShema`, `sofZmanTefillah`, `sofZmanAchilasChametz`, `sofZmanBiurChametz`, `minchaGedola`, `samuchLeMinchaKetana`, `minchaKetana`, `plagHamincha`, and the general `hourOfDay(hours, day)`. `minchaGedola(day, atLeastHalfHourAfterChatzos = true)` returns the later of the 6.5-hour time and `chatzos + 30 minutes`; `minchaGedolaFromChatzos(day)` is the later of `chatzos + 30 minutes` and `chatzos + H / 2`.

`halfDayHour(hours, start, end)` divides an arbitrary half day into six: `start + hours × (end − start) / 6`. It is how the fixed-local-chatzos family is expressed: `halfDayHour(3.0, alos(edge), fixedLocalChatzos)` for sof zman shema counted to mean solar noon, `halfDayHour(3.5, fixedLocalChatzos, shkiah)` for mincha ketana counted from it.

## Chatzos, candle lighting, midnight

- `chatzos` is the model's solar noon; `fixedLocalChatzos` is mean solar noon, `12:00 UTC − longitude × 4 minutes`, independent of the zone's offset and always present.
- `hadlakasNeiros` is `shkiah − hadlakasNeirosMinutes`.
- `chatzosHalaylah` is halfway between today's and tomorrow's solar noon.

## The molad and kiddush levana

The calendar gives every month's `Molad` with its `instant` (Jerusalem mean solar time) and the four derived instants: `earliestKiddushLevana3Days` (+72 h), `earliestKiddushLevana7Days` (+168 h), `latestKiddushLevanaHalfMonth` (+14 d 18 h 22 m 1.666 s) and `latestKiddushLevana15Days` (+360 h). `Zmanim` looks at the molad of the previous, current and next Hebrew month and returns the value that falls within the civil day (local midnight to local midnight), else null:

- `molad`: the molad instant if it falls today.
- `kiddushLevanaStart(rule, alos = null, tzais = null)` with `THREE_DAYS` or `SEVEN_DAYS`: the earliest time if it falls today; when `alos` and `tzais` are given and the time falls between them (daytime), `tzais` is returned instead, since the moon is blessed at night.
- `kiddushLevanaEnd(rule, alos = null, tzais = null)` with `HALF_MONTH` or `FIFTEEN_DAYS`: the latest time if it falls today; a daytime value is replaced by `alos`, the last opportunity before it.

## Melacha

`isAsurBemelacha(at, tzais)` is true from `shkiah` on a day with candle lighting (Friday, erev Yom Tov, erev of a second day) and until `tzais` on Shabbos or a Yom Tov on which work is forbidden; it is false when `tzais` is null or `shkiah` is null. The day's `inIsrael` and `mukefChoma` flags come from the `Location`, so a place marked as being in Israel keeps its second days out of the check.

## The standard list

`Zman` enumerates the zmanim an app usually shows, in the order of the day: `ALOS`, `MISHEYAKIR`, `HANETZ`, `SOF_ZMAN_SHEMA`, `SOF_ZMAN_TEFILLAH`, `SOF_ZMAN_ACHILAS_CHAMETZ`, `SOF_ZMAN_BIUR_CHAMETZ`, `CHATZOS`, `MINCHA_GEDOLA`, `SAMUCH_LEMINCHA_KETANA`, `MINCHA_KETANA`, `PLAG_HAMINCHA`, `HADLAKAS_NEIROS`, `SHKIAH`, `TZAIS`, `CHATZOS_HALAYLAH`.

`Shitos(alos, misheyakir, day, tzais)` is the set of opinions that list is computed with: the alos edge, the misheyakir edge, the `HalachicDay` for the fractions, and the tzais edge. The default is `Shitos.GRA` (`ALOS_16_1`, `MISHEYAKIR_11_5`, `HalachicDay.GRA`, `TZAIS_GEONIM_8_5`); `Shitos.MAGEN_AVRAHAM` uses 72 minutes for alos, the day and tzais. Any combination can be built, for example `Shitos(DayEdge.ALOS_BAAL_HATANYA, DayEdge.MISHEYAKIR_10_2, HalachicDay.BAAL_HATANYA, DayEdge.TZAIS_BAAL_HATANYA)`.

- `zman(zman, shitos = Shitos())` is one entry, the same value the corresponding member returns.
- `all(shitos = Shitos())` is every entry as a `Map<Zman, Instant?>` in enum order, nulls included, ready to be rendered with `LuachFormatter.formatZman` and `formatTime` ([`../formatting/README.md`](../formatting/README.md)).

## Cookbook

| Opinion | Expression |
| --- | --- |
| Alos 72 fixed minutes | `alos(Minutes(72.0))` |
| Alos 72 proportional minutes (1.2 hours) | `alos(ProportionalMinutes(72.0))` |
| Alos 16.1°, 18°, 19.8°, 26° | `alos(Depression(16.1))`, ... |
| Misheyakir 11.5°, 11°, 10.2°, 9.5°, 7.65° | `alos(Depression(11.5))`, ... |
| Tzais of the Geonim at 3.7°–9.75°, Rabbeinu Tam at 13.24°, 72, 90, 96, 120 minutes | `tzais(Depression(3.8))`, `tzais(Minutes(72.0))`, ... |
| Bain hashmashos of the Yereim: 18, 16.875, 13.5 minutes before sunset, or 3.05°, 2.8°, 2.1° above the horizon | `tzais(Minutes(-18.0))`, `tzais(Depression(-2.1))` |
| 13.5 minutes before the 7.083° tzais | `tzais(Shifted(Depression(7 + 5.0 / 60), -13.5))` |
| Rabbeinu Tam's two stars: 5/18 of the 19.8° dawn interval after sunset | `tzais(DawnInterval(Depression(19.8), 5 / 18.0))` |
| Sof zman shema of the GRA, of the MGA (72 minutes), at 16.1° | `sofZmanShema()`, `sofZmanShema(HalachicDay.MAGEN_AVRAHAM_72)`, `sofZmanShema(HalachicDay.MAGEN_AVRAHAM_16_1)` |
| Sof zman shema three hours before chatzos | `chatzos?.minus(3.hours)` |
| Mincha gedola 30 minutes after chatzos | `chatzos?.plus(30.minutes)`, or `minchaGedola(day, atLeastHalfHourAfterChatzos = true)` |
| Ahavat Shalom: mincha ketana and plag between 16.1° and 3.8°; mincha gedola from chatzos with the 16.1°–3.7° hour | `minchaKetana(HalachicDay.degrees(16.1, 3.8))`, `plagHamincha(HalachicDay.degrees(16.1, 3.8))`, `minchaGedolaFromChatzos(HalachicDay.degrees(16.1, 3.7))` |
| Ateret Torah: 72 proportional minutes to 40 minutes after sunset | `HalachicDay(ProportionalMinutes(72.0), Minutes(40.0))` |
| Baal Hatanya: the day from 1.583° to 1.583°, dawn at 16.9°, tzais at 6° | `HalachicDay.degrees(1.583)`, `alos(Depression(16.9))`, `tzais(Depression(6.0))` |
| Plag from 16.1° to sunset | `plagHamincha(HalachicDay(Depression(16.1), Horizon))` |
| Sof zman shema from 16.1° to fixed local chatzos | `halfDayHour(3.0, alos(Depression(16.1)), fixedLocalChatzos)` |
| Chametz: eating until the 4th hour, burning until the 5th | `sofZmanAchilasChametz(day)`, `sofZmanBiurChametz(day)` |
| The whole list for one set of opinions | `all()`, `all(Shitos.MAGEN_AVRAHAM)`, `all(Shitos(alos = DayEdge.ALOS_72, day = HalachicDay.MAGEN_AVRAHAM_72))` |

## Members

<!-- generated:zmanim-members - written by docs/generate.py; to change it, edit the generator -->

### `SolarDay`

| Member | Type | Definition |
| --- | --- | --- |
| `localNoon` | `Instant` | `date.atStartOfDayIn(location.timeZone) + 12.hours` |
| `sunrise` | `Instant?` | `sunriseAt(0.0)` |
| `sunset` | `Instant?` | `sunsetAt(0.0)` |
| `seaLevelSunrise` | `Instant?` | `sunriseAt(0.0, atElevation = false)` |
| `seaLevelSunset` | `Instant?` | `sunsetAt(0.0, atElevation = false)` |
| `civilDawn` | `Instant?` | `sunriseAt(CIVIL_TWILIGHT)` |
| `nauticalDawn` | `Instant?` | `sunriseAt(NAUTICAL_TWILIGHT)` |
| `astronomicalDawn` | `Instant?` | `sunriseAt(ASTRONOMICAL_TWILIGHT)` |
| `civilDusk` | `Instant?` | `sunsetAt(CIVIL_TWILIGHT)` |
| `nauticalDusk` | `Instant?` | `sunsetAt(NAUTICAL_TWILIGHT)` |
| `astronomicalDusk` | `Instant?` | `sunsetAt(ASTRONOMICAL_TWILIGHT)` |
| `solarNoon` | `Instant?` | `place(model.solarNoon(utcDate, location.latitude, location.longitude), meanSolarNoon)` |
| `solarMidnight` | `Instant?` | computed in a block; see the source |
| `meanSolarNoon` | `Instant` | `utcDate.atStartOfDayIn(TimeZone.UTC) + (12.0 - location.longitude / 15).hours` |
| `temporalHour` | `Duration?` | `temporalHour(seaLevelSunrise, seaLevelSunset)` |
| `sunriseAt(depression: Double, atElevation: Boolean = true)` | `Instant?` | `place(model.sunrise(utcDate, location.latitude, location.longitude, zenith(depression, atElevation)), meanSolarNoon - 6.hours)` |
| `sunsetAt(depression: Double, atElevation: Boolean = true)` | `Instant?` | `place(model.sunset(utcDate, location.latitude, location.longitude, zenith(depression, atElevation)), meanSolarNoon + 6.hours)` |
| `depressionBeforeSunrise(minutes: Double)` | `Double?` | computed in a block; see the source |
| `depressionAfterSunset(minutes: Double)` | `Double?` | computed in a block; see the source |

### `Zmanim`

| Member | Type | Definition |
| --- | --- | --- |
| `solar` | `SolarDay` | `SolarDay(location, date, model, horizon)` |
| `hebrewDay` | `HebrewDay` | `HebrewDay(date, location.inIsrael, location.mukefChoma)` |
| `hanetz` | `Instant?` | `if (useElevation) solar.sunrise else solar.seaLevelSunrise` |
| `shkiah` | `Instant?` | `if (useElevation) solar.sunset else solar.seaLevelSunset` |
| `chatzos` | `Instant?` | `solar.solarNoon` |
| `fixedLocalChatzos` | `Instant` | `solar.meanSolarNoon` |
| `chatzosHalaylah` | `Instant?` | `solar.solarMidnight` |
| `hadlakasNeiros` | `Instant?` | `shkiah?.minus(hadlakasNeirosMinutes.minutes)` |
| `alos` | `Instant?` | `alos(DayEdge.ALOS_16_1)` |
| `tzais` | `Instant?` | `tzais(DayEdge.TZAIS_GEONIM_8_5)` |
| `alos(edge: DayEdge)` | `Instant?` | `when (edge) {` |
| `tzais(edge: DayEdge)` | `Instant?` | `when (edge) {` |
| `shaahZmanis(day: HalachicDay = HalachicDay.GRA)` | `Duration?` | `SolarDay.temporalHour(alos(day.alos), tzais(day.tzais))` |
| `hourOfDay(hours: Double, day: HalachicDay = HalachicDay.GRA)` | `Instant?` | computed in a block; see the source |
| `sofZmanShema(day: HalachicDay = HalachicDay.GRA)` | `Instant?` | `hourOfDay(3.0, day)` |
| `sofZmanTefillah(day: HalachicDay = HalachicDay.GRA)` | `Instant?` | `hourOfDay(4.0, day)` |
| `sofZmanAchilasChametz(day: HalachicDay = HalachicDay.GRA)` | `Instant?` | `hourOfDay(4.0, day)` |
| `sofZmanBiurChametz(day: HalachicDay = HalachicDay.GRA)` | `Instant?` | `hourOfDay(5.0, day)` |
| `minchaGedola(day: HalachicDay = HalachicDay.GRA, atLeastHalfHourAfterChatzos: Boolean = false)` | `Instant?` | computed in a block; see the source |
| `minchaGedolaFromChatzos(day: HalachicDay)` | `Instant?` | computed in a block; see the source |
| `samuchLeMinchaKetana(day: HalachicDay = HalachicDay.GRA)` | `Instant?` | `hourOfDay(9.0, day)` |
| `minchaKetana(day: HalachicDay = HalachicDay.GRA)` | `Instant?` | `hourOfDay(9.5, day)` |
| `plagHamincha(day: HalachicDay = HalachicDay.GRA)` | `Instant?` | `hourOfDay(10.75, day)` |
| `halfDayHour(hours: Double, start: Instant?, end: Instant?)` | `Instant?` | computed in a block; see the source |
| `isAsurBemelacha(at: Instant, tzais: Instant?)` | `Boolean` | computed in a block; see the source |
| `zman(zman: Zman, shitos: Shitos = Shitos())` | `Instant?` | `when (zman) {` |
| `all(shitos: Shitos = Shitos())` | `Map<Zman, Instant?>` | `Zman.entries.associateWith { zman(it, shitos) }` |
| `molad` | `Instant?` | `moladsAround().map { it.instant }.firstOrNull { it in today }` |
| `kiddushLevanaStart(rule: KiddushLevanaStart, alos: Instant? = null, tzais: Instant? = null)` | `Instant?` | computed in a block; see the source |
| `kiddushLevanaEnd(rule: KiddushLevanaEnd, alos: Instant? = null, tzais: Instant? = null)` | `Instant?` | computed in a block; see the source |

<!-- /generated:zmanim-members -->
