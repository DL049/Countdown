package com.example.countdown

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioButton
import java.time.LocalDate

/** Shown when a widget is added, and again when the user taps an existing widget. */
class ConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED) // back out = widget is not added
        setContentView(R.layout.activity_config)

        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val title = findViewById<EditText>(R.id.edit_title)
        val picker = findViewById<DatePicker>(R.id.date_picker)
        val everyday = findViewById<RadioButton>(R.id.radio_everyday)
        val weekdays = findViewById<RadioButton>(R.id.radio_weekdays)
        val includeToday = findViewById<CheckBox>(R.id.check_include_today)

        val existing = CountdownStore.load(this, widgetId)
        val date = existing?.target ?: LocalDate.now().plusDays(30)
        title.setText(existing?.title ?: "")
        picker.updateDate(date.year, date.monthValue - 1, date.dayOfMonth)
        if ((existing?.mode ?: CountMode.EVERYDAY) == CountMode.WEEKDAYS) {
            weekdays.isChecked = true
        } else {
            everyday.isChecked = true
        }
        includeToday.isChecked = existing?.includeToday ?: false

        findViewById<Button>(R.id.button_save).setOnClickListener {
            val cfg = CountdownConfig(
                title = title.text.toString().trim(),
                target = LocalDate.of(picker.year, picker.month + 1, picker.dayOfMonth),
                mode = if (weekdays.isChecked) CountMode.WEEKDAYS else CountMode.EVERYDAY,
                includeToday = includeToday.isChecked,
            )
            CountdownStore.save(this, widgetId, cfg)
            CountdownWidgetProvider.updateWidget(
                this, AppWidgetManager.getInstance(this), widgetId,
            )
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
