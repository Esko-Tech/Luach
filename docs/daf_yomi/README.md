# Daf Yomi

`DafYomi` (`src/commonMain/kotlin/com/eskotech/luach/calendar/DafYomi.kt`) computes the day's daf from the cycle arithmetic alone: `DafYomi.bavli(localDate)` (or `bavli(hebrewDay)`) returns a `BavliDaf(tractate, page)` and `DafYomi.yerushalmi(hebrewDay)` a `YerushalmiDaf?`. `HebrewDay.bavliDaf` and `yerushalmiDaf` are the convenient accessors. The tractates are the enums `BavliTractate` (with `hebrewName`, `firstPage`, `lastPage`, `pages`) and `YerushalmiTractate` (with `hebrewName`, `pages`). Hand-authored; the tractate tables are generated from the enums.

## Bavli

- Cycle 1 began on **1923-09-11** (`DafYomi.BAVLI_FIRST_CYCLE`). Cycles 1-7 are 2702 days long with 12 pages of Shekalim (2-13); from cycle 8, which began on **1975-06-24** (`BAVLI_EIGHTH_CYCLE`), a cycle is 2711 days with 21 pages of Shekalim (2-22). The code keeps both anchors and picks by date.
- The number of days since the anchor, modulo the cycle length, is walked through the tractates in order, each contributing its `pages`; the daf is `firstPage + remainder`. Every tractate starts at page 2 except Kinnim, Tamid and Middos, which continue Meilah's numbering from 23, 26 and 34.
- Dates before 1923-09-11 throw `IllegalArgumentException`.

## Yerushalmi

- The cycle began on **1980-02-02** (`YERUSHALMI_FIRST_CYCLE`) and has **1554** pages (`YerushalmiTractate.PAGES_PER_CYCLE`).
- Yom Kippur and Tisha BeAv as observed (Sunday 10 Av when 9 Av is Shabbos) have no daf: those days return `null`, the count of pages learned since the cycle start skips them, and the sequence resumes the next day. Each cycle therefore lasts 1554 days plus the number of those fast days in it.
- Dates before 1980-02-02 throw `IllegalArgumentException`; a fast day before that date still returns null.
- `DafYomi.yerushalmi(localDate)` is a shortcut that reads the date outside Israel; the fast days involved are the same in Israel and outside.

`DafYomiTest` re-derives both schedules from the cycle dates and the page counts and checks every day since 1975 (Bavli, plus the 12-page Shekalim cycles before) and since 1980 (Yerushalmi), published cycle start dates, and the names; `DafYomiSnapshotTest` fingerprints every day to 2100.

## Tractates

<!-- generated:masechtos - written by docs/generate.py; to change it, edit the generator -->

#### Bavli (2711 pages per cycle since the 8th cycle)

