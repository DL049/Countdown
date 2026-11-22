package com.example.countdown

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/** Notifications, calendar (.ics) import and backup / restore. */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var notifySwitch: MaterialSwitch
    private lateinit var timeButton: SoftButton

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setNotify(true)
            } else {
                notifySwitch.isChecked = false
                Toast.makeText(requireContext(), R.string.notif_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    private val exportBackup =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) writeBackup(uri)
        }

    private val importBackup =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) confirmRestore(uri)
        }

    private val importIcs =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) readIcs(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val c = requireContext()
        notifySwitch = view.findViewById(R.id.switch_notify_enabled)
        timeButton = view.findViewById(R.id.btn_time)

        notifySwitch.isChecked = Repo.notifyEnabled(c)
        notifySwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && !Notifier.hasPermission(c) && Build.VERSION.SDK_INT >= 33) {
                requestPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                setNotify(checked)
            }
        }
        refreshTime()
        timeButton.setOnClickListener {
            val m = Repo.notifyMinutes(c)
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(m / 60)
                .setMinute(m % 60)
                .setTitleText(R.string.settings_notify_time)
                .build()
            picker.addOnPositiveButtonClickListener {
                Repo.setNotifyMinutes(c, picker.hour * 60 + picker.minute)
                Notifier.schedule(c)
                refreshTime()
            }
            picker.show(childFragmentManager, "time")
        }

        view.findViewById<View>(R.id.btn_import_ics).setOnClickListener { importIcs.launch(arrayOf("*/*")) }
        view.findViewById<View>(R.id.btn_export).setOnClickListener { exportBackup.launch("countdown-backup.json") }
        view.findViewById<View>(R.id.btn_import).setOnClickListener { importBackup.launch(arrayOf("*/*")) }
    }

    private fun refreshTime() {
        val m = Repo.notifyMinutes(requireContext())
        timeButton.text = String.format("%02d:%02d", m / 60, m % 60)
    }

    private fun setNotify(on: Boolean) {
        val c = requireContext()
        Repo.setNotifyEnabled(c, on)
        if (on) Notifier.ensureChannel(c)
        Notifier.schedule(c)
        if (notifySwitch.isChecked != on) notifySwitch.isChecked = on
    }

    private fun readText(uri: Uri): String? = try {
        requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    } catch (e: Exception) {
        null
    }

    // ---- backup ----
    private fun writeBackup(uri: Uri) {
        val c = requireContext()
        val ok = try {
            c.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(Repo.exportJson(c)) } != null
        } catch (e: Exception) {
            false
        }
        Toast.makeText(c, if (ok) R.string.backup_exported else R.string.backup_failed, Toast.LENGTH_SHORT).show()
    }

    private fun confirmRestore(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.backup_import_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.import_backup) { _, _ ->
                val c = requireContext()
                val text = readText(uri)
                if (text != null && Repo.importJson(c, text)) {
                    WidgetUpdater.updateAll(c)
                    notifySwitch.isChecked = Repo.notifyEnabled(c)
                    refreshTime()
                    Notifier.schedule(c)
                    Toast.makeText(c, R.string.backup_imported, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(c, R.string.backup_failed, Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    // ---- .ics holiday import ----
    private fun readIcs(uri: Uri) {
        val c = requireContext()
        val items = readText(uri)?.let { IcsParser.parse(it) }.orEmpty().sortedBy { it.start }
        if (items.isEmpty()) {
            Toast.makeText(c, R.string.ics_none, Toast.LENGTH_LONG).show()
            return
        }
        val labels = items.map {
            val range = if (it.start == it.end) {
                Fmt.dateMedium(it.start)
            } else {
                getString(R.string.range_fmt, Fmt.dateMedium(it.start), Fmt.dateMedium(it.end))
            }
            "${it.name}（$range）"
        }.toTypedArray()
        val checked = BooleanArray(items.size) { true }
        MaterialAlertDialogBuilder(c)
            .setTitle(R.string.ics_pick_title)
            .setMultiChoiceItems(labels, checked) { _, i, on -> checked[i] = on }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.import_ics) { _, _ ->
                val existing = Repo.holidays(c).map { Triple(it.name, it.start, it.end) }.toSet()
                var added = 0
                items.forEachIndexed { i, it ->
                    if (checked[i] && Triple(it.name, it.start, it.end) !in existing) {
                        Repo.upsertHoliday(c, Holiday(Repo.newId(), it.name, it.start, it.end))
                        added++
                    }
                }
                WidgetUpdater.updateAll(c)
                Toast.makeText(c, getString(R.string.ics_imported, added), Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
