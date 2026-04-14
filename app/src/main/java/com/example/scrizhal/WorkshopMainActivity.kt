package com.example.scrizhal

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class WorkshopMainActivity : AppCompatActivity() {

    private lateinit var prefManager: SharedPrefManager
    private lateinit var adapter: WorkshopOrderAdapter
    private var workshopName: String = ""
    private var currentTab = 0

    private var archiveSearchQuery = ""
    private var archiveAwardFilter = "Все"

    // Firestore реалтайм-данные
    private var firestoreOrders = listOf<Pair<String, Map<String, Any>>>()
    private var ordersListener: ListenerRegistration? = null

    private val navIds by lazy { listOf(R.id.navInbox, R.id.navInWork, R.id.navArchive) }

    private val navIcons = listOf(
        R.drawable.ic_nav_inbox to R.drawable.ic_nav_inbox_active,
        R.drawable.ic_nav_sent to R.drawable.ic_nav_sent_active,
        R.drawable.ic_nav_done to R.drawable.ic_nav_done_active
    )

    private val tabTitles = listOf("Новые заказы", "В работе", "Архив")
    private val emptyMessages = listOf("Нет новых заказов", "Нет заказов в работе", "Архив пуст")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workshop_main)

        prefManager = SharedPrefManager(this)
        workshopName = intent.getStringExtra("WORKSHOP_NAME") ?: "Красноярская Ризница"

        findViewById<ImageView>(R.id.btnWsLogout).setOnClickListener {
            FirestoreManager.unregisterToken(FcmService.NOTIF_TYPE_WORKSHOP)
            startActivity(Intent(this, WorkshopLoginActivity::class.java))
            finish()
        }

        val rvOrders = findViewById<RecyclerView>(R.id.rvOrders)
        rvOrders.layoutManager = LinearLayoutManager(this)
        adapter = WorkshopOrderAdapter(emptyList()) { order -> onOrderClicked(order) }
        rvOrders.adapter = adapter

        setupArchiveControls()

        navIds.forEachIndexed { index, id ->
            findViewById<ImageView>(id).setOnClickListener { selectTab(index) }
        }

        selectTab(0)
    }

    override fun onStart() {
        super.onStart()
        // Подписываемся на Firestore — данные от митрополита появятся автоматически
        ordersListener = FirestoreManager.listenToOrdersForWorkshop(workshopName) { orders ->
            firestoreOrders = orders
            runOnUiThread { refreshOrders() }
        }
    }

    override fun onStop() {
        super.onStop()
        ordersListener?.remove()
        ordersListener = null
    }

    private fun setupArchiveControls() {
        val etSearch = findViewById<EditText>(R.id.etSearchChurch)
        val spinnerFilter = findViewById<Spinner>(R.id.spinnerArchiveFilter)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                archiveSearchQuery = s?.toString()?.trim() ?: ""
                refreshOrders()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val awardOptions = listOf(
            "Все", "Набедренник", "Камилавка", "Наперсный крест (золотой)",
            "Палица", "Крест с украшениями", "Митра"
        )
        spinnerFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, awardOptions)
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                archiveAwardFilter = awardOptions[pos]
                refreshOrders()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun selectTab(index: Int) {
        currentTab = index

        navIds.forEachIndexed { i, id ->
            val iv = findViewById<ImageView>(id)
            val (inactive, active) = navIcons[i]
            iv.setImageResource(if (i == index) active else inactive)
        }

        findViewById<TextView>(R.id.tvTitle).text = tabTitles[index]

        val isArchive = index == 2
        findViewById<EditText>(R.id.etSearchChurch).visibility = if (isArchive) View.VISIBLE else View.GONE
        findViewById<Spinner>(R.id.spinnerArchiveFilter).visibility = if (isArchive) View.VISIBLE else View.GONE

        refreshOrders()
    }

    private fun refreshOrders() {
        val orders = loadOrdersForTab()
        val prefix = when (currentTab) {
            0 -> "до"
            1 -> "от"
            else -> ""
        }
        adapter.updateData(orders, prefix)

        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        tvEmpty.text = emptyMessages.getOrElse(currentTab) { "Нет данных" }
        tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
    }

    // Заказы теперь берутся из Firestore, а не из SharedPreferences
    private fun loadOrdersForTab(): List<WorkshopOrderItem> {
        val status = when (currentTab) {
            0 -> "new"
            1 -> "in_work"
            2 -> "completed"
            else -> return emptyList()
        }

        var items = firestoreOrders
            .filter { (_, data) ->
                (data["status"] as? String) == status &&
                (data["workshopName"] as? String) == workshopName
            }
            .map { (docId, data) -> firestoreDocToOrderItem(docId, data) }

        if (currentTab == 2) {
            if (archiveSearchQuery.isNotBlank()) {
                items = items.filter {
                    it.church.contains(archiveSearchQuery, ignoreCase = true)
                }
            }
            if (archiveAwardFilter != "Все") {
                items = items.filter { it.awardName == archiveAwardFilter }
            }
        }

        return items
    }

    // Конвертация данных из Firestore в WorkshopOrderItem
    private fun firestoreDocToOrderItem(docId: String, data: Map<String, Any>): WorkshopOrderItem {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val clericId = (data["clericId"] as? Long)?.toInt() ?: docId.toIntOrNull() ?: 0
        val status = data["status"] as? String ?: "new"
        val rawDueDate = data["dueDate"] as? String ?: "—"

        val createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)
            ?.toDate()?.let { sdf.format(it) } ?: "—"

        val completedDate = (data["completedDate"] as? com.google.firebase.Timestamp)
            ?.toDate()?.let { sdf.format(it) }
            ?: (data["completedDate"] as? String ?: "")

        val displayDate = when (status) {
            "in_work" -> createdAt
            "completed" -> completedDate.ifBlank { rawDueDate }
            else -> rawDueDate
        }

        return WorkshopOrderItem(
            clericId = clericId,
            clericName = data["clericName"] as? String ?: "Клирик #$clericId",
            awardName = data["awardName"] as? String ?: "—",
            workshopName = data["workshopName"] as? String ?: "—",
            price = data["price"] as? String ?: "—",
            date = displayDate,
            size = (data["size"] as? String)?.ifBlank { null },
            comment = (data["comment"] as? String)?.ifBlank { null },
            church = data["church"] as? String ?: "",
            status = status,
            createdDate = createdAt,
            completedDate = completedDate,
            rawDueDate = rawDueDate
        )
    }

    private fun onOrderClicked(order: WorkshopOrderItem) {
        when (currentTab) {
            0 -> showNewOrderReviewDialog(order)
            1 -> showInWorkDetailDialog(order)
            2 -> showArchiveDetailDialog(order)
        }
    }

    // ---- Tab 0: New orders (accept/reject) ----

    private fun showNewOrderReviewDialog(order: WorkshopOrderItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_workshop_order_review)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }

        dialog.findViewById<ImageView>(R.id.btnCloseReview).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.tvReviewAwardTitle).text = order.awardName
        dialog.findViewById<TextView>(R.id.tvReviewRecipient).text =
            order.church.ifBlank { order.clericName }
        dialog.findViewById<TextView>(R.id.tvReviewPrice).text = order.price

        populateSizeChips(dialog, order)
        populateDateChips(dialog, order)
        populateComment(dialog, order)

        dialog.findViewById<MaterialButton>(R.id.btnAcceptOrder).setOnClickListener {
            dialog.dismiss()
            // Обновляем статус в Firestore — митрополит увидит изменение
            FirestoreManager.updateWorkshopOrderStatus(order.clericId, "in_work")
            prefManager.updateOrderStatus(order.clericId, "in_work")
            showResultDialog("Заказ принят в работу")
        }

        dialog.findViewById<MaterialButton>(R.id.btnRejectOrder).setOnClickListener {
            dialog.dismiss()
            // Удаляем заказ из Firestore
            FirestoreManager.deleteWorkshopOrder(order.clericId)
            prefManager.clearWorkshopOrder(order.clericId)
            showResultDialog("Заказ отклонен")
        }

        dialog.show()
    }

    // ---- Tab 1: In-work detail ----

    private fun showInWorkDetailDialog(order: WorkshopOrderItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_workshop_detail)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }

        dialog.findViewById<ImageView>(R.id.btnCloseDetail).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.tvDetailAwardTitle).text = order.awardName
        dialog.findViewById<TextView>(R.id.tvDetailDateLabel).text = "До какой даты"
        // rawDueDate — оригинальная дата дедлайна из Firestore
        dialog.findViewById<TextView>(R.id.tvDetailDate).text = order.rawDueDate.ifBlank { order.date }

        dialog.findViewById<TextView>(R.id.tvDetailChurch).text =
            order.church.ifBlank { order.clericName }
        dialog.findViewById<TextView>(R.id.tvDetailPrice).text = order.price

        if (order.size.isNullOrBlank()) {
            dialog.findViewById<TextView>(R.id.tvDetailSizeLabel).visibility = View.GONE
            dialog.findViewById<TextView>(R.id.tvDetailSize).visibility = View.GONE
        } else {
            dialog.findViewById<TextView>(R.id.tvDetailSize).text = order.size
        }

        dialog.findViewById<MaterialButton>(R.id.btnDetailAction).text = "Заказ выполнен"
        dialog.findViewById<MaterialButton>(R.id.btnDetailAction).setOnClickListener {
            dialog.dismiss()
            // Обновляем статус в Firestore — митрополит увидит "Выполнено"
            FirestoreManager.updateWorkshopOrderStatus(order.clericId, "completed")
            prefManager.updateOrderStatus(order.clericId, "completed")
            showResultDialog("Заказ выполнен!")
        }

        dialog.show()
    }

    // ---- Tab 2: Archive detail ----

    private fun showArchiveDetailDialog(order: WorkshopOrderItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_workshop_detail)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }

        dialog.findViewById<ImageView>(R.id.btnCloseDetail).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.tvDetailAwardTitle).text = order.awardName
        dialog.findViewById<TextView>(R.id.tvDetailDateLabel).text = "Когда было отправлено"
        dialog.findViewById<TextView>(R.id.tvDetailDate).text = order.completedDate.ifBlank { order.date }

        dialog.findViewById<TextView>(R.id.tvDetailChurch).text =
            order.church.ifBlank { order.clericName }
        dialog.findViewById<TextView>(R.id.tvDetailPrice).text = order.price

        if (order.size.isNullOrBlank()) {
            dialog.findViewById<TextView>(R.id.tvDetailSizeLabel).visibility = View.GONE
            dialog.findViewById<TextView>(R.id.tvDetailSize).visibility = View.GONE
        } else {
            dialog.findViewById<TextView>(R.id.tvDetailSize).text = order.size
        }

        dialog.findViewById<MaterialButton>(R.id.btnDetailAction).text = "Хорошо"
        dialog.findViewById<MaterialButton>(R.id.btnDetailAction).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // ---- Shared helpers ----

    private fun populateSizeChips(dialog: Dialog, order: WorkshopOrderItem) {
        val hasSize = !order.size.isNullOrBlank()
        val tvSizeLabel = dialog.findViewById<TextView>(R.id.tvReviewSizeLabel)
        val llRow1 = dialog.findViewById<LinearLayout>(R.id.llReviewSizesRow1)
        val llRow2 = dialog.findViewById<LinearLayout>(R.id.llReviewSizesRow2)

        if (!hasSize) {
            tvSizeLabel.visibility = View.GONE
            llRow1.visibility = View.GONE
            llRow2.visibility = View.GONE
            return
        }

        val selectedSize = order.size?.toIntOrNull() ?: 56
        (54..65).forEachIndexed { i, size ->
            val sel = size == selectedSize
            val tv = TextView(this).apply {
                text = size.toString()
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    marginEnd = if (i % 6 < 5) dp(4) else 0
                }
                gravity = Gravity.CENTER
                textSize = 14f
                setBackgroundResource(if (sel) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
                setTextColor(getColor(if (sel) R.color.bel else R.color.korich))
            }
            if (i < 6) llRow1.addView(tv) else llRow2.addView(tv)
        }
    }

    private fun populateDateChips(dialog: Dialog, order: WorkshopOrderItem) {
        val llDates = dialog.findViewById<LinearLayout>(R.id.llReviewDates)
        val orderDate = runCatching { DateUtils.parseFlexible(order.rawDueDate) }.getOrNull()
        val today = java.time.LocalDate.now()
        val dates = (7..12).map { today.plusDays(it.toLong()) }
        val dayAbbr = mapOf(
            java.time.DayOfWeek.MONDAY to "пн", java.time.DayOfWeek.TUESDAY to "вт",
            java.time.DayOfWeek.WEDNESDAY to "ср", java.time.DayOfWeek.THURSDAY to "чт",
            java.time.DayOfWeek.FRIDAY to "пт", java.time.DayOfWeek.SATURDAY to "сб",
            java.time.DayOfWeek.SUNDAY to "вс"
        )

        dates.forEachIndexed { i, date ->
            val sel = orderDate != null && date == orderDate
            val tv = TextView(this).apply {
                text = "${date.dayOfMonth}\n${dayAbbr[date.dayOfWeek] ?: ""}"
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginEnd = if (i < 5) dp(4) else 0
                }
                gravity = Gravity.CENTER
                textSize = 12f
                setBackgroundResource(if (sel) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
                setTextColor(getColor(if (sel) R.color.bel else R.color.korich))
            }
            llDates.addView(tv)
        }
    }

    private fun populateComment(dialog: Dialog, order: WorkshopOrderItem) {
        val tvCommentLabel = dialog.findViewById<TextView>(R.id.tvReviewCommentLabel)
        val tvComment = dialog.findViewById<TextView>(R.id.tvReviewComment)
        if (order.comment.isNullOrBlank()) {
            tvCommentLabel.visibility = View.GONE
            tvComment.visibility = View.GONE
        } else {
            tvComment.text = order.comment
        }
    }

    private fun showResultDialog(message: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(28), dp(32), dp(28))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dialog)
        }

        val tvMessage = TextView(this).apply {
            text = message
            textSize = 18f
            setTextColor(getColor(R.color.korich))
            gravity = Gravity.CENTER
        }
        layout.addView(tvMessage)

        val btn = MaterialButton(this).apply {
            this.text = "Хорошо"
            setTextColor(getColor(R.color.text))
            setBackgroundColor(getColor(R.color.korich))
            cornerRadius = dp(10)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
            ).apply { topMargin = dp(20) }
            setOnClickListener { dialog.dismiss() }
        }
        layout.addView(btn)

        dialog.setContentView(layout)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.75).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