| # | `BavliTractate` | Hebrew | First page | Last page | Pages in cycle | Note |
| ---: | --- | --- | ---: | ---: | ---: | --- |
| 0 | `BERACHOS` | ברכות | 2 | 64 | 63 |  |
| 1 | `SHABBOS` | שבת | 2 | 157 | 156 |  |
| 2 | `ERUVIN` | עירובין | 2 | 105 | 104 |  |
| 3 | `PESACHIM` | פסחים | 2 | 121 | 120 |  |
| 4 | `SHEKALIM` | שקלים | 2 | 22 | 21 | 12 pages before the 8th cycle |
| 5 | `YOMA` | יומא | 2 | 88 | 87 |  |
| 6 | `SUKKAH` | סוכה | 2 | 56 | 55 |  |
| 7 | `BEITZAH` | ביצה | 2 | 40 | 39 |  |
| 8 | `ROSH_HASHANAH` | ראש השנה | 2 | 35 | 34 |  |
| 9 | `TAANIS` | תענית | 2 | 31 | 30 |  |
| 10 | `MEGILLAH` | מגילה | 2 | 32 | 31 |  |
| 11 | `MOED_KATAN` | מועד קטן | 2 | 29 | 28 |  |
| 12 | `CHAGIGAH` | חגיגה | 2 | 27 | 26 |  |
| 13 | `YEVAMOS` | יבמות | 2 | 122 | 121 |  |
| 14 | `KESUBOS` | כתובות | 2 | 112 | 111 |  |
| 15 | `NEDARIM` | נדרים | 2 | 91 | 90 |  |
| 16 | `NAZIR` | נזיר | 2 | 66 | 65 |  |
| 17 | `SOTAH` | סוטה | 2 | 49 | 48 |  |
| 18 | `GITTIN` | גיטין | 2 | 90 | 89 |  |
| 19 | `KIDDUSHIN` | קידושין | 2 | 82 | 81 |  |
| 20 | `BAVA_KAMMA` | בבא קמא | 2 | 119 | 118 |  |
| 21 | `BAVA_METZIA` | בבא מציעא | 2 | 119 | 118 |  |
| 22 | `BAVA_BASRA` | בבא בתרא | 2 | 176 | 175 |  |
| 23 | `SANHEDRIN` | סנהדרין | 2 | 113 | 112 |  |
| 24 | `MAKKOS` | מכות | 2 | 24 | 23 |  |
| 25 | `SHEVUOS` | שבועות | 2 | 49 | 48 |  |
| 26 | `AVODAH_ZARAH` | עבודה זרה | 2 | 76 | 75 |  |
| 27 | `HORAYOS` | הוריות | 2 | 14 | 13 |  |
| 28 | `ZEVACHIM` | זבחים | 2 | 120 | 119 |  |
| 29 | `MENACHOS` | מנחות | 2 | 110 | 109 |  |
| 30 | `CHULLIN` | חולין | 2 | 142 | 141 |  |
| 31 | `BECHOROS` | בכורות | 2 | 61 | 60 |  |
| 32 | `ARACHIN` | ערכין | 2 | 34 | 33 |  |
| 33 | `TEMURAH` | תמורה | 2 | 34 | 33 |  |
| 34 | `KEREISOS` | כריתות | 2 | 28 | 27 |  |
| 35 | `MEILAH` | מעילה | 2 | 22 | 21 |  |
| 36 | `KINNIM` | קינים | 23 | 25 | 3 |  |
| 37 | `TAMID` | תמיד | 26 | 33 | 8 |  |
| 38 | `MIDDOS` | מידות | 34 | 37 | 4 |  |
| 39 | `NIDDAH` | נדה | 2 | 73 | 72 |  |

#### Yerushalmi (1554 pages per cycle)

| # | `YerushalmiTractate` | Hebrew | Pages in cycle |
| ---: | --- | --- | ---: |
| 0 | `BERACHOS` | ברכות | 68 |
| 1 | `PEAH` | פיאה | 37 |
| 2 | `DEMAI` | דמאי | 34 |
| 3 | `KILAYIM` | כלאים | 44 |
| 4 | `SHEVIIS` | שביעית | 31 |
| 5 | `TERUMOS` | תרומות | 59 |
| 6 | `MAASROS` | מעשרות | 26 |
| 7 | `MAASER_SHENI` | מעשר שני | 33 |
| 8 | `CHALLAH` | חלה | 28 |
| 9 | `ORLAH` | ערלה | 20 |
| 10 | `BIKKURIM` | ביכורים | 13 |
| 11 | `SHABBOS` | שבת | 92 |
| 12 | `ERUVIN` | עירובין | 65 |
| 13 | `PESACHIM` | פסחים | 71 |
| 14 | `BEITZAH` | ביצה | 22 |
| 15 | `ROSH_HASHANAH` | ראש השנה | 22 |
| 16 | `YOMA` | יומא | 42 |
| 17 | `SUKKAH` | סוכה | 26 |
| 18 | `TAANIS` | תענית | 26 |
| 19 | `SHEKALIM` | שקלים | 33 |
| 20 | `MEGILLAH` | מגילה | 34 |
| 21 | `CHAGIGAH` | חגיגה | 22 |
| 22 | `MOED_KATAN` | מועד קטן | 19 |
| 23 | `YEVAMOS` | יבמות | 85 |
| 24 | `KESUBOS` | כתובות | 72 |
| 25 | `SOTAH` | סוטה | 47 |
| 26 | `NEDARIM` | נדרים | 40 |
| 27 | `NAZIR` | נזיר | 47 |
| 28 | `GITTIN` | גיטין | 54 |
| 29 | `KIDDUSHIN` | קידושין | 48 |
| 30 | `BAVA_KAMMA` | בבא קמא | 44 |
| 31 | `BAVA_METZIA` | בבא מציעא | 37 |
| 32 | `BAVA_BASRA` | בבא בתרא | 34 |
| 33 | `SHEVUOS` | שבועות | 44 |
| 34 | `MAKKOS` | מכות | 9 |
| 35 | `SANHEDRIN` | סנהדרין | 57 |
| 36 | `AVODAH_ZARAH` | עבודה זרה | 37 |
| 37 | `HORAYOS` | הוריות | 19 |
| 38 | `NIDDAH` | נדה | 13 |

<!-- /generated:masechtos -->
