package com.example.scrizhal

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.time.DayOfWeek
import java.time.LocalDate

class DetailActivity : AppCompatActivity() {
    private lateinit var prefManager: SharedPrefManager
    private var currentClericId: Int = -1
    private var currentClericName: String = ""
    private var currentClericChurch: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefManager = SharedPrefManager(this)

        val cleric = intent.getSerializableExtra("CLERIC_KEY") as? Cleric
        if (cleric == null) {
            Toast.makeText(this, "Нет данных о клирике", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvNextAward = findViewById<TextView>(R.id.tvNextAward)
        val btnAction = findViewById<MaterialButton>(R.id.btnAction)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)
        val receivedContainer = findViewById<LinearLayout>(R.id.receivedAwardsContainer)
        val forecastContainer = findViewById<LinearLayout>(R.id.forecastContainer)
        val tvAddAward = findViewById<TextView>(R.id.tvAddAward)
        val btnClose = findViewById<ImageView>(R.id.btnClose)

        btnClose.setOnClickListener { finish() }

        currentClericId = cleric.id
        currentClericName = cleric.name
        currentClericChurch = cleric.church
        tvName.text = cleric.name

        btnAction.text = if (cleric.awardType == "MOSCOW") "Сформировать в документ" else "Отправить в мастерскую"

        val orderStatus = prefManager.getOrderStatus(cleric.id)
        val hasMoscowRequest = prefManager.hasMoscowRequest(cleric.id)

        if (cleric.awardType != "MOSCOW" && (orderStatus == "new" || orderStatus == "in_work")) {
            setButtonOrdered(btnAction)
        } else if (cleric.awardType == "MOSCOW" && hasMoscowRequest) {
            setButtonInDocument(btnAction)
        }

        btnAction.setOnClickListener {
            if (cleric.awardType == "MOSCOW") {
                val awardName = tvNextAward.text.toString()
                if (awardName == "Высшая награда") {
                    Toast.makeText(this, "Все награды уже получены", Toast.LENGTH_SHORT).show()
                } else {
                    // Сохранить заявку «в Москву» (год и дата обновления задаются в SharedPrefManager)
                    prefManager.addMoscowRequest(
                        currentClericId,
                        currentClericName,
                        awardName,
                        currentClericChurch
                    )
                    setButtonInDocument(btnAction)
                    Toast.makeText(this, "Добавлено в список для документа", Toast.LENGTH_SHORT).show()
                }
            } else {
                val awardName = tvNextAward.text.toString()
                if (awardName == "Высшая награда") {
                    Toast.makeText(this, "Все награды уже получены", Toast.LENGTH_SHORT).show()
                } else {
                    showWorkshopDialog(awardName)
                }
            }
        }

        val infoLines = buildList {
            add("Тип события: ${cleric.eventType}")
            add("Значение: ${cleric.eventValue}")
            add("Дата: ${cleric.date} (${cleric.getHumanStatus()})")
            add("Уровень: ${if (cleric.awardType == "MOSCOW") "Москва" else "Регион"}")
            if (cleric.birthday.isNotBlank()) add("Дата рождения: ${DateUtils.formatFlexibleToDisplay(cleric.birthday)}")
            if (cleric.priestOrdination.isNotBlank()) add("Хиротония: ${cleric.priestOrdination}")
            if (cleric.description.isNotBlank()) add("Описание: ${cleric.description}")
        }
        tvInfo.text = infoLines.joinToString("\n")

        if (cleric.eventType != "Награды") {
            tvStatus.text = cleric.getHumanStatus()
            findViewById<View>(R.id.tvNextAwardLabel).visibility = View.GONE
            tvNextAward.visibility = View.GONE
            btnAction.visibility = View.GONE
            findViewById<View>(R.id.tvReceivedTitle).visibility = View.GONE
            receivedContainer.visibility = View.GONE
            tvAddAward.visibility = View.GONE
            findViewById<View>(R.id.tvForecastTitle).visibility = View.GONE
            findViewById<View>(R.id.forecastHeader).visibility = View.GONE
            forecastContainer.visibility = View.GONE
            return
        }

        val ordinationDate = cleric.priestOrdination.takeIf { it.isNotBlank() }?.let {
            runCatching { DateUtils.parseFlexible(it) }.getOrNull()
        }

        fun refreshAwardsUI() {
            receivedContainer.removeAllViews()
            forecastContainer.removeAllViews()

            val history = prefManager.getClericAwards(cleric.id)
            val ord = ordinationDate
            if (ord == null) {
                tvStatus.text = cleric.getHumanStatus()
                tvNextAward.text = Awards.normalizedAwardName(cleric.eventValue)
                receivedContainer.addView(simpleRow("—", ""))
                return
            }

            val next = Awards.computeNextFromHistory(cleric.isMonk, ord, history)
            if (next == null) {
                tvNextAward.text = "Высшая награда"
                tvStatus.text = "—"
            } else {
                tvNextAward.text = next.title
                val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), next.date)
                tvStatus.text = if (days <= 0) "Готов к награждению" else "через $days дн."
            }

