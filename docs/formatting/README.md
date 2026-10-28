# Hebrew date formatting

`LuachFormatter` (`src/commonMain/kotlin/com/eskotech/luach/format/LuachFormatter.kt`) renders dates, months, weekdays, holidays, parshios, dafim, numbers, zman names and times of day as text, in Hebrew or in transliteration. It is a data class of settings: build one with the defaults, or with named arguments, and `copy` it to change a setting; one formatter formats any number of dates. Hand-authored; the settings table is generated.

## Settings

<!-- generated:formatter-settings - written by docs/generate.py; to change it, edit the generator -->

| Setting | Type | Default |
| --- | --- | --- |
| `hebrew` | `Boolean` | `false` |
| `quoteMarks` | `Boolean` | `true` |
| `thousands` | `Boolean` | `false` |
| `finalLetters` | `Boolean` | `false` |
| `abbreviatedWeekdays` | `Boolean` | `false` |
| `monthNames` | `Map<HebrewMonth, String>` | `Transliterations.MONTHS` |
| `adarInLeapYear` | `String` | `"Adar I"` |
| `weekdayNames` | `Map<DayOfWeek, String>` | `Transliterations.WEEKDAYS` |
| `holidayNames` | `Map<Holiday, String>` | `Transliterations.HOLIDAYS` |
| `roshChodeshName` | `String` | `"Rosh Chodesh"` |
| `omerFormat` | `String` | `"Omer {day}"` |
| `omerPrefix` | `String` | `"ב"` |
| `parshaNames` | `Map<Parsha, String>` | `Transliterations.PARSHIOS` |
| `bavliNames` | `Map<BavliTractate, String>` | `Transliterations.BAVLI` |
| `yerushalmiNames` | `Map<YerushalmiTractate, String>` | `Transliterations.YERUSHALMI` |
| `noDafName` | `String` | `"No Daf Today"` |
| `zmanNames` | `Map<Zman, String>` | `Transliterations.ZMANIM` |
| `twelveHourClock` | `Boolean` | `false` |
| `amPmNames` | `Pair<String, String>` | `"AM" to "PM"` |

<!-- /generated:formatter-settings -->

- `hebrew` selects Hebrew output for every function; the Hebrew names are fixed, the transliterated ones come from the name maps.
- `quoteMarks` adds ׳ (geresh) after a one-letter number and ״ (gershayim) before the last letter of a longer one; it also decides the geresh after `אדר א` and `אדר ב`.
- `thousands` prefixes the thousands of a year: `ה׳ תשפ״ד` for 5784.
- `finalLetters` writes a round ten that ends a number of more than one letter with its final form (`תש״ף` for 5780).
- `abbreviatedWeekdays` gives `Sun`..`Sha` or `א׳`..`ש׳`.
- The name maps (`monthNames`, `weekdayNames`, `holidayNames`, `parshaNames`, `bavliNames`, `yerushalmiNames`), `adarInLeapYear`, `roshChodeshName`, `omerFormat` (with `{day}` as the placeholder), `omerPrefix` and `noDafName` are the transliterations an application can replace; the defaults live in the `Transliterations` object.
- `zmanNames` names the `Zman` entries in transliteration; `twelveHourClock` switches `formatTime` from `18:30` to `6:30 PM`, and `amPmNames` supplies the two suffixes (`"AM" to "PM"` by default; empty strings drop the suffix, `"לפנה״צ" to "אחה״צ"` gives Hebrew ones).

## Functions

| Function | Output |
| --- | --- |
| `formatDate(date)` | `י״ד ניסן תשפ״ד` or `14 Nissan, 5784`. |
| `formatMonth(date)`, `formatMonth(month, meuberes)` | The month name; in a leap year `ADAR` is `אדר א׳` / `Adar I` and `ADAR_II` is `אדר ב׳` / `Adar II`. |
| `formatWeekday(date)`, `formatWeekday(dayOfWeek)` | `ראשון`..`שבת`, `Sunday`..`Shabbos`, or the abbreviations. |
| `formatHoliday(day)` | The holiday name, with the day for Chanukah (`ג׳ חנוכה` / `Chanukah 3`); empty for an ordinary day. |
| `formatRoshChodesh(day)` | `ראש חודש אייר` / `Rosh Chodesh Iyar`, naming the month that begins (on a 30th, the following month); empty on other days. |
| `formatOmer(day)` | `ל״ג בעומר` / `Omer 33`; in transliteration day 33 is the Lag BaOmer holiday name; empty outside the omer. |
| `formatParsha(day)`, `formatParsha(parsha)`, `formatSpecialShabbos(day)` | The parsha name; empty on a weekday. Doubles are joined with a hyphen (`Vayakhel-Pekudei`). |
| `formatDaf(bavliDaf)`, `formatDaf(yerushalmiDaf?)` | `ברכות ב׳` / `Berachos 2`; a null Yerushalmi daf prints `אין דף היום` / `No Daf Today`. |
| `formatZman(zman)` | The name of a `Zman`: `עלות השחר` / `Alos Hashachar`, `סוף זמן קריאת שמע` / `Sof Zman Krias Shema`, from `zmanNames` in transliteration. |
| `formatTime(instant, timeZone)`, `formatTime(instant, location)` | The local wall-clock time of an `Instant?`: `18:30`, or `6:30 PM` with `twelveHourClock`; `seconds = true` appends `:59`; the time is truncated; a null instant gives an empty string. |
| `formatNumber(number)` | Hebrew numerals for `0..9999`; anything else throws `IllegalArgumentException`. |
| `formatYearSignature(year)` | The three-letter signature of a year: the weekday of Rosh Hashanah, `ח`/`כ`/`ש` for a deficient, regular or complete year, and the weekday of Pesach, without marks. |

## Hebrew numerals

1. `0` is `אפס`.
2. An exact multiple of 1000 is the thousands digit plus ` אלפים` (`ה׳ אלפים`).
3. Otherwise the thousands are dropped unless `thousands` is set (then `ה׳ ` is prefixed); the hundreds use `ק ר ש ת תק תר תש תת תתק`, 15 and 16 are `טו` and `טז`, and tens and ones follow.
4. A round ten after a hundreds letter uses its final form when `finalLetters` is set (`ק״ך` for 120, `תש״ם` for 5740).
5. With `quoteMarks`: a one-letter number gets a geresh after it; any other number gets a gershayim before its last letter.

`LuachFormatterTest` pins about forty numbers and every list; `FormatterSnapshotTest` fingerprints every format of every day from 1900 to 2100 under every setting.
