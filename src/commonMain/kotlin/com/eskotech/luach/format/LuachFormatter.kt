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

package com.eskotech.luach.format

import com.eskotech.luach.calendar.BavliDaf
import com.eskotech.luach.calendar.BavliTractate
import com.eskotech.luach.calendar.HebrewDate
import com.eskotech.luach.calendar.HebrewDay
import com.eskotech.luach.calendar.HebrewMonth
import com.eskotech.luach.calendar.Holiday
import com.eskotech.luach.calendar.Parsha
import com.eskotech.luach.calendar.YearType
import com.eskotech.luach.calendar.YerushalmiDaf
import com.eskotech.luach.calendar.YerushalmiTractate
import com.eskotech.luach.geo.Location
import com.eskotech.luach.zmanim.Zman
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class LuachFormatter(
    val hebrew: Boolean = false,
    val quoteMarks: Boolean = true,
    val thousands: Boolean = false,
    val finalLetters: Boolean = false,
    val abbreviatedWeekdays: Boolean = false,
    val monthNames: Map<HebrewMonth, String> = Transliterations.MONTHS,
    val adarInLeapYear: String = "Adar I",
    val weekdayNames: Map<DayOfWeek, String> = Transliterations.WEEKDAYS,
    val holidayNames: Map<Holiday, String> = Transliterations.HOLIDAYS,
    val roshChodeshName: String = "Rosh Chodesh",
    val omerFormat: String = "Omer {day}",
    val omerPrefix: String = "ב",
    val parshaNames: Map<Parsha, String> = Transliterations.PARSHIOS,
    val bavliNames: Map<BavliTractate, String> = Transliterations.BAVLI,
    val yerushalmiNames: Map<YerushalmiTractate, String> = Transliterations.YERUSHALMI,
    val noDafName: String = "No Daf Today",
    val zmanNames: Map<Zman, String> = Transliterations.ZMANIM,
    val twelveHourClock: Boolean = false,
    val amPmNames: Pair<String, String> = "AM" to "PM",
) {

    fun formatDate(date: HebrewDate): String =
        if (hebrew) "${formatNumber(date.day)} ${formatMonth(date)} ${formatNumber(date.year)}"
        else "${date.day} ${formatMonth(date)}, ${date.year}"

    fun formatMonth(date: HebrewDate): String = formatMonth(date.month, date.isMeuberes)

    fun formatMonth(month: HebrewMonth, meuberes: Boolean): String {
        if (hebrew) {
            val name = HebrewNames.MONTHS.getValue(month)
            return when {
                meuberes && month == HebrewMonth.ADAR -> HebrewNames.ADAR_I + mark(SINGLE_MARK)
                month == HebrewMonth.ADAR_II -> name + mark(SINGLE_MARK)
                else -> name
            }
        }
        if (meuberes && month == HebrewMonth.ADAR) {
            return adarInLeapYear
        }
        return monthNames.getValue(month)
    }

    fun formatWeekday(date: HebrewDate): String = formatWeekday(date.dayOfWeek)

    fun formatWeekday(dayOfWeek: DayOfWeek): String {
        if (hebrew) {
            if (!abbreviatedWeekdays) {
                return HebrewNames.WEEKDAYS.getValue(dayOfWeek)
            }
            return formatNumber(if (dayOfWeek == DayOfWeek.SATURDAY) 300 else weekdayNumber(dayOfWeek))
        }
        val name = weekdayNames.getValue(dayOfWeek)
        return if (abbreviatedWeekdays) name.take(3) else name
    }

    fun formatHoliday(day: HebrewDay): String {
        val holiday = day.holiday ?: return ""
        val name = if (hebrew) HebrewNames.HOLIDAYS.getValue(holiday) else holidayNames.getValue(holiday)
        if (holiday == Holiday.CHANUKAH) {
            val dayOfChanukah = day.dayOfChanukah ?: return name
            return if (hebrew) "${formatNumber(dayOfChanukah)} $name" else "$name $dayOfChanukah"
        }
        return name
    }

    fun formatRoshChodesh(day: HebrewDay): String {
        if (!day.isRoshChodesh) {
            return ""
        }
        val month = if (day.dayOfMonth == 30) day.date.plusDays(1).month else day.month
        val label = if (hebrew) HebrewNames.ROSH_CHODESH else roshChodeshName
        return "$label ${formatMonth(month, day.date.isMeuberes)}"
    }

    fun formatOmer(day: HebrewDay): String {
        val omer = day.sefirasHaomer ?: return ""
        if (hebrew) {
            return "${formatNumber(omer)} $omerPrefix${HebrewNames.OMER}"
        }
        if (omer == 33) {
            return holidayNames.getValue(Holiday.LAG_BAOMER)
        }
        return omerFormat.replace("{day}", omer.toString())
    }

    fun formatParsha(day: HebrewDay): String = day.parsha?.let { formatParsha(it) } ?: ""

    fun formatSpecialShabbos(day: HebrewDay): String = day.specialShabbos?.let { formatParsha(it) } ?: ""

    fun formatParsha(parsha: Parsha): String {
        if (hebrew) {
            return HebrewNames.PARSHIOS.getValue(parsha)
        }
        return parshaNames.getValue(parsha)
    }

    fun formatDaf(daf: BavliDaf): String =
        if (hebrew) "${daf.tractate.hebrewName} ${formatNumber(daf.page)}"
        else "${bavliNames.getValue(daf.tractate)} ${daf.page}"

    fun formatDaf(daf: YerushalmiDaf?): String {
        if (daf == null) {
            return if (hebrew) HebrewNames.NO_DAF else noDafName
        }
        return if (hebrew) "${daf.tractate.hebrewName} ${formatNumber(daf.page)}"
        else "${yerushalmiNames.getValue(daf.tractate)} ${daf.page}"
    }

    fun formatYearSignature(year: Int): String {
        val roshHashanah = HebrewDate.of(year, HebrewMonth.TISHREI, 1)
        val pesach = HebrewDate.of(year, HebrewMonth.NISAN, 15)
        val yearType = when (HebrewDate.yearType(year)) {
            YearType.DEFICIENT -> "ח"
            YearType.REGULAR -> "כ"
            YearType.COMPLETE -> "ש"
        }
        return HebrewNumerals.letter(weekdayNumber(roshHashanah.dayOfWeek)) + yearType + HebrewNumerals.letter(weekdayNumber(pesach.dayOfWeek))
    }

    fun formatZman(zman: Zman): String = if (hebrew) HebrewNames.ZMANIM.getValue(zman) else zmanNames.getValue(zman)

    fun formatTime(instant: Instant?, location: Location, seconds: Boolean = false): String = formatTime(instant, location.timeZone, seconds)

    fun formatTime(instant: Instant?, timeZone: TimeZone, seconds: Boolean = false): String {
        val time = instant?.toLocalDateTime(timeZone)?.time ?: return ""
        val minutes = time.minute.toString().padStart(2, '0')
        val secondsPart = if (seconds) ":" + time.second.toString().padStart(2, '0') else ""
        if (!twelveHourClock) {
            return "${time.hour.toString().padStart(2, '0')}:$minutes$secondsPart"
        }
        val hour = (time.hour + 11) % 12 + 1
        val suffix = if (time.hour < 12) amPmNames.first else amPmNames.second
        val text = "$hour:$minutes$secondsPart"
        return if (suffix.isEmpty()) text else "$text $suffix"
    }

    fun formatNumber(number: Int): String {
        require(number in 0..9999) { "$number cannot be written in Hebrew numerals; the range is 0..9999" }
        if (number == 0) {
            return HebrewNumerals.ZERO
        }
        val thousandsDigit = number / 1000
        val remainder = number % 1000
        if (remainder == 0) {
            return HebrewNumerals.letter(thousandsDigit) + mark(SINGLE_MARK) + " " + HebrewNumerals.THOUSANDS
        }
        val prefix = if (thousands && thousandsDigit > 0) HebrewNumerals.letter(thousandsDigit) + mark(SINGLE_MARK) + " " else ""
        val letters = HebrewNumerals.letters(remainder, finalLetters)
        if (!quoteMarks) {
            return prefix + letters
        }
        return prefix + if (letters.length == 1) letters + SINGLE_MARK else letters.dropLast(1) + DOUBLE_MARK + letters.last()
    }

    private fun mark(mark: String): String = if (quoteMarks) mark else ""

    private fun weekdayNumber(dayOfWeek: DayOfWeek): Int = (dayOfWeek.ordinal + 1) % 7 + 1

    private companion object {
        const val SINGLE_MARK = "׳"

        const val DOUBLE_MARK = "״"
    }
}

