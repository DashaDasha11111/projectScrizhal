package com.example.scrizhal

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MetropolitanDiaryActivity : AppCompatActivity() {

    private lateinit var prefManager: SharedPrefManager
    private lateinit var diaryAdapter: MetropolitanDiaryAdapter
    private val allFeasts = mutableListOf<FeastItem>()
    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metropolitan_diary)

        prefManager = SharedPrefManager(this)
        initFeasts()

        findViewById<ImageView>(R.id.navDiaryBack).setOnClickListener { finish() }

        findViewById<FloatingActionButton>(R.id.fabAddDiary).setOnClickListener {
            showDiaryDialog(null)
        }

        val rv = findViewById<RecyclerView>(R.id.rvDiaryEntries)
        rv.layoutManager = LinearLayoutManager(this)
        diaryAdapter = MetropolitanDiaryAdapter(
            emptyList(),
            onEdit = { showDiaryDialog(it) },
            onDelete = { entry ->
                AlertDialog.Builder(this)
                    .setTitle("Удалить запись?")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Удалить") { _, _ ->
                        prefManager.deleteMetropolitanDiaryEntry(entry.id)
                        refreshDiaryList()
                    }
                    .show()
            }
        )
        rv.adapter = diaryAdapter

        refreshFeastLoad()
        refreshDiaryList()
    }

    override fun onResume() {
        super.onResume()
        refreshFeastLoad()
        refreshDiaryList()
    }

    private fun initFeasts() {
        allFeasts.clear()
        val year = LocalDate.now().year
        allFeasts += listOf(
            FeastItem("Рождество Христово", LocalDate.of(year, 1, 7)),
            FeastItem("Богоявление (Крещение Господне)", LocalDate.of(year, 1, 19)),
            FeastItem("Благовещение Пресвятой Богородицы", LocalDate.of(year, 4, 7)),
            FeastItem("Вербное воскресенье", LocalDate.of(year, 4, 12)),
            FeastItem("Пасха Христова", LocalDate.of(year, 4, 19)),
            FeastItem("Вознесение Господне", LocalDate.of(year, 5, 28)),
            FeastItem("Троица", LocalDate.of(year, 6, 7))
        )
        allFeasts.sortBy { it.date }
    }

    private fun refreshFeastLoad() {
        val container = findViewById<LinearLayout>(R.id.feastLoadContainer)
        container.removeAllViews()
        val churches = prefManager.ensureChurchListLoaded()
        val assignments = prefManager.getAllLiturgyAssignments()
        val today = LocalDate.now().minusDays(1)
        val inflater = layoutInflater

        var any = false
        for (feast in allFeasts) {
            if (feast.date.isBefore(today)) continue
            any = true
            var filled = 0
            val total = churches.size.coerceAtLeast(1)
            for (church in churches) {
                val has = assignments.any { it.churchId == church.id && it.feastName == feast.name }
                if (has) filled++
            }
            val row = inflater.inflate(R.layout.item_metropolitan_feast_load, container, false)
            row.findViewById<TextView>(R.id.tvFeastLoadTitle).text = feast.name
            row.findViewById<TextView>(R.id.tvFeastLoadDate).text = feast.date.format(dateFmt)
            val progress = row.findViewById<ProgressBar>(R.id.progressFeastLoad)
            progress.max = total
            progress.progress = filled
            val pct = (100 * filled) / total
            row.findViewById<TextView>(R.id.tvFeastLoadStats).text =
                "Назначения есть в $filled из $total храмов ($pct%)"
            val barColor = when {
                pct >= 80 -> android.R.color.holo_green_dark
                pct >= 40 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            progress.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, barColor))
            container.addView(row)
        }
        if (!any) {
            val tv = TextView(this)
            tv.text = "Нет предстоящих праздников в списке"
            tv.setTextColor(ContextCompat.getColor(this, R.color.button))
            tv.textSize = 13f
            container.addView(tv)
        }
    }

    private fun refreshDiaryList() {
        val list = prefManager.getMetropolitanDiaryEntries()
        diaryAdapter.update(list)
        findViewById<TextView>(R.id.tvDiaryEmpty).visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDiaryDialog(existing: MetropolitanDiaryEntry?) {
        val view = layoutInflater.inflate(R.layout.dialog_metropolitan_diary_edit, null)
        val etDate = view.findViewById<EditText>(R.id.etDiaryDate)
        val etWhere = view.findViewById<EditText>(R.id.etDiaryWhere)
        val etSchedule = view.findViewById<EditText>(R.id.etDiarySchedule)
        val etMeetings = view.findViewById<EditText>(R.id.etDiaryMeetings)

        if (existing != null) {
            etDate.setText(existing.date)
            etWhere.setText(existing.whereServes)
            etSchedule.setText(existing.scheduleNote)
            etMeetings.setText(existing.meetingsNote)
        } else {
            etDate.setText(LocalDate.now().format(dateFmt))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Новая запись" else "Редактировать")
            .setView(view)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val d = etDate.text.toString().trim()
                if (d.isEmpty()) {
                    etDate.error = "Укажите дату"
                    return@setOnClickListener
                }
                prefManager.addOrUpdateMetropolitanDiaryEntry(
                    MetropolitanDiaryEntry(
                        id = existing?.id ?: "",
                        date = d,
                        whereServes = etWhere.text.toString().trim(),
                        scheduleNote = etSchedule.text.toString().trim(),
                        meetingsNote = etMeetings.text.toString().trim()
                    )
                )
                refreshDiaryList()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}

private class MetropolitanDiaryAdapter(
    private var items: List<MetropolitanDiaryEntry>,
    private val onEdit: (MetropolitanDiaryEntry) -> Unit,
    private val onDelete: (MetropolitanDiaryEntry) -> Unit
) : RecyclerView.Adapter<MetropolitanDiaryAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDiaryDate)
        val tvWhere: TextView = view.findViewById(R.id.tvDiaryWhere)
        val tvSchedule: TextView = view.findViewById(R.id.tvDiarySchedule)
        val tvMeetings: TextView = view.findViewById(R.id.tvDiaryMeetings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_metropolitan_diary_entry, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.tvDate.text = e.date
        holder.tvWhere.text = if (e.whereServes.isNotBlank()) "Служение: ${e.whereServes}" else ""
        holder.tvSchedule.text = if (e.scheduleNote.isNotBlank()) "Порядок дня: ${e.scheduleNote}" else ""
        holder.tvMeetings.text = if (e.meetingsNote.isNotBlank()) "Встречи и дела: ${e.meetingsNote}" else ""
        holder.tvWhere.visibility = if (e.whereServes.isNotBlank()) View.VISIBLE else View.GONE
        holder.tvSchedule.visibility = if (e.scheduleNote.isNotBlank()) View.VISIBLE else View.GONE
        holder.tvMeetings.visibility = if (e.meetingsNote.isNotBlank()) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onEdit(e) }
        holder.itemView.setOnLongClickListener {
            onDelete(e)
            true
        }
    }

    fun update(newItems: List<MetropolitanDiaryEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
}