            val received = Awards.receivedFromHistory(cleric.isMonk, history)
            if (received.isEmpty()) {
                receivedContainer.addView(simpleRow("—", ""))
            } else {
                received.forEach { (title, year) ->
                    receivedContainer.addView(simpleRow(title, "($year)"))
                }
            }

            if (next != null) {
                val forecast = Awards.forecastFromNext(cleric.isMonk, ord, next, maxRows = 6)
                forecast.forEach { row ->
                    forecastContainer.addView(forecastRow(row.year.toString(), row.title, row.status))
                }
            }
        }

        tvAddAward.setOnClickListener {
            val ord = ordinationDate
            if (ord == null) {
                Toast.makeText(this, "Нужна дата хиротонии для расчёта наград", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val history = prefManager.getClericAwards(cleric.id)
            val receivedTitles = history.map { Awards.normalizedAwardName(it.first) }.toSet()
            val options = Awards.ladder(cleric.isMonk).filter { Awards.normalizedAwardName(it) !in receivedTitles }
            if (options.isEmpty()) {
                Toast.makeText(this, "Все очередные награды уже внесены", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@DetailActivity, android.R.layout.simple_spinner_dropdown_item, options)
            }

            AlertDialog.Builder(this)
                .setTitle("Добавить награду")
                .setView(spinner)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Далее") { _, _ ->
                    val selectedTitle = spinner.selectedItem?.toString()?.trim().orEmpty()
                    if (selectedTitle.isBlank()) return@setPositiveButton

                    val now = LocalDate.now()
                    DatePickerDialog(
                        this,
                        { _, y, m, d ->
                            val date = LocalDate.of(y, m + 1, d)
                            prefManager.addClericAward(cleric.id, selectedTitle, date)
                            prefManager.clearWorkshopOrder(cleric.id)
                            refreshAwardsUI()
                            resetButtonToDefault(
                                findViewById(R.id.btnAction),
                                cleric.awardType
                            )
                        },
                        now.year,
                        now.monthValue - 1,
                        now.dayOfMonth
                    ).show()
                }
                .show()
        }

        refreshAwardsUI()
    }

    private fun simpleRow(left: String, right: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }

        val tvLeft = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = left
            setTextColor(getColor(R.color.korich))
            textSize = 14f
        }

        val tvRight = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = right
            setTextColor(getColor(R.color.button))
            textSize = 13f
        }

        row.addView(tvLeft)
        row.addView(tvRight)
        return row
    }

    private fun forecastRow(date: String, award: String, status: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }

        fun cell(text: String, weight: Float, gravityEnd: Boolean = false): TextView {
            return TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                this.text = text
                setTextColor(getColor(R.color.korich))
                textSize = 13f
                if (gravityEnd) this.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            }
        }

        val c1 = cell(date, 1f)
        val c2 = cell(award, 2f)
        val c3 = cell(status, 1f, gravityEnd = true).apply {
            setTextColor(getColor(if (status == "Готов") R.color.button else R.color.korich))
        }

        row.addView(c1)
        row.addView(c2)
        row.addView(c3)
        return row
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun setButtonOrdered(btn: MaterialButton) {
        btn.text = "Заказ оформлен"
        btn.isEnabled = false
        btn.alpha = 0.6f
    }

    private fun setButtonInDocument(btn: MaterialButton) {
        btn.text = "В документе"
        btn.isEnabled = false
        btn.alpha = 0.6f
    }

    private fun resetButtonToDefault(btn: MaterialButton, awardType: String) {
        btn.text = if (awardType == "MOSCOW") "Сформировать в документ" else "Отправить в мастерскую"
        btn.isEnabled = true
        btn.alpha = 1f
    }

    // --- Workshop dialog flow ---

    companion object {
        private val WORKSHOP_NAMES = listOf("Красноярская Ризница", "Софрино", "Мастерская Регент")

        private val AWARD_PRICES = mapOf(
            "Набедренник" to listOf("5 000 руб", "6 500 руб", "4 500 руб"),
            "Камилавка" to listOf("4 000 руб", "5 000 руб", "3 500 руб"),
            "Наперсный крест (золотой)" to listOf("15 000 руб", "18 000 руб", "12 000 руб"),
            "Палица" to listOf("7 000 руб", "9 000 руб", "6 500 руб"),
            "Крест с украшениями" to listOf("20 000 руб", "24 000 руб", "18 000 руб"),
            "Протоиерей" to listOf("3 000 руб", "4 000 руб", "2 500 руб"),
            "Литургия с отверстыми вратами (Иже Херувимы)" to listOf("3 000 руб", "4 000 руб", "2 500 руб"),
            "Литургия с отверстыми вратами (Отче наш)" to listOf("3 000 руб", "4 000 руб", "2 500 руб"),
            "Митра" to listOf("25 000 руб", "30 000 руб", "22 000 руб"),
            "Архимандрит" to listOf("3 000 руб", "4 000 руб", "2 500 руб"),
            "Второй крест с украшениями" to listOf("22 000 руб", "26 000 руб", "20 000 руб")
        )

        private val HEADWEAR_AWARDS = setOf("Камилавка", "Митра")

        private val DAY_ABBR = mapOf(
            DayOfWeek.MONDAY to "пн",
            DayOfWeek.TUESDAY to "вт",
            DayOfWeek.WEDNESDAY to "ср",
            DayOfWeek.THURSDAY to "чт",
            DayOfWeek.FRIDAY to "пт",
            DayOfWeek.SATURDAY to "сб",
            DayOfWeek.SUNDAY to "вс"
        )
    }

    private fun getPrice(award: String, workshopIndex: Int): String {
        return AWARD_PRICES[award]?.getOrElse(workshopIndex) { "—" } ?: "—"
    }

    private fun showWorkshopDialog(awardName: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_workshop)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.findViewById<ImageView>(R.id.btnCloseDialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.tvDialogAwardTitle).text = awardName

        val spinner = dialog.findViewById<Spinner>(R.id.spinnerWorkshop)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, WORKSHOP_NAMES)

        val tvPrice = dialog.findViewById<TextView>(R.id.tvDialogPrice)
        tvPrice.text = getPrice(awardName, 0)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                tvPrice.text = getPrice(awardName, pos)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        dialog.findViewById<MaterialButton>(R.id.btnDialogNext).setOnClickListener {
            val selectedIndex = spinner.selectedItemPosition
            dialog.dismiss()
            showParamsDialog(awardName, selectedIndex)
        }

        dialog.show()
    }

    private fun showParamsDialog(awardName: String, workshopIndex: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_workshop_params)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        dialog.findViewById<ImageView>(R.id.btnCloseParams).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.tvParamsAwardTitle).text = awardName

        val spinner = dialog.findViewById<Spinner>(R.id.spinnerWorkshopParams)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, WORKSHOP_NAMES)
        spinner.setSelection(workshopIndex)

        val tvPrice = dialog.findViewById<TextView>(R.id.tvParamsPrice)
        tvPrice.text = getPrice(awardName, workshopIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                tvPrice.text = getPrice(awardName, pos)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // --- Sizes (only for headwear) ---
        val isHeadwear = awardName in HEADWEAR_AWARDS
        val tvSizeLabel = dialog.findViewById<TextView>(R.id.tvSizeLabel)
        val llRow1 = dialog.findViewById<LinearLayout>(R.id.llSizesRow1)
        val llRow2 = dialog.findViewById<LinearLayout>(R.id.llSizesRow2)

        var selectedSizeIndex = 2
        val allSizeViews = mutableListOf<TextView>()

        if (!isHeadwear) {
            tvSizeLabel.visibility = View.GONE
            llRow1.visibility = View.GONE
            llRow2.visibility = View.GONE
        } else {
            val sizes = (54..65).toList()

            fun refreshSizes() {
                allSizeViews.forEachIndexed { i, tv ->
                    val sel = i == selectedSizeIndex
                    tv.setBackgroundResource(if (sel) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
                    tv.setTextColor(getColor(if (sel) R.color.bel else R.color.korich))
                }
            }

            sizes.forEachIndexed { i, size ->
                val tv = TextView(this).apply {
                    text = size.toString()
                    layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                        marginEnd = if (i % 6 < 5) dp(4) else 0
                    }
                    gravity = Gravity.CENTER
                    textSize = 14f
                    setOnClickListener {
                        selectedSizeIndex = i
                        refreshSizes()
                    }
                }
                allSizeViews.add(tv)
                if (i < 6) llRow1.addView(tv) else llRow2.addView(tv)
            }

            refreshSizes()
        }

        // --- Dates (6 days starting from today+7) ---
        val llDates = dialog.findViewById<LinearLayout>(R.id.llDates)
        var selectedDateIndex = 0
        val allDateViews = mutableListOf<TextView>()
        val today = LocalDate.now()
        val dates = (7..12).map { today.plusDays(it.toLong()) }

        fun refreshDates() {
            allDateViews.forEachIndexed { i, tv ->
                val sel = i == selectedDateIndex
                tv.setBackgroundResource(if (sel) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
                tv.setTextColor(getColor(if (sel) R.color.bel else R.color.korich))
            }
        }

        dates.forEachIndexed { i, date ->
            val dayNum = date.dayOfMonth
            val dayName = DAY_ABBR[date.dayOfWeek] ?: ""
            val tv = TextView(this).apply {
                text = "$dayNum\n$dayName"
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginEnd = if (i < 5) dp(4) else 0
                }
                gravity = Gravity.CENTER
                textSize = 12f
                setOnClickListener {
                    selectedDateIndex = i
                    refreshDates()
                }
            }
            allDateViews.add(tv)
            llDates.addView(tv)
        }

        refreshDates()

        // --- Submit ---
        val etComment = dialog.findViewById<EditText>(R.id.etComment)
        dialog.findViewById<MaterialButton>(R.id.btnSubmitOrder).setOnClickListener {
            val workshopName = WORKSHOP_NAMES[spinner.selectedItemPosition]
            val price = tvPrice.text.toString()
            val selectedDate = dates[selectedDateIndex]
            val size = if (isHeadwear) (54 + selectedSizeIndex).toString() else null
            val comment = etComment.text.toString()

            val dueDateStr = DateUtils.format(selectedDate)

            prefManager.saveWorkshopOrder(
                currentClericId, awardName, workshopName,
                price, dueDateStr, size, comment,
                recipient = currentClericName,
                church = currentClericChurch
            )

            FirestoreManager.saveWorkshopOrder(
                clericId = currentClericId,
                clericName = currentClericName,
                awardName = awardName,
                workshopName = workshopName,
                price = price,
                dueDate = dueDateStr,
                size = size,
                comment = comment,
                church = currentClericChurch
            )

            dialog.dismiss()

            setButtonOrdered(findViewById(R.id.btnAction))
            showSuccessDialog(awardName, workshopName, price, selectedDate, size)
        }

        dialog.show()
    }

    private fun showSuccessDialog(
        awardName: String,
        workshopName: String,
        price: String,
        date: LocalDate,
        size: String?
    ) {
        val dateStr = DateUtils.format(date)
        val message = buildString {
            append("Награда: $awardName\n")
            append("Мастерская: $workshopName\n")
            append("Стоимость: $price\n")
            append("Дата: $dateStr")
            if (size != null) append("\nРазмер: $size")
        }

        AlertDialog.Builder(this)
            .setTitle("Заказ отправлен!")
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}