internal object HebrewNumerals {
    const val ZERO = "אפס"

    const val THOUSANDS = "אלפים"

    private val ONES = listOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")

    private val TENS = listOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")

    private val FINAL_TENS = listOf("", "י", "ך", "ל", "ם", "ן", "ס", "ע", "ף", "ץ")

    private val HUNDREDS = listOf("", "ק", "ר", "ש", "ת", "תק", "תר", "תש", "תת", "תתק")

    fun letter(digit: Int): String = ONES[digit]

    fun letters(number: Int, finalLetters: Boolean): String {
        val hundreds = number / 100
        val tens = number % 100 / 10
        val ones = number % 10
        val text = StringBuilder(HUNDREDS[hundreds])
        when {
            tens == 1 && ones == 5 -> text.append("טו")
            tens == 1 && ones == 6 -> text.append("טז")
            else -> {
                val endsWithTen = ones == 0 && tens > 0 && hundreds > 0
                text.append(if (finalLetters && endsWithTen) FINAL_TENS[tens] else TENS[tens])
                text.append(ONES[ones])
            }
        }
        return text.toString()
    }
}

internal object HebrewNames {
    const val ROSH_CHODESH = "ראש חודש"

    const val OMER = "עומר"

    const val NO_DAF = "אין דף היום"

