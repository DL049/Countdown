package com.example.countdown

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

object Fmt {
    private val TAG_COLORS = intArrayOf(
        R.color.tag_blue, R.color.tag_green, R.color.tag_orange, R.color.tag_purple, R.color.tag_red,
    )
    val TAG_COUNT = TAG_COLORS.size

    fun tagColor(c: Context, idx: Int): Int =
        ContextCompat.getColor(c, TAG_COLORS[idx.coerceIn(0, TAG_COLORS.size - 1)])

    /** Colour of the big number: tag colour while counting down, green on the day, grey afterwards. */
    fun numberColor(c: Context, e: CountdownEvent, s: EventSummary): Int = when (s.phase) {
        Phase.UPCOMING -> tagColor(c, e.colorIdx)
        Phase.TODAY -> ContextCompat.getColor(c, R.color.soft_green_text)
        Phase.PAST -> ContextCompat.getColor(c, R.color.soft_muted)
    }

    fun dateFull(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    fun dateMedium(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    /** The big number: days left while counting down, days since once the date has passed. */
    fun number(s: EventSummary): Int = if (s.phase == Phase.PAST) s.elapsedDays else s.left

    fun unit(c: Context, e: CountdownEvent, s: EventSummary): String = c.getString(
        when (s.phase) {
            Phase.PAST -> R.string.unit_past
            Phase.TODAY -> R.string.unit_today
            Phase.UPCOMING ->
                if (e.activeDays.size == 7) R.string.unit_days else R.string.unit_weekdays
        },
    )

    /** One-line form, e.g. "12 天" / "12 天前" / "就是今天 🎉". */
    fun compact(c: Context, e: CountdownEvent, s: EventSummary): String =
        if (s.phase == Phase.TODAY) unit(c, e, s) else "${number(s)} ${unit(c, e, s)}"

    fun daysLabel(c: Context, days: Set<DayOfWeek>): String = when {
        days.size == 7 -> c.getString(R.string.mode_everyday)
        days == CountdownCalc.WEEKDAYS -> c.getString(R.string.mode_weekdays)
        else -> days.sorted().joinToString(c.getString(R.string.list_sep)) {
            it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    fun detail(c: Context, e: CountdownEvent, s: EventSummary): String {
        val parts = ArrayList<String>()
        parts.add(daysLabel(c, e.activeDays))
        if (e.repeat == Repeat.YEARLY) parts.add(c.getString(R.string.repeat_yearly))
        if (s.skippedByHolidays > 0) parts.add(c.getString(R.string.detail_skipped_part, s.skippedByHolidays))
        return parts.joinToString(" · ")
    }
}

/** Shows a Material date picker; MaterialDatePicker works in UTC milliseconds. */
fun AppCompatActivity.pickDate(titleRes: Int, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    val picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(titleRes)
        .setSelection(initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        .build()
    picker.addOnPositiveButtonClickListener { ms ->
        onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
    }
    picker.show(supportFragmentManager, "date")
}
