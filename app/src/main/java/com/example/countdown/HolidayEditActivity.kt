package com.example.countdown

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.google.android.material.materialswitch.MaterialSwitch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate

/** Create or edit one holiday (a named, inclusive date range). */
class HolidayEditActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_HOLIDAY_ID = "holiday_id"
    }

    private lateinit var holidayId: String
    private var isNew = true
    private var start: LocalDate = LocalDate.now().plusDays(7)
    private var end: LocalDate = LocalDate.now().plusDays(7)

    private lateinit var nameError: TextView
    private lateinit var nameEdit: EditText
    private lateinit var startButton: SoftButton
    private lateinit var endButton: SoftButton
    private lateinit var singleSwitch: MaterialSwitch
    private lateinit var endLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_holiday_edit)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nameError = findViewById(R.id.name_error)
        nameEdit = findViewById(R.id.edit_name)
        startButton = findViewById(R.id.btn_start)
        endButton = findViewById(R.id.btn_end)
        singleSwitch = findViewById(R.id.switch_single)
        endLabel = findViewById(R.id.label_end)

        val existing = intent.getStringExtra(EXTRA_HOLIDAY_ID)?.let { Repo.holiday(this, it) }
        isNew = existing == null
        holidayId = existing?.id ?: Repo.newId()
        supportActionBar?.setTitle(if (isNew) R.string.new_holiday else R.string.edit_holiday)
        existing?.let {
            nameEdit.setText(it.name)
            start = it.start
            end = it.end
            singleSwitch.isChecked = it.start == it.end
        }
        singleSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) end = start
            refresh()
        }

        startButton.setOnClickListener {
            pickDate(R.string.label_start, start) {
                start = it
                if (singleSwitch.isChecked || end.isBefore(start)) end = start
                refresh()
            }
        }
        endButton.setOnClickListener {
            pickDate(R.string.label_end, end) {
                end = it
                if (end.isBefore(start)) start = end
                refresh()
            }
        }
        findViewById<View>(R.id.btn_save).setOnClickListener { save() }
        refresh()
    }

    private fun refresh() {
        val single = singleSwitch.isChecked
        endLabel.visibility = if (single) View.GONE else View.VISIBLE
        endButton.visibility = if (single) View.GONE else View.VISIBLE
        startButton.text = Fmt.dateFull(start)
        endButton.text = Fmt.dateFull(end)
    }

    private fun save() {
        val name = nameEdit.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            nameError.visibility = View.VISIBLE
            nameEdit.requestFocus()
            return
        }
        Repo.upsertHoliday(this, Holiday(holidayId, name, start, end))
        WidgetUpdater.updateAll(this)
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
                .setMessage(R.string.confirm_delete_holiday)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    Repo.deleteHoliday(this, holidayId)
                    WidgetUpdater.updateAll(this)
                    finish()
                }
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
