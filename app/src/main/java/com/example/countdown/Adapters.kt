package com.example.countdown

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(private val onClick: (CountdownEvent) -> Unit) :
    RecyclerView.Adapter<EventAdapter.VH>() {

    private data class Row(val event: CountdownEvent, val summary: EventSummary)

    private var rows = emptyList<Row>()

    fun submit(events: List<CountdownEvent>, holidays: List<Holiday>) {
        rows = CountdownCalc.sortEvents(events).map { Row(it, CountdownCalc.summarize(it, holidays)) }
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.tv_number)
        val unit: TextView = v.findViewById(R.id.tv_unit)
        val title: TextView = v.findViewById(R.id.tv_title)
        val date: TextView = v.findViewById(R.id.tv_date)
        val detail: TextView = v.findViewById(R.id.tv_detail)
        val meter: SoftMeter = v.findViewById(R.id.meter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false))

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val (e, s) = rows[position]
        val c = h.itemView.context
        h.number.text = Fmt.number(s).toString()
        h.unit.text = Fmt.unit(c, e, s)
        h.title.text = if (e.pinned) "📌 ${e.title}" else e.title
        h.date.text = listOf(e.category, Fmt.dateFull(s.target)).filter { it.isNotBlank() }.joinToString(" · ")
        h.detail.text = Fmt.detail(c, e, s)
        // darker text shades keep WCAG AA contrast on the soft background
        h.number.setTextColor(Fmt.numberColor(c, e, s))
        h.detail.setTextColor(
            ContextCompat.getColor(
                c, if (s.skippedByHolidays > 0) R.color.soft_orange_text else R.color.soft_muted,
            ),
        )
        h.meter.visibility = if (s.phase == Phase.UPCOMING) View.VISIBLE else View.GONE
        h.meter.progress = s.progress
        h.itemView.setOnClickListener { onClick(e) }
    }
}

class HolidayAdapter(private val onClick: (Holiday) -> Unit) :
    RecyclerView.Adapter<HolidayAdapter.VH>() {

    private var items = emptyList<Holiday>()

    fun submit(list: List<Holiday>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val range: TextView = v.findViewById(R.id.tv_range)
        val length: TextView = v.findViewById(R.id.tv_length)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_holiday, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val hol = items[position]
        val c = h.itemView.context
        h.name.text = hol.name
        h.range.text = if (hol.start == hol.end) {
            Fmt.dateMedium(hol.start)
        } else {
            c.getString(R.string.range_fmt, Fmt.dateMedium(hol.start), Fmt.dateMedium(hol.end))
        }
        h.length.text = c.getString(R.string.holiday_length, hol.lengthDays)
        h.itemView.setOnClickListener { onClick(hol) }
    }
}
