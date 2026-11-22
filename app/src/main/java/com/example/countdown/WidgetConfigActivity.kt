package com.example.countdown

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

/** Shown when a widget is added: pick which countdown event it displays. */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val adapter = EventAdapter { bind(it.id) }
    private lateinit var empty: TextView

    private val createEvent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            r.data?.getStringExtra(EventEditActivity.EXTRA_EVENT_ID)?.let { bind(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED) // backing out = the widget is not added
        setContentView(R.layout.activity_widget_config)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setTitle(R.string.pick_event)

        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WidgetConfigActivity.adapter
        }
        empty = findViewById(R.id.empty)
        findViewById<View>(R.id.btn_create).setOnClickListener {
            createEvent.launch(Intent(this, EventEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val events = Repo.events(this)
        adapter.submit(events, Repo.holidays(this))
        empty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun bind(eventId: String) {
        Repo.bindWidget(this, widgetId, eventId)
        CountdownWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
