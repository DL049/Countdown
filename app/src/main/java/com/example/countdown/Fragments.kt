package com.example.countdown

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EventsFragment : Fragment(R.layout.fragment_list) {
    private val adapter = EventAdapter { e ->
        startActivity(
            Intent(requireContext(), EventEditActivity::class.java)
                .putExtra(EventEditActivity.EXTRA_EVENT_ID, e.id),
        )
    }
    private lateinit var empty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@EventsFragment.adapter
        }
        empty = view.findViewById(R.id.empty)
        empty.setText(R.string.empty_events)
        view.findViewById<FloatingActionButton>(R.id.fab).apply {
            contentDescription = getString(R.string.new_event)
            setOnClickListener {
                startActivity(Intent(requireContext(), EventEditActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val events = Repo.events(requireContext())
        adapter.submit(events, Repo.holidays(requireContext()))
        empty.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
    }
}

class HolidaysFragment : Fragment(R.layout.fragment_list) {
    private val adapter = HolidayAdapter { h ->
        startActivity(
            Intent(requireContext(), HolidayEditActivity::class.java)
                .putExtra(HolidayEditActivity.EXTRA_HOLIDAY_ID, h.id),
        )
    }
    private lateinit var empty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HolidaysFragment.adapter
        }
        empty = view.findViewById(R.id.empty)
        empty.setText(R.string.empty_holidays)
        view.findViewById<FloatingActionButton>(R.id.fab).apply {
            contentDescription = getString(R.string.new_holiday)
            setOnClickListener {
                startActivity(Intent(requireContext(), HolidayEditActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val list = Repo.holidays(requireContext())
        adapter.submit(list)
        empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
}
