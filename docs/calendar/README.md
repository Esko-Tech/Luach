# The Hebrew calendar

`HebrewDate` is the arithmetic core: an immutable day that knows its Hebrew year, month and day, its `LocalDate`, its weekday, the length and type of its year and month, and its month's molad. `HebrewDay` adds the meaning of the day - holidays, fasts, the parsha, the omer, the tekufa, Daf Yomi - under two settings: `inIsrael` and `mukefChoma`. Hand-authored; the month, holiday and parsha tables are generated. Files: `src/commonMain/kotlin/com/eskotech/luach/calendar/`.

## Mental model

- A `HebrewDate` is a value identified by its `epochDay` (days since 1970-01-01, the same count as `LocalDate.toEpochDays()`). `plusDays`, `plusMonths`, `plusYears`, `minus…`, `withDay`, `withMonth` and `withYear` return new dates; `compareTo`, `equals` and `hashCode` use the epoch day.
- Months are the `HebrewMonth` enum, numbered from Nisan: `NISAN(1)` ... `ELUL(6)`, `TISHREI(7)` ... `ADAR(12)`, `ADAR_II(13)`. A year starts with `TISHREI`. In a leap year `ADAR` is Adar I and `ADAR_II` is Adar II; in a common year `ADAR_II` is rejected. `HebrewMonth.inYearOrder(meuberes)` lists the months of a year in order.
- The weekday is `kotlinx.datetime.DayOfWeek`; Gregorian fields come from `localDate`.
- The holiday of a day is `HebrewDay.holiday: Holiday?` (an enum with `isTaanis`, `isErev` and `asurBemelacha` properties), with boolean views (`isYomTov`, `isTaanis`, `isPesach`, ...). Which holiday a date gets depends on `inIsrael` (second days, Isru Chag); `mukefChoma` only changes `isPurim`.
- The parsha is computed from the shape of the year: the Shabbosos of a Torah cycle are counted and the doubled parshios are chosen so that the readings fit ([below](#the-parsha-schedule)).

## HebrewDate

| Call | Result |
| --- | --- |
| `HebrewDate.of(year, month, day)`, `HebrewDate.of(year, monthNumber, day)` | That date; throws `IllegalArgumentException` for a year below 1, an `ADAR_II` in a common year, or a day outside `1..monthLength`. |
| `HebrewDate.from(localDate)`, `HebrewDate.from(instant, timeZone)`, `HebrewDate.today(timeZone)` | The Hebrew date of a civil date; an instant is read as the civil date in the zone, at any hour of the day. |
| `HebrewDate.ofEpochDay(epochDay)` | The inverse of `epochDay`; the earliest supported day is 1 Tishrei of the year 1 (epoch day −2092590). |
| `localDate`, `dayOfWeek`, `epochDay`, `dayOfYear` | The civil views and the ordinal within the Hebrew year (1 Tishrei is 1). |
| `isMeuberes`, `yearLength`, `yearType`, `monthLength`, `monthsInYear` | The year rules for this date's year and month. |
| `plusMonths(n)` | Walks Hebrew months (Elul to the next Tishrei, Adar or Adar II to Nisan); a day beyond the target month's length is clamped. `plusYears(n)` keeps month and day, maps `ADAR_II` to `ADAR` when the target year is common, and clamps the day. |
| `molad` | The `Molad` of this date's month. |
| `daysUntil(other)` | Signed day difference. |

Companion helpers: `isMeuberes(year)` (years 0, 3, 6, 8, 11, 14 and 17 of the 19-year cycle), `yearLength(year)`, `yearType(year)`, `monthLength(year, month)`, `roshHashanah(year)`, `molad(year, month)`.

## The year rules

Rosh Hashanah of a year is the day of the molad of Tishrei, delayed by the four postponements:

1. a molad at or after 18 hours of its day;
2. a molad on Tuesday at or after 9 h 204 p in a common year;
3. a molad on Monday at or after 15 h 589 p in the year after a leap year;
4. a Rosh Hashanah on Sunday, Wednesday or Friday moves one more day.

`yearLength` is the difference between two consecutive Rosh Hashanahs (353, 354, 355, 383, 384 or 385) and `YearType` follows from it: `DEFICIENT` (length ending in 3: Cheshvan and Kislev both 29 days), `REGULAR` (ending in 4), `COMPLETE` (ending in 5: both 30 days). Month lengths: Cheshvan 30 only in a complete year, Kislev 29 only in a deficient year, Adar 30 only in a leap year (as Adar I), Iyar, Tammuz, Elul, Tevet and Adar II 29, every other month 30.

## The molad

The fixed calendar counts a lunation as 29 days 12 hours 793 parts (`Molad.LUNATION_PARTS`; 1080 parts to the hour, 18 to the minute). The molad of Tishrei of the year 1 fell on the second day of the week at 5 hours 204 parts, and the molad of any later month is that plus one lunation per elapsed month (235 months per 19-year cycle). A `Molad` exposes `dayOfWeek`, `hour`, `minute` and `parts` of the traditional 18:00-based day, `clockHour` (`(hour + 18) mod 24`), `date` (the civil `HebrewDate` on which the molad falls: a molad at 6 hours or later of its day belongs to the next civil date), and `instant`: the molad read as Jerusalem mean solar time (the stated clock time, with the chalakim as thirds of a second, minus the Temple Mount longitude 35.2354° × 4 minutes). The four kiddush levana instants are offsets from it ([`../zmanim/README.md`](../zmanim/README.md#the-molad-and-kiddush-levana)).

`HebrewDateTest.moladMatchesFirstPrinciples` recomputes every molad of 5770-5790 from the lunation count and checks the weekday, the time, the civil date and the Rosh Hashanah postponements.

## HebrewDay

`HebrewDay(date, inIsrael = false, mukefChoma = false)`, also from a `LocalDate` or an `Instant` with a zone, or `HebrewDay.today(timeZone, inIsrael, mukefChoma)` for the current date in a zone. It is a data class: `copy` changes a setting, `plusDays(n)` moves the date and keeps the settings.

### Holidays

Exactly one `Holiday` per day, or null:

| Month | Rule |
| --- | --- |
| Nisan | 14 `EREV_PESACH`; 15, 21 (and 16, 22 outside Israel) `PESACH`; 17-20 (and 16 in Israel) `CHOL_HAMOED_PESACH`; 22 in Israel / 23 outside `ISRU_CHAG`. |
| Iyar | 14 `PESACH_SHENI`; 18 `LAG_BAOMER`. |
| Sivan | 5 `EREV_SHAVUOS`; 6 (and 7 outside Israel) `SHAVUOS`; 7 in Israel / 8 outside `ISRU_CHAG`. |
| Tammuz | 17 `SHIVA_ASAR_BETAMMUZ` unless Shabbos, then Sunday 18. |
| Av | 9 `TISHA_BEAV` unless Shabbos, then Sunday 10; 15 `TU_BEAV`. |
| Elul | 29 `EREV_ROSH_HASHANAH`. |
| Tishrei | 1-2 `ROSH_HASHANAH`; 3 `TZOM_GEDALIAH` unless Shabbos, then Sunday 4; 9 `EREV_YOM_KIPPUR`; 10 `YOM_KIPPUR`; 14 `EREV_SUKKOS`; 15 (and 16 outside Israel) `SUKKOS`; 17-20 (and 16 in Israel) `CHOL_HAMOED_SUKKOS`; 21 `HOSHANA_RABBAH`; 22 `SHEMINI_ATZERES`; 23 outside Israel `SIMCHAS_TORAH`; 23 in Israel / 24 outside `ISRU_CHAG`. |
| Kislev | 25-30 `CHANUKAH`. |
| Tevet | 1-2 `CHANUKAH`, and 3 in a deficient year; 10 `ASARAH_BETEVES`. |
| Shevat | 15 `TU_BISHVAT`. |
| Adar (common year) / Adar II | 13 `TAANIS_ESTHER` unless Friday or Shabbos, then Thursday 11; 14 `PURIM`; 15 `SHUSHAN_PURIM`. |
| Adar I (leap year) | 14 `PURIM_KATAN`; 15 `SHUSHAN_PURIM_KATAN`. |

Rosh Chodesh is the `isRoshChodesh` view (day 1 of any month but Tishrei, or day 30).

### The boolean views

| View | True when |
| --- | --- |
| `isYomTov` | Every holiday other than an eve, Isru Chag or a fast, with Yom Kippur included. Chol Hamoed, Pesach Sheni, Lag BaOmer, Tu BeAv, Tu BiShvat, Chanukah, Purim and Yom Kippur all count; the specific views answer the narrower questions. |
| `isYomTovAsurBemelacha` | `holiday.asurBemelacha`: Pesach, Shavuos, Sukkos, Shemini Atzeres, Simchas Torah, Rosh Hashanah, Yom Kippur. `isAsurBemelacha`: that or Shabbos. |
| `isErevYomTov` | An eve (`holiday.isErev`), Hoshana Rabbah, or 20 Nisan. `isErevYomTovSheni`: 1 Tishrei; outside Israel also 15 and 21 Nisan, 15 and 22 Tishrei, 6 Sivan. `hasHadlakasNeiros`: Friday or either eve. |
| `isTaanis` | `holiday.isTaanis`. `isTaanisBechoros`: 14 Nisan unless Shabbos, or Thursday 12 Nisan when 14 is Shabbos. |
| `isPesach` | Pesach or Chol Hamoed Pesach; `isSukkos`: Sukkos, Chol Hamoed Sukkos or Hoshana Rabbah; `isCholHamoedSukkos` includes Hoshana Rabbah; `isCholHamoed` is either. `isShavuos`, `isRoshHashanah`, `isYomKippur`, `isHoshanaRabbah`, `isShminiAtzeres`, `isSimchasTorah`, `isChanukah`, `isTishaBeav`, `isIsruChag`, `isCholHamoedPesach`: that holiday exactly. |
| `isPurim` | `PURIM`, or `SHUSHAN_PURIM` when `mukefChoma`. |
| `isAseresYemeiTeshuvah` | 1-10 Tishrei. `isErevRoshChodesh`: day 29 of any month but Elul. `isMacharChodesh`: Shabbos on the 29th or 30th. `isShabbosMevorchim`: Shabbos on 23-29 of any month but Elul. |
| `isYomKippurKatan` | In every month but Elul, Tishrei, Kislev and Nisan: the 29th, or Thursday 27 or 28 when the 29th is Friday or Shabbos. `isBehab`: in Cheshvan or Iyar, a Monday on 5-17 or a Thursday on 8-13. |
| `isBirkasHachamah` | A whole number of 28-year solar cycles (10227 days) from 2009-04-08. |

Counts: `sefirasHaomer` (16 Nisan is 1, 5 Sivan is 49, else null) and `dayOfChanukah` (1-8, else null).

### The parsha schedule

`parsha` is the weekly reading on a Shabbos (null on a weekday and on a Shabbos that is Yom Tov), `specialShabbos` the special reading (Shekalim, Zachor, Parah, Hachodesh, Hagadol, Chazon, Nachamu, Shuvah, Shirah) and `upcomingParsha` the reading of the next Shabbos that has one. `Parsha` lists the 54 readings in Torah order, then the seven doubles (`first` and `second` name their parts), then the special Shabbosos (`isSpecialShabbos`).

A Torah cycle runs from the Shabbos after Simchas Torah (22 Tishrei in Israel, 23 outside) to the Shabbos before the next Simchas Torah, skipping the Shabbosos of Pesach, Shavuos, Rosh Hashanah, Yom Kippur, Sukkos and Shemini Atzeres. The 53 readings from Bereishis to Haazinu (Vezos Haberachah is read on Simchas Torah) are assigned in order, one per Shabbos, and the doubles are combined only as the count requires:

- Up to Shavuos there must be 34 readings (through Bamidbar). When fewer Shabbosos are available, Vayakhel-Pekudei is combined if the Shabbosos before Pesach are fewer than 25, then Tazria-Metzora, Acharei Mos-Kedoshim and Behar-Bechukosai in that order. In a leap year the Shabbosos suffice and no spring double is needed (34 or 35 readings before Shavuos).
- Between Shavuos and Shabbos Chazon (the Shabbos on or before 9 Av) the readings must reach Devarim. Chukas-Balak is combined outside Israel when the second day of Shavuos is Shabbos; Matos-Masei whenever another double is still needed.
- Nitzavim-Vayelech is combined when only one Shabbos with a reading remains between the next Rosh Hashanah and Simchas Torah (Rosh Hashanah on Thursday or Shabbos), so that Haazinu is read on it.

`ParshaTest` checks over 5700-5900, in Israel and outside, that every cycle reads every parsha once in order, that the Shabbosos without a reading are exactly the Yom Tov Shabbosos, the anchored Shabbosos (Bereishis after Simchas Torah, Tzav or Metzora or Acharei Mos before Pesach, Bamidbar or Naso before Shavuos, Devarim before 9 Av, Nitzavim before Rosh Hashanah), the special Shabbosos and `upcomingParsha`.

### The tekufa

`daysFromTekufasTishrei` counts whole days from the autumn tekufa of the 365.25-day solar year; `TefillahRules` starts vesein tal umatar outside Israel when it reaches 47 ([`../tefillah/README.md`](../tefillah/README.md)).

### Daf Yomi

`bavliDaf` and `yerushalmiDaf` delegate to `DafYomi` ([`../daf_yomi/README.md`](../daf_yomi/README.md)).

## Tables

<!-- generated:month-table - written by docs/generate.py; to change it, edit the generator -->

| `HebrewMonth` | `number` |
| --- | ---: |
| `NISAN` | 1 |
| `IYAR` | 2 |
| `SIVAN` | 3 |
| `TAMMUZ` | 4 |
| `AV` | 5 |
| `ELUL` | 6 |
| `TISHREI` | 7 |
| `CHESHVAN` | 8 |
| `KISLEV` | 9 |
| `TEVET` | 10 |
| `SHEVAT` | 11 |
| `ADAR` | 12 |
| `ADAR_II` | 13 |

`YearType`: `DEFICIENT`, `REGULAR`, `COMPLETE`

<!-- /generated:month-table -->

<!-- generated:holiday-table - written by docs/generate.py; to change it, edit the generator -->

| `Holiday` | `isTaanis` | `isErev` | `asurBemelacha` |
| --- | --- | --- | --- |
| `EREV_PESACH` |  | yes |  |
| `PESACH` |  |  | yes |
| `CHOL_HAMOED_PESACH` |  |  |  |
| `PESACH_SHENI` |  |  |  |
| `EREV_SHAVUOS` |  | yes |  |
| `SHAVUOS` |  |  | yes |
| `SHIVA_ASAR_BETAMMUZ` | yes |  |  |
| `TISHA_BEAV` | yes |  |  |
| `TU_BEAV` |  |  |  |
| `EREV_ROSH_HASHANAH` |  | yes |  |
| `ROSH_HASHANAH` |  |  | yes |
| `TZOM_GEDALIAH` | yes |  |  |
| `EREV_YOM_KIPPUR` |  | yes |  |
| `YOM_KIPPUR` | yes |  | yes |
| `EREV_SUKKOS` |  | yes |  |
| `SUKKOS` |  |  | yes |
| `CHOL_HAMOED_SUKKOS` |  |  |  |
| `HOSHANA_RABBAH` |  |  |  |
| `SHEMINI_ATZERES` |  |  | yes |
| `SIMCHAS_TORAH` |  |  | yes |
| `CHANUKAH` |  |  |  |
| `ASARAH_BETEVES` | yes |  |  |
| `TU_BISHVAT` |  |  |  |
| `TAANIS_ESTHER` | yes |  |  |
| `PURIM` |  |  |  |
| `SHUSHAN_PURIM` |  |  |  |
| `PURIM_KATAN` |  |  |  |
| `SHUSHAN_PURIM_KATAN` |  |  |  |
| `LAG_BAOMER` |  |  |  |
| `ISRU_CHAG` |  |  |  |

<!-- /generated:holiday-table -->

<!-- generated:parsha-enum - written by docs/generate.py; to change it, edit the generator -->

`70` entries, in declaration order (`ordinal`):

| Ordinal | Entry | Combines | Special Shabbos |
| ---: | --- | --- | --- |
| 0 | `BERESHIT` |  |  |
| 1 | `NOACH` |  |  |
| 2 | `LECH_LECHA` |  |  |
| 3 | `VAYERA` |  |  |
| 4 | `CHAYEI_SARAH` |  |  |
| 5 | `TOLDOT` |  |  |
| 6 | `VAYETZE` |  |  |
| 7 | `VAYISHLACH` |  |  |
| 8 | `VAYESHEV` |  |  |
| 9 | `MIKETZ` |  |  |
| 10 | `VAYIGASH` |  |  |
| 11 | `VAYECHI` |  |  |
| 12 | `SHEMOT` |  |  |
| 13 | `VAERA` |  |  |
| 14 | `BO` |  |  |
| 15 | `BESHALACH` |  |  |
| 16 | `YITRO` |  |  |
| 17 | `MISHPATIM` |  |  |
| 18 | `TERUMAH` |  |  |
| 19 | `TETZAVEH` |  |  |
| 20 | `KI_TISA` |  |  |
| 21 | `VAYAKHEL` |  |  |
| 22 | `PEKUDEI` |  |  |
| 23 | `VAYIKRA` |  |  |
| 24 | `TZAV` |  |  |
| 25 | `SHEMINI` |  |  |
| 26 | `TAZRIA` |  |  |
| 27 | `METZORA` |  |  |
| 28 | `ACHAREI_MOT` |  |  |
| 29 | `KEDOSHIM` |  |  |
| 30 | `EMOR` |  |  |
| 31 | `BEHAR` |  |  |
| 32 | `BECHUKOTAI` |  |  |
| 33 | `BAMIDBAR` |  |  |
| 34 | `NASO` |  |  |
| 35 | `BEHAALOTCHA` |  |  |
| 36 | `SHELACH` |  |  |
| 37 | `KORACH` |  |  |
| 38 | `CHUKAT` |  |  |
| 39 | `BALAK` |  |  |
| 40 | `PINCHAS` |  |  |
| 41 | `MATOT` |  |  |
| 42 | `MASEI` |  |  |
| 43 | `DEVARIM` |  |  |
| 44 | `VAETCHANAN` |  |  |
| 45 | `EKEV` |  |  |
| 46 | `REEH` |  |  |
| 47 | `SHOFTIM` |  |  |
| 48 | `KI_TETZE` |  |  |
| 49 | `KI_TAVO` |  |  |
| 50 | `NITZAVIM` |  |  |
| 51 | `VAYELECH` |  |  |
| 52 | `HAAZINU` |  |  |
| 53 | `VEZOT_HABERACHAH` |  |  |
| 54 | `VAYAKHEL_PEKUDEI` | `VAYAKHEL` + `PEKUDEI` |  |
| 55 | `TAZRIA_METZORA` | `TAZRIA` + `METZORA` |  |
| 56 | `ACHAREI_MOT_KEDOSHIM` | `ACHAREI_MOT` + `KEDOSHIM` |  |
| 57 | `BEHAR_BECHUKOTAI` | `BEHAR` + `BECHUKOTAI` |  |
| 58 | `CHUKAT_BALAK` | `CHUKAT` + `BALAK` |  |
| 59 | `MATOT_MASEI` | `MATOT` + `MASEI` |  |
| 60 | `NITZAVIM_VAYELECH` | `NITZAVIM` + `VAYELECH` |  |
| 61 | `SHEKALIM` |  | yes |
| 62 | `ZACHOR` |  | yes |
| 63 | `PARAH` |  | yes |
| 64 | `HACHODESH` |  | yes |
| 65 | `SHUVAH` |  | yes |
| 66 | `SHIRAH` |  | yes |
| 67 | `HAGADOL` |  | yes |
| 68 | `CHAZON` |  | yes |
| 69 | `NACHAMU` |  | yes |

<!-- /generated:parsha-enum -->