    const val ADAR_I = "אדר א"

    val MONTHS: Map<HebrewMonth, String> = mapOf(
        HebrewMonth.NISAN to "ניסן", HebrewMonth.IYAR to "אייר", HebrewMonth.SIVAN to "סיון", HebrewMonth.TAMMUZ to "תמוז",
        HebrewMonth.AV to "אב", HebrewMonth.ELUL to "אלול", HebrewMonth.TISHREI to "תשרי", HebrewMonth.CHESHVAN to "חשון",
        HebrewMonth.KISLEV to "כסלו", HebrewMonth.TEVET to "טבת", HebrewMonth.SHEVAT to "שבט", HebrewMonth.ADAR to "אדר",
        HebrewMonth.ADAR_II to "אדר ב",
    )

    val WEEKDAYS: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.SUNDAY to "ראשון", DayOfWeek.MONDAY to "שני", DayOfWeek.TUESDAY to "שלישי", DayOfWeek.WEDNESDAY to "רביעי",
        DayOfWeek.THURSDAY to "חמישי", DayOfWeek.FRIDAY to "שישי", DayOfWeek.SATURDAY to "שבת",
    )

    val HOLIDAYS: Map<Holiday, String> = mapOf(
        Holiday.EREV_PESACH to "ערב פסח", Holiday.PESACH to "פסח", Holiday.CHOL_HAMOED_PESACH to "חול המועד פסח",
        Holiday.PESACH_SHENI to "פסח שני", Holiday.EREV_SHAVUOS to "ערב שבועות", Holiday.SHAVUOS to "שבועות",
        Holiday.SHIVA_ASAR_BETAMMUZ to "שבעה עשר בתמוז", Holiday.TISHA_BEAV to "תשעה באב", Holiday.TU_BEAV to "ט״ו באב",
        Holiday.EREV_ROSH_HASHANAH to "ערב ראש השנה", Holiday.ROSH_HASHANAH to "ראש השנה", Holiday.TZOM_GEDALIAH to "צום גדליה",
        Holiday.EREV_YOM_KIPPUR to "ערב יום כיפור", Holiday.YOM_KIPPUR to "יום כיפור", Holiday.EREV_SUKKOS to "ערב סוכות",
        Holiday.SUKKOS to "סוכות", Holiday.CHOL_HAMOED_SUKKOS to "חול המועד סוכות", Holiday.HOSHANA_RABBAH to "הושענא רבה",
        Holiday.SHEMINI_ATZERES to "שמיני עצרת", Holiday.SIMCHAS_TORAH to "שמחת תורה", Holiday.CHANUKAH to "חנוכה",
        Holiday.ASARAH_BETEVES to "עשרה בטבת", Holiday.TU_BISHVAT to "ט״ו בשבט", Holiday.TAANIS_ESTHER to "תענית אסתר",
        Holiday.PURIM to "פורים", Holiday.SHUSHAN_PURIM to "שושן פורים", Holiday.PURIM_KATAN to "פורים קטן",
        Holiday.SHUSHAN_PURIM_KATAN to "שושן פורים קטן", Holiday.LAG_BAOMER to "ל״ג בעומר",
        Holiday.ISRU_CHAG to "אסרו חג",
    )

    val PARSHIOS: Map<Parsha, String> = mapOf(
        Parsha.BERESHIT to "בראשית", Parsha.NOACH to "נח", Parsha.LECH_LECHA to "לך לך", Parsha.VAYERA to "וירא",
        Parsha.CHAYEI_SARAH to "חיי שרה", Parsha.TOLDOT to "תולדות", Parsha.VAYETZE to "ויצא", Parsha.VAYISHLACH to "וישלח",
        Parsha.VAYESHEV to "וישב", Parsha.MIKETZ to "מקץ", Parsha.VAYIGASH to "ויגש", Parsha.VAYECHI to "ויחי",
        Parsha.SHEMOT to "שמות", Parsha.VAERA to "וארא", Parsha.BO to "בא", Parsha.BESHALACH to "בשלח", Parsha.YITRO to "יתרו",
        Parsha.MISHPATIM to "משפטים", Parsha.TERUMAH to "תרומה", Parsha.TETZAVEH to "תצוה", Parsha.KI_TISA to "כי תשא",
        Parsha.VAYAKHEL to "ויקהל", Parsha.PEKUDEI to "פקודי", Parsha.VAYIKRA to "ויקרא", Parsha.TZAV to "צו",
        Parsha.SHEMINI to "שמיני", Parsha.TAZRIA to "תזריע", Parsha.METZORA to "מצורע", Parsha.ACHAREI_MOT to "אחרי מות",
        Parsha.KEDOSHIM to "קדושים", Parsha.EMOR to "אמור", Parsha.BEHAR to "בהר", Parsha.BECHUKOTAI to "בחוקותי",
        Parsha.BAMIDBAR to "במדבר", Parsha.NASO to "נשא", Parsha.BEHAALOTCHA to "בהעלותך", Parsha.SHELACH to "שלח",
        Parsha.KORACH to "קרח", Parsha.CHUKAT to "חוקת", Parsha.BALAK to "בלק", Parsha.PINCHAS to "פינחס",
        Parsha.MATOT to "מטות", Parsha.MASEI to "מסעי", Parsha.DEVARIM to "דברים", Parsha.VAETCHANAN to "ואתחנן",
        Parsha.EKEV to "עקב", Parsha.REEH to "ראה", Parsha.SHOFTIM to "שופטים", Parsha.KI_TETZE to "כי תצא",
        Parsha.KI_TAVO to "כי תבוא", Parsha.NITZAVIM to "ניצבים", Parsha.VAYELECH to "וילך", Parsha.HAAZINU to "האזינו",
        Parsha.VEZOT_HABERACHAH to "וזאת הברכה", Parsha.VAYAKHEL_PEKUDEI to "ויקהל-פקודי", Parsha.TAZRIA_METZORA to "תזריע-מצורע",
        Parsha.ACHAREI_MOT_KEDOSHIM to "אחרי מות-קדושים", Parsha.BEHAR_BECHUKOTAI to "בהר-בחוקותי", Parsha.CHUKAT_BALAK to "חוקת-בלק",
        Parsha.MATOT_MASEI to "מטות-מסעי", Parsha.NITZAVIM_VAYELECH to "ניצבים-וילך", Parsha.SHEKALIM to "שקלים",
        Parsha.ZACHOR to "זכור", Parsha.PARAH to "פרה", Parsha.HACHODESH to "החודש", Parsha.SHUVAH to "שובה",
        Parsha.SHIRAH to "שירה", Parsha.HAGADOL to "הגדול", Parsha.CHAZON to "חזון", Parsha.NACHAMU to "נחמו",
    )

    val ZMANIM: Map<Zman, String> = mapOf(
        Zman.ALOS to "עלות השחר", Zman.MISHEYAKIR to "משיכיר", Zman.HANETZ to "הנץ החמה", Zman.SOF_ZMAN_SHEMA to "סוף זמן קריאת שמע",
        Zman.SOF_ZMAN_TEFILLAH to "סוף זמן תפילה", Zman.SOF_ZMAN_ACHILAS_CHAMETZ to "סוף זמן אכילת חמץ",
        Zman.SOF_ZMAN_BIUR_CHAMETZ to "סוף זמן ביעור חמץ", Zman.CHATZOS to "חצות", Zman.MINCHA_GEDOLA to "מנחה גדולה",
        Zman.SAMUCH_LEMINCHA_KETANA to "סמוך למנחה קטנה", Zman.MINCHA_KETANA to "מנחה קטנה", Zman.PLAG_HAMINCHA to "פלג המנחה",
        Zman.HADLAKAS_NEIROS to "הדלקת נרות", Zman.SHKIAH to "שקיעה", Zman.TZAIS to "צאת הכוכבים", Zman.CHATZOS_HALAYLAH to "חצות הלילה",
    )
}

