package com.example.countdown

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.LocalDate

/** Create or edit one countdown event (including its widget background image). */
class EventEditActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    private lateinit var eventId: String
    private var isNew = true
    private var target: LocalDate = LocalDate.now().plusDays(30)
    private var weekdays = false
    private var allHolidays = emptyList<Holiday>()
    private val checks = LinkedHashMap<String, CheckBox>()

    private lateinit var titleEdit: EditText
    private lateinit var titleError: TextView
    private lateinit var dateButton: SoftButton
    private lateinit var btnEveryday: SoftButton
    private lateinit var btnWeekdays: SoftButton
    private lateinit var todaySwitch: MaterialSwitch
    private lateinit var preview: TextView
    private lateinit var bgPreview: ImageView
    private lateinit var bgHint: TextView
    private lateinit var btnRemoveImage: SoftButton

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                if (WidgetBg.saveSource(this, eventId, uri)) {
                    updateBgPreview()
                } else {
                    Toast.makeText(this, R.string.image_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_edit)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        titleEdit = findViewById(R.id.edit_title)
        titleError = findViewById(R.id.title_error)
        dateButton = findViewById(R.id.btn_date)
        btnEveryday = findViewById(R.id.btn_everyday)
        btnWeekdays = findViewById(R.id.btn_weekdays)
        todaySwitch = findViewById(R.id.switch_today)
        preview = findViewById(R.id.tv_preview)
        bgPreview = findViewById(R.id.img_bg_preview)
        bgHint = findViewById(R.id.tv_bg_hint)
        btnRemoveImage = findViewById(R.id.btn_remove_image)

        allHolidays = Repo.holidays(this)
        val existing = intent.getStringExtra(EXTRA_EVENT_ID)?.let { Repo.event(this, it) }
        isNew = existing == null
        eventId = existing?.id ?: Repo.newId()
        supportActionBar?.setTitle(if (isNew) R.string.new_event else R.string.edit_event)

        existing?.let {
            titleEdit.setText(it.title)
            target = it.target
            todaySwitch.isChecked = it.includeToday
            weekdays = it.mode == CountMode.WEEKDAYS
        }

        // holiday checklist (new events skip every holiday by default)
        val container = findViewById<LinearLayout>(R.id.holiday_container)
        val noHolidays = allHolidays.isEmpty()
        findViewById<View>(R.id.tv_no_holidays).visibility = if (noHolidays) View.VISIBLE else View.GONE
        findViewById<View>(R.id.holiday_well).visibility = if (noHolidays) View.GONE else View.VISIBLE
        allHolidays.forEach { h ->
            val cb = CheckBox(this).apply {
                text = getString(
                    R.string.holiday_check_fmt, h.name,
                    Fmt.dateMedium(h.start), Fmt.dateMedium(h.end),
                )
                setTextColor(ContextCompat.getColor(context, R.color.soft_text))
                isChecked = existing?.let { h.id in it.holidayIds } ?: true
                setOnCheckedChangeListener { _, _ -> refresh() }
            }
            checks[h.id] = cb
            container.addView(cb)
        }

        dateButton.setOnClickListener {
            pickDate(R.string.label_target_date, target) { target = it; refresh() }
        }
        btnEveryday.setOnClickListener { weekdays = false; refresh() }
        btnWeekdays.setOnClickListener { weekdays = true; refresh() }
        todaySwitch.setOnCheckedChangeListener { _, _ -> refresh() }
        findViewById<View>(R.id.btn_pick_image).setOnClickListener { pickImage.launch("image/*") }
        btnRemoveImage.setOnClickListener {
            WidgetBg.delete(this, eventId)
            updateBgPreview()
            CountdownWidgetProvider.updateAll(this)
        }
        findViewById<View>(R.id.btn_save).setOnClickListener { save() }

        updateBgPreview()
        refresh()
    }

    private fun currentEvent() = CountdownEvent(
        id = eventId,
        title = titleEdit.text?.toString()?.trim().orEmpty(),
        target = target,
        mode = if (weekdays) CountMode.WEEKDAYS else CountMode.EVERYDAY,
        includeToday = todaySwitch.isChecked,
        holidayIds = checks.filter { it.value.isChecked }.keys.toSet(),
    )

    private fun refresh() {
        dateButton.text = Fmt.dateFull(target)
        btnEveryday.setSegment(!weekdays)
        btnWeekdays.setSegment(weekdays)
        val e = currentEvent()
        val s = CountdownCalc.summarize(e, allHolidays)
        preview.text = getString(R.string.preview_fmt, s.left, Fmt.unit(this, e, s))
    }

    private fun updateBgPreview() {
        val bmp = WidgetBg.thumb(this, eventId)
        if (bmp != null) {
            bgPreview.setImageBitmap(bmp)
            bgPreview.visibility = View.VISIBLE
            bgHint.visibility = View.GONE
            btnRemoveImage.visibility = View.VISIBLE
        } else {
            bgPreview.setImageDrawable(null)
            bgPreview.visibility = View.GONE
            bgHint.visibility = View.VISIBLE
            btnRemoveImage.visibility = View.GONE
        }
    }

    private fun save() {
        titleError.visibility = View.GONE
        val e = currentEvent()
        if (e.title.isEmpty()) {
            titleError.visibility = View.VISIBLE
            titleEdit.requestFocus()
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
                    WidgetBg.delete(this, eventId)
                    CountdownWidgetProvider.updateAll(this)
                    finish()
                }
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
