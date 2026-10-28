# Tefillah rules

`TefillahRules` (`src/commonMain/kotlin/com/eskotech/luach/calendar/Tefillah.kt`) answers "is X said today?" for a `HebrewDay`. It holds a `TachanunCustoms` value, a data class of twelve switches for the customs that differ between communities; everything else comes from the day and its `inIsrael` setting. Hand-authored; the customs table is generated.

## Customs

<!-- generated:tefillah-customs - written by docs/generate.py; to change it, edit the generator -->

| Custom | Default |
| --- | --- |
| `omitAfterSukkos` | `false` |
| `omitWeekAfterShavuos` | `true` |
| `omit13SivanOutsideIsrael` | `false` |
| `omitPesachSheni` | `true` |
| `omit15IyarOutsideIsrael` | `false` |
| `omitMinchaErevLagBaomer` | `true` |
| `omitShivasYemeiHamiluim` | `false` |
| `omitWeekOfHod` | `false` |
| `omitWeekOfPurim` | `false` |
| `omitFridays` | `false` |
| `omitSundays` | `false` |
| `omitMincha` | `false` |

<!-- /generated:tefillah-customs -->

Every switch names a case in which tachanun is **omitted**; `TachanunCustoms()` is the default set, and `TachanunCustoms(omitFridays = true)` or `customs.copy(omitMincha = true)` changes one.

## Tachanun

`tachanunAtShacharis(day)` is **false** (no tachanun) on:

- Shabbos; Sunday with `omitSundays`; Friday with `omitFridays`.
- The whole of Nisan.
- Tishrei 9-21, or Tishrei from the 9th to the end of the month with `omitAfterSukkos`.
- Sivan 1-6 without `omitWeekAfterShavuos`; with it, 1-12, or 1-13 outside Israel with `omit13SivanOutsideIsrael`.
- Any `isYomTov` day that is not a fast (see [`../calendar/README.md`](../calendar/README.md#the-boolean-views) for how wide `isYomTov` is: Chol Hamoed, Chanukah, Purim, Lag BaOmer and more), except Pesach Sheni without `omitPesachSheni`.
- 15 Iyar outside Israel with `omitPesachSheni` and `omit15IyarOutsideIsrael`.
- Erev Rosh Hashanah, Tisha BeAv, Isru Chag, Rosh Chodesh.
- Adar (Adar II) 23-29 with `omitShivasYemeiHamiluim`; 11-17 with `omitWeekOfPurim`.
- Iyar 14-20 with `omitWeekOfHod`.

`tachanunAtMincha(day)` is false with `omitMincha`, on Friday, when shacharis has no tachanun, and on the day **before** a day without tachanun - except before Erev Rosh Hashanah, Erev Yom Kippur, Pesach Sheni and Lag BaOmer, where mincha keeps tachanun; the eve of Lag BaOmer drops it only with `omitMinchaErevLagBaomer` (the default).

## Vesein tal umatar

| Function | Rule |
| --- | --- |
| `isVeseinTalUmatarStart` | In Israel: 7 Cheshvan. Outside: the day `daysFromTekufasTishrei` reaches 47, unless that is Shabbos (then Sunday, where the count is 47 or 48). |
| `veseinTalUmatarStartsTonight` | The day before: 6 Cheshvan in Israel; outside, count 46 unless that is Friday (then Shabbos, count 46 or 47). |
| `veseinTalUmatar` | 1-14 Nisan; then again from 7 Cheshvan in Israel, or from count 47 outside, through 14 Nisan. |
| `veseinBerachah` | The complement. |

## Mashiv haruach and morid hatal

- `isMashivHaruachStart`: 22 Tishrei (Shemini Atzeres). `isMashivHaruachEnd`: 15 Nisan.
- `mashivHaruach`: strictly between those dates. `moridHatal`: the rest of the year, including both boundary days (the change happens at musaf).

## Hallel, al hanissim, yaaleh veyavo

- `hallel(day)` returns `Hallel.FULL`, `Hallel.PARTIAL` or `Hallel.NONE`. Hallel is said on Rosh Chodesh, Chanukah, 15-21 Nisan (15-22 outside Israel), 6 Sivan (and 7 outside Israel), 15-22 Tishrei (15-23 outside Israel); it is partial on Rosh Chodesh (unless it is also Chanukah) and on Pesach after the first day (after the second day outside Israel).
- `alHanissim`: `isPurim` (so Shushan Purim for a walled city) or Chanukah.
- `yaalehVeyavo`: Pesach, Shavuos, Rosh Hashanah, Yom Kippur, Sukkos, Shemini Atzeres, Simchas Torah or Rosh Chodesh (the `is*` views, so Chol Hamoed and Hoshana Rabbah are included).

`TefillahRulesTest` re-derives every rule for every day of 5770-5790 from the holiday table and checks the published vesein tal umatar start dates and each custom; `TefillahSnapshotTest` fingerprints every rule for every day from 1990 to 2060 under every custom.

## The whole day in one call

`all(day)` returns a `TefillahSummary`, a data class with one field per rule above, named exactly like the function it comes from: `tachanunAtShacharis`, `tachanunAtMincha`, `veseinTalUmatar`, `veseinBerachah`, `isVeseinTalUmatarStart`, `veseinTalUmatarStartsTonight`, `mashivHaruach`, `moridHatal`, `isMashivHaruachStart`, `isMashivHaruachEnd`, `hallel`, `alHanissim` and `yaalehVeyavo`. It is the tefillah counterpart of `Zmanim.all()`: one call fills a screen. `TefillahRulesTest.summaryMatchesTheIndividualRules` checks every field against the individual call for every day of 5770-5790 under two sets of customs.
