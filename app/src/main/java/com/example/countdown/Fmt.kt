package com.example.countdown

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object Fmt {
    fun dateFull(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    fun dateMedium(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    fun unit(c: Context, e: CountdownEvent, s: EventSummary): String = c.getString(
        when {
            s.reached -> R.string.unit_done
            e.mode == CountMode.WEEKDAYS -> R.string.unit_weekdays
            else -> R.string.unit_days
        },
    )

    fun detail(c: Context, e: CountdownEvent, s: EventSummary): String {
        val mode = c.getString(
            if (e.mode == CountMode.WEEKDAYS) R.string.mode_weekdays else R.string.mode_everyday,
        )
        return if (s.skippedByHolidays > 0) {
            c.getString(R.string.detail_skipped, mode, s.skippedByHolidays)
        } else mode
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
