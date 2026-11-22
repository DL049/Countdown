package com.example.countdown

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        Notifier.ensureChannel(this)
        Notifier.schedule(this)
        WidgetUpdater.updateAll(this)

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_events -> show(EventsFragment(), R.string.tab_events)
                R.id.nav_holidays -> show(HolidaysFragment(), R.string.tab_holidays)
                R.id.nav_settings -> show(SettingsFragment(), R.string.tab_settings)
            }
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_events
    }

    private fun show(f: Fragment, title: Int) {
        supportActionBar?.setTitle(title)
        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
    }
}