object Transliterations {
    val MONTHS: Map<HebrewMonth, String> = mapOf(
        HebrewMonth.NISAN to "Nissan", HebrewMonth.IYAR to "Iyar", HebrewMonth.SIVAN to "Sivan", HebrewMonth.TAMMUZ to "Tammuz",
        HebrewMonth.AV to "Av", HebrewMonth.ELUL to "Elul", HebrewMonth.TISHREI to "Tishrei", HebrewMonth.CHESHVAN to "Cheshvan",
        HebrewMonth.KISLEV to "Kislev", HebrewMonth.TEVET to "Teves", HebrewMonth.SHEVAT to "Shevat", HebrewMonth.ADAR to "Adar",
        HebrewMonth.ADAR_II to "Adar II",
    )

    val WEEKDAYS: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.SUNDAY to "Sunday", DayOfWeek.MONDAY to "Monday", DayOfWeek.TUESDAY to "Tuesday", DayOfWeek.WEDNESDAY to "Wednesday",
        DayOfWeek.THURSDAY to "Thursday", DayOfWeek.FRIDAY to "Friday", DayOfWeek.SATURDAY to "Shabbos",
    )

    val HOLIDAYS: Map<Holiday, String> = mapOf(
        Holiday.EREV_PESACH to "Erev Pesach", Holiday.PESACH to "Pesach", Holiday.CHOL_HAMOED_PESACH to "Chol Hamoed Pesach",
        Holiday.PESACH_SHENI to "Pesach Sheini", Holiday.EREV_SHAVUOS to "Erev Shavuos", Holiday.SHAVUOS to "Shavuos",
        Holiday.SHIVA_ASAR_BETAMMUZ to "Shivah Asar B'Tammuz", Holiday.TISHA_BEAV to "Tishah B'Av", Holiday.TU_BEAV to "Tu B'Av",
        Holiday.EREV_ROSH_HASHANAH to "Erev Rosh Hashanah", Holiday.ROSH_HASHANAH to "Rosh Hashanah", Holiday.TZOM_GEDALIAH to "Tzom Gedaliah",
        Holiday.EREV_YOM_KIPPUR to "Erev Yom Kippur", Holiday.YOM_KIPPUR to "Yom Kippur", Holiday.EREV_SUKKOS to "Erev Sukkos",
        Holiday.SUKKOS to "Sukkos", Holiday.CHOL_HAMOED_SUKKOS to "Chol Hamoed Sukkos", Holiday.HOSHANA_RABBAH to "Hoshana Rabbah",
        Holiday.SHEMINI_ATZERES to "Shemini Atzeres", Holiday.SIMCHAS_TORAH to "Simchas Torah", Holiday.CHANUKAH to "Chanukah",
        Holiday.ASARAH_BETEVES to "Asarah B'Teves", Holiday.TU_BISHVAT to "Tu B'Shevat", Holiday.TAANIS_ESTHER to "Taanis Esther",
        Holiday.PURIM to "Purim", Holiday.SHUSHAN_PURIM to "Shushan Purim", Holiday.PURIM_KATAN to "Purim Katan",
        Holiday.SHUSHAN_PURIM_KATAN to "Shushan Purim Katan", Holiday.LAG_BAOMER to "Lag BaOmer",
        Holiday.ISRU_CHAG to "Isru Chag",
    )

    val PARSHIOS: Map<Parsha, String> = mapOf(
        Parsha.BERESHIT to "Bereishis", Parsha.NOACH to "Noach", Parsha.LECH_LECHA to "Lech Lecha", Parsha.VAYERA to "Vayeira",
        Parsha.CHAYEI_SARAH to "Chayei Sarah", Parsha.TOLDOT to "Toldos", Parsha.VAYETZE to "Vayeitzei", Parsha.VAYISHLACH to "Vayishlach",
        Parsha.VAYESHEV to "Vayeishev", Parsha.MIKETZ to "Mikeitz", Parsha.VAYIGASH to "Vayigash", Parsha.VAYECHI to "Vayechi",
        Parsha.SHEMOT to "Shemos", Parsha.VAERA to "Va'eira", Parsha.BO to "Bo", Parsha.BESHALACH to "Beshalach", Parsha.YITRO to "Yisro",
        Parsha.MISHPATIM to "Mishpatim", Parsha.TERUMAH to "Terumah", Parsha.TETZAVEH to "Tetzaveh", Parsha.KI_TISA to "Ki Sisa",
        Parsha.VAYAKHEL to "Vayakhel", Parsha.PEKUDEI to "Pekudei", Parsha.VAYIKRA to "Vayikra", Parsha.TZAV to "Tzav",
        Parsha.SHEMINI to "Shemini", Parsha.TAZRIA to "Tazria", Parsha.METZORA to "Metzora", Parsha.ACHAREI_MOT to "Acharei Mos",
        Parsha.KEDOSHIM to "Kedoshim", Parsha.EMOR to "Emor", Parsha.BEHAR to "Behar", Parsha.BECHUKOTAI to "Bechukosai",
        Parsha.BAMIDBAR to "Bamidbar", Parsha.NASO to "Naso", Parsha.BEHAALOTCHA to "Beha'aloscha", Parsha.SHELACH to "Shelach",
        Parsha.KORACH to "Korach", Parsha.CHUKAT to "Chukas", Parsha.BALAK to "Balak", Parsha.PINCHAS to "Pinchas",
        Parsha.MATOT to "Matos", Parsha.MASEI to "Masei", Parsha.DEVARIM to "Devarim", Parsha.VAETCHANAN to "Va'eschanan",
        Parsha.EKEV to "Eikev", Parsha.REEH to "Re'eh", Parsha.SHOFTIM to "Shoftim", Parsha.KI_TETZE to "Ki Seitzei",
        Parsha.KI_TAVO to "Ki Savo", Parsha.NITZAVIM to "Nitzavim", Parsha.VAYELECH to "Vayeilech", Parsha.HAAZINU to "Ha'azinu",
        Parsha.VEZOT_HABERACHAH to "Vezos Haberachah", Parsha.VAYAKHEL_PEKUDEI to "Vayakhel-Pekudei", Parsha.TAZRIA_METZORA to "Tazria-Metzora",
        Parsha.ACHAREI_MOT_KEDOSHIM to "Acharei Mos-Kedoshim", Parsha.BEHAR_BECHUKOTAI to "Behar-Bechukosai", Parsha.CHUKAT_BALAK to "Chukas-Balak",
        Parsha.MATOT_MASEI to "Matos-Masei", Parsha.NITZAVIM_VAYELECH to "Nitzavim-Vayeilech", Parsha.SHEKALIM to "Shekalim",
        Parsha.ZACHOR to "Zachor", Parsha.PARAH to "Parah", Parsha.HACHODESH to "Hachodesh", Parsha.SHUVAH to "Shuvah",
        Parsha.SHIRAH to "Shirah", Parsha.HAGADOL to "Hagadol", Parsha.CHAZON to "Chazon", Parsha.NACHAMU to "Nachamu",
    )

    val BAVLI: Map<BavliTractate, String> = mapOf(
        BavliTractate.BERACHOS to "Berachos", BavliTractate.SHABBOS to "Shabbos", BavliTractate.ERUVIN to "Eruvin",
        BavliTractate.PESACHIM to "Pesachim", BavliTractate.SHEKALIM to "Shekalim", BavliTractate.YOMA to "Yoma",
        BavliTractate.SUKKAH to "Sukkah", BavliTractate.BEITZAH to "Beitzah", BavliTractate.ROSH_HASHANAH to "Rosh Hashanah",
        BavliTractate.TAANIS to "Taanis", BavliTractate.MEGILLAH to "Megillah", BavliTractate.MOED_KATAN to "Moed Katan",
        BavliTractate.CHAGIGAH to "Chagigah", BavliTractate.YEVAMOS to "Yevamos", BavliTractate.KESUBOS to "Kesubos",
        BavliTractate.NEDARIM to "Nedarim", BavliTractate.NAZIR to "Nazir", BavliTractate.SOTAH to "Sotah",
        BavliTractate.GITTIN to "Gittin", BavliTractate.KIDDUSHIN to "Kiddushin", BavliTractate.BAVA_KAMMA to "Bava Kamma",
        BavliTractate.BAVA_METZIA to "Bava Metzia", BavliTractate.BAVA_BASRA to "Bava Basra", BavliTractate.SANHEDRIN to "Sanhedrin",
        BavliTractate.MAKKOS to "Makkos", BavliTractate.SHEVUOS to "Shevuos", BavliTractate.AVODAH_ZARAH to "Avodah Zarah",
        BavliTractate.HORAYOS to "Horayos", BavliTractate.ZEVACHIM to "Zevachim", BavliTractate.MENACHOS to "Menachos",
        BavliTractate.CHULLIN to "Chullin", BavliTractate.BECHOROS to "Bechoros", BavliTractate.ARACHIN to "Arachin",
        BavliTractate.TEMURAH to "Temurah", BavliTractate.KEREISOS to "Kereisos", BavliTractate.MEILAH to "Meilah",
        BavliTractate.KINNIM to "Kinnim", BavliTractate.TAMID to "Tamid", BavliTractate.MIDDOS to "Middos", BavliTractate.NIDDAH to "Niddah",
    )

    val YERUSHALMI: Map<YerushalmiTractate, String> = mapOf(
        YerushalmiTractate.BERACHOS to "Berachos", YerushalmiTractate.PEAH to "Pe'ah", YerushalmiTractate.DEMAI to "Demai",
        YerushalmiTractate.KILAYIM to "Kilayim", YerushalmiTractate.SHEVIIS to "Shevi'is", YerushalmiTractate.TERUMOS to "Terumos",
        YerushalmiTractate.MAASROS to "Ma'asros", YerushalmiTractate.MAASER_SHENI to "Ma'aser Sheini", YerushalmiTractate.CHALLAH to "Challah",
        YerushalmiTractate.ORLAH to "Orlah", YerushalmiTractate.BIKKURIM to "Bikkurim", YerushalmiTractate.SHABBOS to "Shabbos",
        YerushalmiTractate.ERUVIN to "Eruvin", YerushalmiTractate.PESACHIM to "Pesachim", YerushalmiTractate.BEITZAH to "Beitzah",
        YerushalmiTractate.ROSH_HASHANAH to "Rosh Hashanah", YerushalmiTractate.YOMA to "Yoma", YerushalmiTractate.SUKKAH to "Sukkah",
        YerushalmiTractate.TAANIS to "Taanis", YerushalmiTractate.SHEKALIM to "Shekalim", YerushalmiTractate.MEGILLAH to "Megillah",
        YerushalmiTractate.CHAGIGAH to "Chagigah", YerushalmiTractate.MOED_KATAN to "Moed Katan", YerushalmiTractate.YEVAMOS to "Yevamos",
        YerushalmiTractate.KESUBOS to "Kesubos", YerushalmiTractate.SOTAH to "Sotah", YerushalmiTractate.NEDARIM to "Nedarim",
        YerushalmiTractate.NAZIR to "Nazir", YerushalmiTractate.GITTIN to "Gittin", YerushalmiTractate.KIDDUSHIN to "Kiddushin",
        YerushalmiTractate.BAVA_KAMMA to "Bava Kamma", YerushalmiTractate.BAVA_METZIA to "Bava Metzia", YerushalmiTractate.BAVA_BASRA to "Bava Basra",
        YerushalmiTractate.SHEVUOS to "Shevuos", YerushalmiTractate.MAKKOS to "Makkos", YerushalmiTractate.SANHEDRIN to "Sanhedrin",
        YerushalmiTractate.AVODAH_ZARAH to "Avodah Zarah", YerushalmiTractate.HORAYOS to "Horayos", YerushalmiTractate.NIDDAH to "Niddah",
    )

    val ZMANIM: Map<Zman, String> = mapOf(
        Zman.ALOS to "Alos Hashachar", Zman.MISHEYAKIR to "Misheyakir", Zman.HANETZ to "Hanetz Hachamah", Zman.SOF_ZMAN_SHEMA to "Sof Zman Krias Shema",
        Zman.SOF_ZMAN_TEFILLAH to "Sof Zman Tefillah", Zman.SOF_ZMAN_ACHILAS_CHAMETZ to "Sof Zman Achilas Chametz",
        Zman.SOF_ZMAN_BIUR_CHAMETZ to "Sof Zman Biur Chametz", Zman.CHATZOS to "Chatzos", Zman.MINCHA_GEDOLA to "Mincha Gedolah",
        Zman.SAMUCH_LEMINCHA_KETANA to "Samuch LeMincha Ketanah", Zman.MINCHA_KETANA to "Mincha Ketanah", Zman.PLAG_HAMINCHA to "Plag Hamincha",
        Zman.HADLAKAS_NEIROS to "Hadlakas Neiros", Zman.SHKIAH to "Shkiah", Zman.TZAIS to "Tzais Hakochavim", Zman.CHATZOS_HALAYLAH to "Chatzos Halaylah",
    )
}
