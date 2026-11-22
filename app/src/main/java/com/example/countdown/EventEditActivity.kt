package com.example.countdown

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

/** Create or edit one countdown event. */
class EventEditActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    private lateinit var eventId: String
    private var isNew = true
    private var target: LocalDate = LocalDate.now().plusDays(30)
    private var allHolidays = emptyList<Holiday>()
    private val checks = LinkedHashMap<String, CheckBox>()

    private lateinit var titleLayout: TextInputLayout
    private lateinit var titleEdit: TextInputEditText
    private lateinit var dateButton: Button
    private lateinit var toggle: MaterialButtonToggleGroup
    private lateinit var todaySwitch: MaterialSwitch
    private lateinit var preview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_edit)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        titleLayout = findViewById(R.id.title_layout)
        titleEdit = findViewById(R.id.edit_title)
        dateButton = findViewById(R.id.btn_date)
        toggle = findViewById(R.id.toggle_mode)
        todaySwitch = findViewById(R.id.switch_today)
        preview = findViewById(R.id.tv_preview)

        allHolidays = Repo.holidays(this)
        val existing = intent.getStringExtra(EXTRA_EVENT_ID)?.let { Repo.event(this, it) }
        isNew = existing == null
        eventId = existing?.id ?: Repo.newId()
        supportActionBar?.setTitle(if (isNew) R.string.new_event else R.string.edit_event)

        existing?.let {
            titleEdit.setText(it.title)
            target = it.target
            todaySwitch.isChecked = it.includeToday
        }
        toggle.check(
            if (existing?.mode == CountMode.WEEKDAYS) R.id.btn_weekdays else R.id.btn_everyday,
        )

        // holiday checklist (new events skip every holiday by default)
        val container = findViewById<LinearLayout>(R.id.holiday_container)
        val noHolidays = findViewById<TextView>(R.id.tv_no_holidays)
        noHolidays.visibility = if (allHolidays.isEmpty()) View.VISIBLE else View.GONE
        allHolidays.forEach { h ->
            val cb = CheckBox(this).apply {
                text = getString(
                    R.string.holiday_check_fmt, h.name,
                    Fmt.dateMedium(h.start), Fmt.dateMedium(h.end),
                )
                isChecked = existing?.let { h.id in it.holidayIds } ?: true
                setOnCheckedChangeListener { _, _ -> refresh() }
            }
            checks[h.id] = cb
            container.addView(cb)
        }

        dateButton.setOnClickListener {
            pickDate(R.string.label_target_date, target) { target = it; refresh() }
        }
        toggle.addOnButtonCheckedListener { _, _, _ -> refresh() }
        todaySwitch.setOnCheckedChangeListener { _, _ -> refresh() }
        findViewById<Button>(R.id.btn_save).setOnClickListener { save() }
        refresh()
    }

    private fun currentEvent() = CountdownEvent(
        id = eventId,
        title = titleEdit.text?.toString()?.trim().orEmpty(),
        target = target,
        mode = if (toggle.checkedButtonId == R.id.btn_weekdays) CountMode.WEEKDAYS else CountMode.EVERYDAY,
        includeToday = todaySwitch.isChecked,
        holidayIds = checks.filter { it.value.isChecked }.keys.toSet(),
    )

    private fun refresh() {
        dateButton.text = Fmt.dateFull(target)
        val e = currentEvent()
        val s = CountdownCalc.summarize(e, allHolidays)
        preview.text = getString(R.string.preview_fmt, s.left, Fmt.unit(this, e, s))
    }

    private fun save() {
        val e = currentEvent()
        if (e.title.isEmpty()) {
            titleLayout.error = getString(R.string.title_required)
            return
        }
        Repo.upsertEvent(this, e)
        CountdownWidgetProvider.updateAll(this)
        setResult(RESULT_OK, Intent().putExtra(EXTRA_EVENT_ID, e.id))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isNew) menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { finish(); true }
        R.id.action_delete -> {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirm_delete_event)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    Repo.deleteEvent(this, eventId)
                    CountdownWidgetProvider.updateAll(this)
                    finish()
                }
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
