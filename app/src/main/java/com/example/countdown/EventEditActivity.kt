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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Create or edit one countdown event (including its widget background image). */
class EventEditActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    private lateinit var eventId: String
    private var isNew = true
    private var created: LocalDate = LocalDate.now()
    private var target: LocalDate = LocalDate.now().plusDays(30)
    private var activeDays: Set<DayOfWeek> = CountdownCalc.WEEKDAYS
    private var colorIdx = 0
    private var scrim = 40
    private var lightText = true
    private var allHolidays = emptyList<Holiday>()
    private val checks = LinkedHashMap<String, CheckBox>()

    private lateinit var titleEdit: EditText
    private lateinit var titleError: TextView
    private lateinit var categoryEdit: EditText
    private lateinit var dateButton: SoftButton
    private lateinit var btnEveryday: SoftButton
    private lateinit var btnWeekdays: SoftButton
    private lateinit var dayChips: List<SoftButton>
    private lateinit var colorChips: List<SoftButton>
    private lateinit var todaySwitch: MaterialSwitch
    private lateinit var yearlySwitch: MaterialSwitch
    private lateinit var pinSwitch: MaterialSwitch
    private lateinit var notifySwitch: MaterialSwitch
    private lateinit var preview: TextView
    private lateinit var bgPreview: ImageView
    private lateinit var bgHint: TextView
    private lateinit var btnRemoveImage: SoftButton
    private lateinit var seekScrim: SeekBar
    private lateinit var scrimLabel: TextView
    private lateinit var btnTextLight: SoftButton
    private lateinit var btnTextDark: SoftButton

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
        categoryEdit = findViewById(R.id.edit_category)
        dateButton = findViewById(R.id.btn_date)
        btnEveryday = findViewById(R.id.btn_everyday)
        btnWeekdays = findViewById(R.id.btn_weekdays)
        todaySwitch = findViewById(R.id.switch_today)
        yearlySwitch = findViewById(R.id.switch_yearly)
        pinSwitch = findViewById(R.id.switch_pin)
        notifySwitch = findViewById(R.id.switch_notify)
        preview = findViewById(R.id.tv_preview)
        bgPreview = findViewById(R.id.img_bg_preview)
        bgHint = findViewById(R.id.tv_bg_hint)
        btnRemoveImage = findViewById(R.id.btn_remove_image)
        seekScrim = findViewById(R.id.seek_scrim)
        scrimLabel = findViewById(R.id.tv_scrim_label)
        btnTextLight = findViewById(R.id.btn_text_light)
        btnTextDark = findViewById(R.id.btn_text_dark)

        allHolidays = Repo.holidays(this)
        val existing = intent.getStringExtra(EXTRA_EVENT_ID)?.let { Repo.event(this, it) }
        isNew = existing == null
        eventId = existing?.id ?: Repo.newId()
        supportActionBar?.setTitle(if (isNew) R.string.new_event else R.string.edit_event)

        existing?.let {
            titleEdit.setText(it.title)
            categoryEdit.setText(it.category)
            created = it.start
            target = it.target
            activeDays = it.activeDays
            colorIdx = it.colorIdx
            scrim = it.scrimPercent
            lightText = it.lightText
            todaySwitch.isChecked = it.includeToday
            yearlySwitch.isChecked = it.repeat == Repeat.YEARLY
            pinSwitch.isChecked = it.pinned
            notifySwitch.isChecked = it.notify
        } ?: run { notifySwitch.isChecked = true }
        seekScrim.progress = scrim

        setupDayChips()
        setupColorChips()
        setupHolidayChecklist(existing)

        dateButton.setOnClickListener {
            pickDate(R.string.label_target_date, target) { target = it; refresh() }
        }
        btnEveryday.setOnClickListener { activeDays = CountdownCalc.ALL_DAYS; refresh() }
        btnWeekdays.setOnClickListener { activeDays = CountdownCalc.WEEKDAYS; refresh() }
        listOf(todaySwitch, yearlySwitch).forEach { sw -> sw.setOnCheckedChangeListener { _, _ -> refresh() } }
        btnTextLight.setOnClickListener { lightText = true; refresh() }
        btnTextDark.setOnClickListener { lightText = false; refresh() }
        seekScrim.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                scrim = p
                refresh()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        findViewById<View>(R.id.btn_pick_image).setOnClickListener { pickImage.launch("image/*") }
        btnRemoveImage.setOnClickListener {
            WidgetBg.delete(this, eventId)
            updateBgPreview()
            WidgetUpdater.updateAll(this)
        }
        findViewById<View>(R.id.btn_save).setOnClickListener { save() }

        updateBgPreview()
        refresh()
    }

    private fun setupDayChips() {
        val ids = intArrayOf(
            R.id.day_1, R.id.day_2, R.id.day_3, R.id.day_4, R.id.day_5, R.id.day_6, R.id.day_7,
        )
        val letters = resources.getStringArray(R.array.day_letters)
        dayChips = ids.mapIndexed { i, id ->
            val day = DayOfWeek.of(i + 1)
            findViewById<SoftButton>(id).apply {
                text = letters[i]
                contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                setOnClickListener {
                    val next = if (day in activeDays) activeDays - day else activeDays + day
                    if (next.isNotEmpty()) { // at least one day must count
                        activeDays = next
                        refresh()
                    }
                }
            }
        }
    }

    private fun setupColorChips() {
        val ids = intArrayOf(R.id.color_0, R.id.color_1, R.id.color_2, R.id.color_3, R.id.color_4)
        colorChips = ids.mapIndexed { i, id ->
            findViewById<SoftButton>(id).apply {
                setLabelColor(Fmt.tagColor(this@EventEditActivity, i))
                contentDescription = getString(R.string.color_desc, i + 1)
                setOnClickListener { colorIdx = i; refresh() }
            }
        }
    }

    private fun setupHolidayChecklist(existing: CountdownEvent?) {
        // new events skip every holiday by default
        val container = findViewById<LinearLayout>(R.id.holiday_container)
        val none = allHolidays.isEmpty()
        findViewById<View>(R.id.tv_no_holidays).visibility = if (none) View.VISIBLE else View.GONE
        findViewById<View>(R.id.holiday_well).visibility = if (none) View.GONE else View.VISIBLE
        allHolidays.forEach { h ->
            val range = if (h.start == h.end) {
                Fmt.dateMedium(h.start)
            } else {
                getString(R.string.range_fmt, Fmt.dateMedium(h.start), Fmt.dateMedium(h.end))
            }
            val cb = CheckBox(this).apply {
                text = getString(R.string.holiday_check_fmt, h.name, range)
                setTextColor(ContextCompat.getColor(context, R.color.soft_text))
                isChecked = existing?.let { h.id in it.holidayIds } ?: true
                setOnCheckedChangeListener { _, _ -> refresh() }
            }
            checks[h.id] = cb
            container.addView(cb)
        }
    }

    private fun currentEvent() = CountdownEvent(
        id = eventId,
        title = titleEdit.text?.toString()?.trim().orEmpty(),
        target = target,
        activeDays = activeDays,
        includeToday = todaySwitch.isChecked,
        holidayIds = checks.filter { it.value.isChecked }.keys.toSet(),
        start = created,
        pinned = pinSwitch.isChecked,
        colorIdx = colorIdx,
        category = categoryEdit.text?.toString()?.trim().orEmpty(),
        repeat = if (yearlySwitch.isChecked) Repeat.YEARLY else Repeat.NONE,
        notify = notifySwitch.isChecked,
        scrimPercent = scrim,
        lightText = lightText,
    )

    private fun refresh() {
        dateButton.text = Fmt.dateFull(target)
        btnEveryday.setSegment(activeDays.size == 7)
        btnWeekdays.setSegment(activeDays == CountdownCalc.WEEKDAYS)
        dayChips.forEachIndexed { i, b -> b.setSegment(DayOfWeek.of(i + 1) in activeDays) }
        colorChips.forEachIndexed { i, b -> b.setPressedIn(i == colorIdx) }
        btnTextLight.setSegment(lightText)
        btnTextDark.setSegment(!lightText)
        scrimLabel.text = getString(R.string.scrim_label, scrim)

        val e = currentEvent()
        val s = CountdownCalc.summarize(e, allHolidays)
        preview.text = when (s.phase) {
            Phase.UPCOMING -> getString(R.string.preview_future, s.left, Fmt.unit(this, e, s))
            Phase.TODAY -> getString(R.string.preview_today)
            Phase.PAST -> getString(R.string.preview_past, s.elapsedDays, Fmt.unit(this, e, s))
        }
        preview.setTextColor(Fmt.numberColor(this, e, s))
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
        WidgetUpdater.updateAll(this)
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
                    WidgetUpdater.updateAll(this)
                    finish()
                }
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
