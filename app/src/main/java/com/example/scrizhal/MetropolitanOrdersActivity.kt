package com.example.scrizhal

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MetropolitanOrdersActivity : AppCompatActivity() {

    private lateinit var prefManager: SharedPrefManager
    private lateinit var adapter: MetOrderAdapter
    private var currentTab = "workshop"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metropolitan_orders)

        prefManager = SharedPrefManager(this)

        val rv = findViewById<RecyclerView>(R.id.rvMetOrders)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = MetOrderAdapter(emptyList()) { item -> onItemClick(item) }
        rv.adapter = adapter

        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleOrderType)
        val btnMoscow = findViewById<MaterialButton>(R.id.btnMoscowOrders)
        val btnWorkshop = findViewById<MaterialButton>(R.id.btnWorkshopOrders)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnMoscowOrders -> {
                        currentTab = "moscow"
                        btnMoscow.setBackgroundColor(ContextCompat.getColor(this, R.color.korich))
                        btnMoscow.setTextColor(ContextCompat.getColor(this, R.color.bel))
                    }
                    R.id.btnWorkshopOrders -> {
                        currentTab = "workshop"
                        btnWorkshop.setBackgroundColor(ContextCompat.getColor(this, R.color.korich))
                        btnWorkshop.setTextColor(ContextCompat.getColor(this, R.color.bel))
                    }
                }
                refreshList()
            } else {
                val btn = findViewById<MaterialButton>(checkedId)
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.obichni))
                btn.setTextColor(ContextCompat.getColor(this, R.color.text))
            }
        }

        findViewById<EditText>(R.id.etSearchName).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                refreshList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<ImageView>(R.id.navMetDashboard).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.navMetDiary).setOnClickListener {
            startActivity(Intent(this, MetropolitanDiaryActivity::class.java))
        }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val items = loadOrders()
        adapter.updateData(items)
        findViewById<TextView>(R.id.tvMetEmpty).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadOrders(): List<MetOrderItem> {
        return if (currentTab == "moscow") {
            loadMoscowGroups()
        } else {
            loadWorkshopOrders()
        }
    }

    private fun loadWorkshopOrders(): List<MetOrderItem> {
        val allIds = prefManager.getOrderedClericIds()
        var items = mutableListOf<MetOrderItem>()

        for (id in allIds) {
            val map = prefManager.parseOrderMap(id) ?: continue
            val status = map["status"] ?: "new"
            val statusText = when (status) {
                "new" -> "Отправлено"
                "in_work" -> "В работе"
                "completed" -> "Выполнено"
                else -> status
            }
            items.add(
                MetOrderItem(
                    clericId = id,
                    clericName = map["recipient"] ?: "Клирик #$id",
                    awardName = map["award"] ?: "—",
                    statusText = statusText,
                    isMoscowGroup = false
                )
            )
        }

        if (searchQuery.isNotBlank()) {
            items = items.filter {
                it.clericName.contains(searchQuery, ignoreCase = true)
            }.toMutableList()
        }

        return items
    }

    private fun loadMoscowGroups(): List<MetOrderItem> {
        val years = prefManager.getMoscowYearsWithRequests()
        if (years.isEmpty()) return emptyList()
        val sentYears = prefManager.getMoscowSentYears()

        var groups = years.map { year ->
            val lastUpdated = prefManager.getLastUpdatedForYear(year)
            val isSent = year in sentYears
            val title = if (isSent) {
                "Обращение $year — Отправлено"
            } else {
                "Обращение $year (обновлено $lastUpdated)"
            }
            MetOrderItem(
                clericId = -1,
                clericName = title,
                awardName = "",
                statusText = if (isSent) "" else "Скачать / Отметить отправленным",
                isMoscowGroup = true,
                dateKey = year.toString(),
                moscowYear = year,
                moscowSent = isSent
            )
        }

        if (searchQuery.isNotBlank()) {
            groups = groups.filter { it.dateKey.contains(searchQuery, ignoreCase = true) }
        }

        return groups.sortedByDescending { it.moscowYear }
    }

    private fun onItemClick(item: MetOrderItem) {
        if (currentTab != "moscow" || !item.isMoscowGroup || item.moscowYear == null) return
        val year = item.moscowYear!!
        if (item.moscowSent == true) {
            generateMoscowDocument(year)
            return
        }
        showMoscowYearDialog(year)
    }

    private fun showMoscowYearDialog(year: Int) {
        val requests = prefManager.getMoscowRequestsByYear(year)
        val lastUpdated = prefManager.getLastUpdatedForYear(year)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Обращение $year")
            .setMessage("Последнее обновление: $lastUpdated\nКлириков: ${requests.size}\n\nСкачать документ или отметить обращение как отправленное?")
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Скачать документ") { _, _ -> generateMoscowDocument(year) }
            .setPositiveButton("Обращение отправлено") { _, _ ->
                prefManager.setMoscowRequestSent(year)
                refreshList()
                android.widget.Toast.makeText(this, "Обращение $year отмечено как отправленное", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun generateMoscowDocument(year: Int) {
        val requests = prefManager.getMoscowRequestsByYear(year)
        if (requests.isEmpty()) return

        prefManager.updateMoscowRequestDateAddedForYear(year)
        val lastUpdated = prefManager.getLastUpdatedForYear(year)
        val html = buildDocTable(lastUpdated, requests)

        val fileName = "moscow_awards_$year.doc"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = java.io.File(downloadsDir, fileName)
        file.writeText(html, Charsets.UTF_8)

        android.widget.Toast.makeText(
            this,
            "Документ сохранён: Загрузки / $fileName",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun htmlEscape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun buildDocTable(lastUpdatedStr: String, requests: List<MoscowRequest>): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Ходатайство</title></head>")
        sb.append("<body style=\"font-family: 'Times New Roman', Times, serif; font-size: 12pt;\">")
        sb.append("<p><strong>Ходатайство о награждении</strong></p>")
        sb.append("<p>Последнее обновление: ${htmlEscape(lastUpdatedStr)}</p>")
        sb.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%;\">")
        sb.append("<tr style=\"font-weight: bold;\"><td>ФИО</td><td>Храм</td><td>Награда</td><td>Характеристика</td></tr>")
        for (r in requests) {
            sb.append("<tr>")
            sb.append("<td>${htmlEscape(r.name)}</td>")
            sb.append("<td>${htmlEscape(r.church)}</td>")
            sb.append("<td>${htmlEscape(r.awardName)}</td>")
            sb.append("<td></td>")
            sb.append("</tr>")
        }
        sb.append("</table></body></html>")
        return sb.toString()
    }
}

data class MetOrderItem(
    val clericId: Int,
    val clericName: String,
    val awardName: String,
    val statusText: String,
    val isMoscowGroup: Boolean = false,
    val dateKey: String = "",
    val moscowYear: Int? = null,
    val moscowSent: Boolean? = null
)

class MetOrderAdapter(
    private var items: List<MetOrderItem>,
    private val onClick: (MetOrderItem) -> Unit
) : RecyclerView.Adapter<MetOrderAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvOrderName)
        val tvAward: TextView = view.findViewById(R.id.tvOrderAward)
        val tvStatus: TextView = view.findViewById(R.id.tvOrderStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.metropolitan_order_item, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.clericName
        holder.tvAward.text = item.awardName
        holder.tvStatus.text = item.statusText
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MetOrderItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
