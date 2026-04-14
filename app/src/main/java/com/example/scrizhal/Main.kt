package com.example.scrizhal

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Main : AppCompatActivity() {

    private lateinit var prefManager: SharedPrefManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var klirikAdapter: KlirikAdapter
    private lateinit var clericsListAdapter: ClericsListAdapter
    private lateinit var churchesListAdapter: ChurchesListAdapter
    private lateinit var liturgyListAdapter: LiturgyListAdapter

    private var currentTab = "dashboard" // "dashboard" | "clerics" | "churches"
    private var currentChurchSubTab = "churches" // "churches" | "liturgy" | "map"
    private var currentEventFilter = "Все"
    private var currentAwardNameFilter = "Все награды"
    private var currentLevelFilter = "Все"
    private var currentPeriodFilter = "Неделя"
    private var currentLiturgyFilter = "unassigned"

    private val PRIEST_RANKS = setOf("Иерей", "Протоиерей", "Иеромонах", "Игумен", "Архимандрит")

    private val liturgyDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Клирики, уже имеющие назначение на эту календарную дату в другом храме / на другой службе (не эта пара храм+праздник). */
    private fun liturgyBusyClericIdsElsewhere(dateStr: String, churchId: Int, feastName: String): Set<Int> {
        val target = runCatching { LocalDate.parse(dateStr.trim(), liturgyDateFormatter) }.getOrNull()
            ?: return emptySet()
        return prefManager.getAllLiturgyAssignments()
            .filter { a ->
                val ad = runCatching { LocalDate.parse(a.date.trim(), liturgyDateFormatter) }.getOrNull()
                    ?: return@filter false
                ad == target && !(a.churchId == churchId && a.feastName == feastName)
            }
            .map { it.clericId }
            .toSet()
    }
    private val allFeasts = mutableListOf<FeastItem>()

    private val masterList = listOf(
        // --- События (белое духовенство) ---
        Cleric(3, "Александр Сидоров", false, "1988-10-10", "23.03.2026", "Хиротония", "10 лет", "REGION", "2016-03-23", "", "Храм Рождества Христова г. Красноярска", "+7(999)000-00-03", "sidorov@example.com", "10.10", "2016-01-20"),
        Cleric(5, "Михаил Лукин", false, "1990-12-05", "05.04.2026", "День ангела", "35 лет", "MOSCOW", "2018-07-07", "", "Храм Архистратига Михаила г. Красноярска", "+7(999)000-00-05", "lukin@example.com", "05.12", "2018-05-15"),
        Cleric(7, "Николай Романов", false, "1985-07-10", "10.07.2026", "День рождения", "41 год", "REGION", "2010-01-01", "", "Храм Святителя Николая Чудотворца г. Красноярска", "+7(999)000-00-07", "romanov@example.com", "10.07", "2009-10-15"),
        Cleric(17, "Матфей Кузнецов", false, "1982-03-19", "19.03.2026", "День рождения", "44 года", "REGION", "2008-06-10", "", "Храм Покрова Пресвятой Богородицы г. Красноярска", "+7(999)000-00-17", "kuznetsov@example.com", "19.03", "2008-04-20"),
        Cleric(18, "Павел Смирнов", false, "1979-03-28", "28.03.2026", "День ангела", "47 лет", "MOSCOW", "2005-05-20", "", "Храм Вознесения Господня г. Красноярска", "+7(999)000-00-18", "smirnov@example.com", "28.03", "2005-03-25"),
        Cleric(19, "Григорий Попов", false, "1991-09-15", "10.04.2026", "Хиротония", "5 лет", "REGION", "2021-04-10", "", "Храм Казанской иконы Божией Матери г. Красноярска", "+7(999)000-00-19", "popov@example.com", "15.09", "2021-02-10"),

        // --- Награды: Набедренник ---
        Cleric(2, "Иоанн Петров", false, "1975-01-20", "20.03.2026", "Награды", "Набедренник", "MOSCOW", "2023-03-20", "", "Свято-Покровский кафедральный собор г. Красноярска", "+7(999)000-00-02", "petrov@example.com", "20.01", "2023-01-25"),
        Cleric(9, "Василий Крылов", false, "1995-08-12", "25.03.2026", "Награды", "Набедренник", "REGION", "2023-03-25", "", "Храм Преображения Господня г. Красноярска", "+7(999)000-00-09", "krylov@example.com", "12.08", "2023-02-01"),

        // --- Награды: Камилавка (белое) ---
        Cleric(4, "Сергий Васильев", false, "1978-05-15", "18.03.2025", "Награды", "Камилавка", "REGION", "2019-03-18", "", "Свято-Троицкий собор г. Красноярска", "+7(999)000-00-04", "vasiliev@example.com", "15.05", "2019-01-20"),
        Cleric(10, "Димитрий Соколов", false, "1988-11-03", "01.04.2026", "Награды", "Камилавка", "REGION", "2020-04-01", "", "Храм Иоанна Предтечи г. Красноярска", "+7(999)000-00-10", "sokolov@example.com", "03.11", "2020-02-15"),

        // --- Награды: Наперсный крест (белое и монашествующие) ---
        Cleric(11, "Алексий Волков", false, "1980-06-22", "15.05.2026", "Награды", "Наперсный крест (золотой)", "MOSCOW", "2016-05-15", "", "Свято-Благовещенский храм г. Красноярска", "+7(999)000-00-11", "volkov@example.com", "22.06", "2016-03-20"),
        Cleric(12, "Игумен Серафим", true, "1983-04-01", "20.03.2026", "Награды", "Наперсный крест (золотой)", "REGION", "2016-03-20", "", "Спасский мужской монастырь г. Красноярска", "+7(999)000-00-12", "serafim@example.com", "01.04", "2016-01-25"),
        Cleric(6, "Иеромонах Тихон", true, "1989-03-16", "15.04.2026", "Награды", "Наперсный крест (золотой)", "REGION", "2016-04-15", "", "Успенский мужской монастырь г. Красноярска", "+7(999)000-00-06", "tihon@example.com", "16.03", "2016-02-20"),

        // --- Награды: Палица ---
        Cleric(13, "Георгий Козлов", false, "1972-02-14", "20.01.2026", "Награды", "Палица", "REGION", "2005-01-20", "", "Храм Святых Петра и Павла г. Красноярска", "+7(999)000-00-13", "kozlov@example.com", "14.02", "2004-11-20"),
        Cleric(8, "Петр Могила", false, "1970-08-20", "20.06.2026", "Награды", "Палица", "MOSCOW", "2011-06-20", "", "Знаменский храм г. Красноярска", "+7(999)000-00-08", "mogila@example.com", "20.08", "2011-04-25"),

        // --- Награды: Крест с украшениями ---
        Cleric(14, "Владимир Орлов", false, "1968-12-25", "14.09.2025", "Награды", "Крест с украшениями", "MOSCOW", "2000-09-14", "", "Свято-Введенский храм г. Красноярска", "+7(999)000-00-14", "orlov@example.com", "25.12", "2000-07-20"),
        Cleric(16, "Архимандрит Нектарий", true, "1965-07-19", "25.04.2026", "Награды", "Крест с украшениями", "MOSCOW", "2001-04-25", "", "Троицкий мужской монастырь г. Красноярска", "+7(999)000-00-16", "nektariy@example.com", "19.07", "2001-03-01"),

        // --- Награды: Протоиерей (только белое) ---
        Cleric(15, "Борис Медведев", false, "1960-01-30", "01.06.2025", "Награды", "Протоиерей", "REGION", "1995-06-01", "", "Храм Успения Пресвятой Богородицы г. Красноярска", "+7(999)000-00-15", "medvedev@example.com", "30.01", "1995-04-01"),

        // --- Награды: Митра (белое) ---
        Cleric(20, "Анатолий Жуков", false, "1955-04-10", "10.04.2026", "Награды", "Митра", "REGION", "1985-04-10", "", "Кафедральный собор г. Красноярска", "+7(999)000-00-20", "zhukov@example.com", "10.04", "")
    )

    private var currentClericList = mutableListOf<Cleric>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        prefManager = SharedPrefManager(this)
        prefManager.preloadTestDataIfNeeded()
        if (!prefManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        currentClericList = prefManager.getClericList().toMutableList()
        if (currentClericList.isEmpty()) {
            currentClericList = masterList.toMutableList()
            prefManager.saveClericList(currentClericList)
        }
        prefManager.ensureChurchListLoaded()
        FirestoreManager.syncClerics(currentClericList)

        setContentView(R.layout.activity_main2)

        initFeasts()

        findViewById<android.widget.ImageView>(R.id.btnLogout).setOnClickListener {
            if (currentTab == "churches") {
                onBackPressed()
            } else {
                FirestoreManager.unregisterToken(FcmService.NOTIF_TYPE_METROPOLITAN)
                prefManager.logout()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        syncMetropolitanTopAction()

        val dashboardViews = listOf<View>(
            findViewById(R.id.periodToggleGroup),
            findViewById(R.id.title_events),
            findViewById(R.id.title_awards),
            findViewById(R.id.spinner_events),
            findViewById(R.id.spinner_awards),
            findViewById(R.id.rg_award_level),
            findViewById(R.id.spisok)
        )
        val clericsPanel = findViewById<View>(R.id.clericsPanel)
        val churchesPanel = findViewById<View>(R.id.churchesPanel)
        val bottomNavMain = findViewById<View>(R.id.bottomNavMain)
        val bottomNavChurches = findViewById<View>(R.id.bottomNavChurches)

        fun setMainNavIcons(dashboard: Boolean, clerics: Boolean, churches: Boolean) {
            findViewById<android.widget.ImageView>(R.id.navMainDashboard).setImageResource(
                if (dashboard) R.drawable.ic_nav_bird_active else R.drawable.ic_nav_bird
            )
            findViewById<android.widget.ImageView>(R.id.navMainClerics).setImageResource(
                if (clerics) R.drawable.ic_nav_chel_active else R.drawable.ic_nav_chel
            )
            findViewById<android.widget.ImageView>(R.id.navMainChurches).setImageResource(
                if (churches) R.drawable.ic_nav_church_s_active else R.drawable.ic_nav_church_s
            )
            findViewById<android.widget.ImageView>(R.id.navMainOrders).setImageResource(R.drawable.ic_nav_push_s)
        }

        fun enterChurchesMode() {
            currentTab = "churches"
            dashboardViews.forEach { it.visibility = View.GONE }
            clericsPanel.visibility = View.GONE
            churchesPanel.visibility = View.VISIBLE
            bottomNavMain.visibility = View.GONE
            bottomNavChurches.visibility = View.VISIBLE
            syncMetropolitanTopAction()
        }

        fun exitChurchesMode() {
            currentTab = "dashboard"
            dashboardViews.forEach { it.visibility = View.VISIBLE }
            clericsPanel.visibility = View.GONE
            churchesPanel.visibility = View.GONE
            bottomNavMain.visibility = View.VISIBLE
            bottomNavChurches.visibility = View.GONE
            setMainNavIcons(true, false, false)
            syncMetropolitanTopAction()
        }

        findViewById<android.widget.ImageView>(R.id.navMainClerics).setOnClickListener {
            if (currentTab == "clerics") return@setOnClickListener
            currentTab = "clerics"
            dashboardViews.forEach { it.visibility = View.GONE }
            clericsPanel.visibility = View.VISIBLE
            churchesPanel.visibility = View.GONE
            bottomNavMain.visibility = View.VISIBLE
            bottomNavChurches.visibility = View.GONE
            setMainNavIcons(false, true, false)
            syncMetropolitanTopAction()
        }

        findViewById<android.widget.ImageView>(R.id.navMainChurches).setOnClickListener {
            if (currentTab == "churches") return@setOnClickListener
            enterChurchesMode()
            showChurchesSubTab("churches")
        }

        findViewById<android.widget.ImageView>(R.id.navMainDashboard).setOnClickListener {
            if (currentTab == "dashboard") return@setOnClickListener
            currentTab = "dashboard"
            dashboardViews.forEach { it.visibility = View.VISIBLE }
            clericsPanel.visibility = View.GONE
            churchesPanel.visibility = View.GONE
            bottomNavMain.visibility = View.VISIBLE
            bottomNavChurches.visibility = View.GONE
            setMainNavIcons(true, false, false)
            syncMetropolitanTopAction()
        }

        findViewById<android.widget.ImageView>(R.id.navMainOrders).setOnClickListener {
            startActivity(Intent(this, MetropolitanOrdersActivity::class.java))
        }

        // Sub-navigation inside churches section
        findViewById<android.widget.ImageView>(R.id.navSubChurchList).setOnClickListener {
            showChurchesSubTab("churches")
        }

        findViewById<android.widget.ImageView>(R.id.navSubLiturgy).setOnClickListener {
            showChurchesSubTab("liturgy")
        }

        findViewById<android.widget.ImageView>(R.id.navSubMap).setOnClickListener {
            showChurchesSubTab("map")
        }

        recyclerView = findViewById(R.id.spisok)
        recyclerView.layoutManager = LinearLayoutManager(this)

        klirikAdapter = KlirikAdapter(currentClericList) { selected ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("CLERIC_KEY", selected)
            startActivity(intent)
        }
        recyclerView.adapter = klirikAdapter

        val spinnerEvents = findViewById<Spinner>(R.id.spinner_events)
        val spinnerSpecific = findViewById<Spinner>(R.id.spinner_awards)
        val radioGroup = findViewById<RadioGroup>(R.id.rg_award_level)
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.periodToggleGroup)

        val eventTypes = arrayOf("Все", "День рождения", "День ангела", "Хиротония", "Награды")
        spinnerEvents.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, eventTypes)

        val specificAwards = arrayOf(
            "Все награды",
            "Набедренник",
            "Камилавка",
            "Наперсный крест (золотой)",
            "Палица",
            "Крест с украшениями",
            "Протоиерей",
            "Литургия с отверстыми вратами (Иже Херувимы)",
            "Литургия с отверстыми вратами (Отче наш)",
            "Митра",
            "Архимандрит",
            "Второй крест с украшениями"
        )
        spinnerSpecific.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, specificAwards)

        spinnerEvents.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                currentEventFilter = parent?.getItemAtPosition(pos).toString()
                applyFilters()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spinnerSpecific.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                currentAwardNameFilter = parent?.getItemAtPosition(pos).toString()
                applyFilters()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentLevelFilter = when (checkedId) {
                R.id.rb_region -> "REGION"
                R.id.rb_moscow -> "MOSCOW"
                else -> "Все"
            }
            applyFilters()
        }

        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            val button = findViewById<MaterialButton>(checkedId)
            if (isChecked) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.korich))
                button.setTextColor(ContextCompat.getColor(this, R.color.bel))

                currentPeriodFilter = when (checkedId) {
                    R.id.btn_week -> "Неделя"
                    R.id.btn_month -> "Месяц"
                    R.id.btn_later -> "Далее"
                    else -> "Все"
                }
                applyFilters()
            } else {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.obichni))
                button.setTextColor(ContextCompat.getColor(this, R.color.text))
            }
        }

        setupClericsTab()
        applyFilters()
    }

    private fun showChurchesSubTab(tab: String) {
        currentChurchSubTab = tab
        val churchesListPanel = findViewById<View>(R.id.churchesListPanel)
        val liturgyPanel = findViewById<View>(R.id.liturgyPanel)
        val mapPanel = findViewById<View>(R.id.mapPanel)
        val navSubChurchList = findViewById<android.widget.ImageView>(R.id.navSubChurchList)
        val navSubLiturgy = findViewById<android.widget.ImageView>(R.id.navSubLiturgy)
        val navSubMap = findViewById<android.widget.ImageView>(R.id.navSubMap)

        churchesListPanel.visibility = if (tab == "churches") View.VISIBLE else View.GONE
        liturgyPanel.visibility = if (tab == "liturgy") View.VISIBLE else View.GONE
        mapPanel.visibility = if (tab == "map") View.VISIBLE else View.GONE

        when (tab) {
            "churches" -> {
                navSubChurchList.setImageResource(R.drawable.ic_nav_church_s_active)
                navSubLiturgy.setImageResource(R.drawable.ic_nav_book_k)
                navSubMap.setImageResource(R.drawable.ic_nav_map)
            }
            "liturgy" -> {
                navSubChurchList.setImageResource(R.drawable.ic_nav_church_s)
                navSubLiturgy.setImageResource(R.drawable.ic_nav_book_k_active)
                navSubMap.setImageResource(R.drawable.ic_nav_map)
                refreshLiturgyList()
            }
            "map" -> {
                navSubChurchList.setImageResource(R.drawable.ic_nav_church_s)
                navSubLiturgy.setImageResource(R.drawable.ic_nav_book_k)
                navSubMap.setImageResource(R.drawable.ic_nav_map_active)
                refreshChurchMapOverlays()
            }
        }
    }

    private fun setupChurchMap() {
        val mapView = findViewById<MapView>(R.id.churchMapView) ?: return
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true
        mapView.controller.setZoom(8.4)
        mapView.controller.setCenter(GeoPoint(56.2, 91.5))

        val spinner = findViewById<Spinner>(R.id.spinnerMapFeast)
        val names = allFeasts.map { it.name }
        if (names.isEmpty()) return
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (currentChurchSubTab == "map") refreshChurchMapOverlays()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun refreshChurchMapOverlays() {
        val mapView = findViewById<MapView>(R.id.churchMapView) ?: return
        val spinner = findViewById<Spinner>(R.id.spinnerMapFeast)
        if (allFeasts.isEmpty()) return
        val idx = spinner.selectedItemPosition.coerceIn(0, allFeasts.size - 1)
        val feastName = allFeasts[idx].name

        val churches = prefManager.ensureChurchListLoaded()
        val assignments = prefManager.getAllLiturgyAssignments()
        mapView.overlays.clear()

        val iconAssigned = ContextCompat.getDrawable(this, R.drawable.map_marker_assigned)
        val iconFree = ContextCompat.getDrawable(this, R.drawable.map_marker_free)

        for (church in churches) {
            val lat = church.latitude
            val lon = church.longitude
            if (lat == 0.0 && lon == 0.0) continue
            val assigned = assignments.any { it.churchId == church.id && it.feastName == feastName }
            val marker = Marker(mapView)
            marker.position = GeoPoint(lat, lon)
            marker.title = church.name
            marker.snippet = if (assigned) "Назначено на службу" else "Пока не назначено"
            marker.icon = if (assigned) iconAssigned else iconFree
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
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

    private fun refreshLiturgyList() {
        val churches = prefManager.ensureChurchListLoaded()
        val assignments = prefManager.getAllLiturgyAssignments()
        val today = LocalDate.now().minusDays(1)

        val items = mutableListOf<LiturgyListItem>()

        for (church in churches) {
            val feastRows = mutableListOf<LiturgyListItem.FeastRow>()
            for (feast in allFeasts) {
                if (feast.date.isBefore(today)) continue
                val assigned = assignments.filter {
                    it.churchId == church.id && it.feastName == feast.name
                }
                val isAssigned = assigned.isNotEmpty()
                val show = when (currentLiturgyFilter) {
                    "unassigned" -> !isAssigned
                    "assigned" -> isAssigned
                    else -> true
                }
                if (show) {
                    feastRows.add(
                        LiturgyListItem.FeastRow(
                            feast = feast,
                            church = church,
                            assignedClericIds = assigned.map { it.clericId }
                        )
                    )
                }
            }
            if (feastRows.isNotEmpty()) {
                items.add(LiturgyListItem.ChurchHeader(church))
                items.addAll(feastRows)
            }
        }

        liturgyListAdapter.updateData(items)
        val tvEmpty = findViewById<TextView>(R.id.tvLiturgyEmpty)
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAssignDialog(feast: FeastItem, church: Church) {
        val view = layoutInflater.inflate(R.layout.dialog_assign_liturgy, null)
        view.findViewById<TextView>(R.id.tvFeastTitle).text = feast.name
        view.findViewById<TextView>(R.id.tvAssignChurchName).text = church.name

        val dateEt = view.findViewById<EditText>(R.id.etFeastDate)
        dateEt.setText(feast.date.format(liturgyDateFormatter))

        val allClerics = prefManager.getClericList()
        val listView = view.findViewById<ListView>(R.id.lvClerics)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        var eligibleClerics: List<Cleric> = emptyList()

        fun rebuildClericList() {
            val dateStr = dateEt.text.toString().trim()
            val busyElsewhere = liturgyBusyClericIdsElsewhere(dateStr, church.id, feast.name)
            eligibleClerics = allClerics.filter {
                ClericsListAdapter.getDisplayRank(it) in PRIEST_RANKS && it.id !in busyElsewhere
            }
            listView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_multiple_choice,
                eligibleClerics.map { "${it.name} (${ClericsListAdapter.getDisplayRank(it)})" }
            )
            val alreadyAssigned = prefManager.getAllLiturgyAssignments()
                .filter { it.churchId == church.id && it.feastName == feast.name }
                .map { it.clericId }
            for (i in eligibleClerics.indices) {
                if (eligibleClerics[i].id in alreadyAssigned) {
                    listView.setItemChecked(i, true)
                }
            }
        }

        rebuildClericList()
        dateEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { rebuildClericList() }
        })

        val dialog = AlertDialog.Builder(this).setView(view).create()

        view.findViewById<Button>(R.id.btnCancelAssign).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnSaveAssign).setOnClickListener {
            val dateStr = dateEt.text.toString().trim()
            if (dateStr.isEmpty()) {
                Toast.makeText(this, "Укажите дату", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (runCatching { LocalDate.parse(dateStr, liturgyDateFormatter) }.getOrNull() == null) {
                Toast.makeText(this, "Дата в формате дд.мм.гггг", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val busyElsewhere = liturgyBusyClericIdsElsewhere(dateStr, church.id, feast.name)
            val checkedIds = mutableListOf<Int>()
            for (i in eligibleClerics.indices) {
                if (listView.isItemChecked(i)) checkedIds.add(eligibleClerics[i].id)
            }
            if (checkedIds.isEmpty()) {
                Toast.makeText(this, "Выберите хотя бы одного священника", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val conflict = checkedIds.any { it in busyElsewhere }
            if (conflict) {
                Toast.makeText(
                    this,
                    "Один из выбранных уже назначен на эту дату в другом храме",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            for (clericId in checkedIds) {
                prefManager.addLiturgyAssignment(
                    LiturgyAssignment(
                        id = 0,
                        clericId = clericId,
                        churchId = church.id,
                        feastName = feast.name,
                        date = dateStr,
                        status = "назначено"
                    )
                )
                prefManager.addClericNotification(
                    clericId = clericId,
                    text = "Вы назначены на праздничную литургию",
                    date = dateStr,
                    churchName = church.name,
                    feastName = feast.name
                )
                val clericName = eligibleClerics.firstOrNull { it.id == clericId }?.name ?: "Клирик #$clericId"
                FirestoreManager.saveLiturgyAssignment(
                    clericId = clericId,
                    clericName = clericName,
                    feastName = feast.name,
                    date = dateStr,
                    churchName = church.name
                )
            }

            Toast.makeText(this, "Назначения сохранены", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refreshLiturgyList()
            if (currentChurchSubTab == "map") refreshChurchMapOverlays()
        }

        dialog.show()
    }

    private var clericsFilteredList = emptyList<Cleric>()

    private fun setupClericsTab() {
        val searchEdit = findViewById<EditText>(R.id.clericsSearch)
        val spinnerRanks = findViewById<Spinner>(R.id.clericsSpinnerRanks)
        val spinnerTypes = findViewById<Spinner>(R.id.clericsSpinnerTypes)
        val btnAdd = findViewById<com.google.android.material.button.MaterialButton>(R.id.clericsBtnAdd)
        val clericsRecycler = findViewById<RecyclerView>(R.id.clericsRecycler)

        val clergyTypes = arrayOf("Все", "Белое духовенство", "Монашествующие")
        spinnerRanks.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clergyTypes)
        val ranks = arrayOf("Все саны", "Иерей", "Протоиерей", "Иеромонах", "Игумен", "Архимандрит", "Митра")
        spinnerTypes.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ranks)

        clericsListAdapter = ClericsListAdapter(currentClericList) { cleric -> showClericCardDialog(cleric) }
        clericsRecycler.layoutManager = LinearLayoutManager(this)
        clericsRecycler.adapter = clericsListAdapter

        fun applyClericsFilter() {
            var list = currentClericList.toList()
            val query = searchEdit.text.toString().trim().lowercase()
            if (query.isNotEmpty()) {
                list = list.filter { it.name.lowercase().contains(query) }
            }
            val clergyTypeSel = spinnerRanks.selectedItem?.toString() ?: "Все"
            when (clergyTypeSel) {
                "Белое духовенство" -> list = list.filter { !it.isMonk }
                "Монашествующие" -> list = list.filter { it.isMonk }
            }
            val rankSel = spinnerTypes.selectedItem?.toString() ?: "Все саны"
            if (rankSel != "Все саны") {
                list = list.filter { ClericsListAdapter.getDisplayRank(it) == rankSel }
            }
            clericsFilteredList = list
            clericsListAdapter.updateList(list)
        }

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applyClericsFilter() }
            override fun afterTextChanged(s: Editable?) { applyClericsFilter() }
        })
        spinnerRanks.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { applyClericsFilter() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerTypes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { applyClericsFilter() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        btnAdd.setOnClickListener { showClericEditDialog(null) }
        applyClericsFilter()
        setupChurchesTab()
    }

    private fun setupChurchesTab() {
        val churchList = prefManager.ensureChurchListLoaded()
        val searchEdit = findViewById<EditText>(R.id.churchesSearch)
        val churchesRecycler = findViewById<RecyclerView>(R.id.churchesRecycler)
        churchesListAdapter = ChurchesListAdapter(churchList) { church -> showChurchCardDialog(church) }
        churchesRecycler.layoutManager = LinearLayoutManager(this)
        churchesRecycler.adapter = churchesListAdapter
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                val list = if (query.isEmpty()) churchList else churchList.filter {
                    it.name.lowercase().contains(query) || it.address.lowercase().contains(query)
                }
                churchesListAdapter.updateList(list)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup liturgy list
        val liturgyRecycler = findViewById<RecyclerView>(R.id.rvFeasts)
        liturgyRecycler.layoutManager = LinearLayoutManager(this)
        liturgyListAdapter = LiturgyListAdapter(emptyList()) { feast, church -> showAssignDialog(feast, church) }
        liturgyRecycler.adapter = liturgyListAdapter

        val toggleLiturgy = findViewById<MaterialButtonToggleGroup>(R.id.toggleLiturgyFilter)
        val btnUnassigned = findViewById<MaterialButton>(R.id.btnUnassigned)
        val btnAssigned = findViewById<MaterialButton>(R.id.btnAssigned)

        fun updateLiturgyButtonColors() {
            btnUnassigned.setBackgroundColor(
                ContextCompat.getColor(this, if (currentLiturgyFilter == "unassigned") R.color.korich else R.color.obichni)
            )
            btnUnassigned.setTextColor(
                ContextCompat.getColor(this, if (currentLiturgyFilter == "unassigned") R.color.bel else R.color.korich)
            )
            btnAssigned.setBackgroundColor(
                ContextCompat.getColor(this, if (currentLiturgyFilter == "assigned") R.color.korich else R.color.obichni)
            )
            btnAssigned.setTextColor(
                ContextCompat.getColor(this, if (currentLiturgyFilter == "assigned") R.color.bel else R.color.korich)
            )
        }

        toggleLiturgy.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentLiturgyFilter = when (checkedId) {
                    R.id.btnUnassigned -> "unassigned"
                    R.id.btnAssigned -> "assigned"
                    else -> "unassigned"
                }
                updateLiturgyButtonColors()
                refreshLiturgyList()
            }
        }

        setupChurchMap()
    }

    override fun onPause() {
        findViewById<MapView>(R.id.churchMapView)?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        findViewById<MapView>(R.id.churchMapView)?.onDetach()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (currentTab == "churches") {
            // Return to dashboard from churches sub-nav
            currentTab = "dashboard"
            val dashboardViews = listOf<View>(
                findViewById(R.id.periodToggleGroup),
                findViewById(R.id.title_events),
                findViewById(R.id.title_awards),
                findViewById(R.id.spinner_events),
                findViewById(R.id.spinner_awards),
                findViewById(R.id.rg_award_level),
                findViewById(R.id.spisok)
            )
            dashboardViews.forEach { it.visibility = View.VISIBLE }
            findViewById<View>(R.id.clericsPanel).visibility = View.GONE
            findViewById<View>(R.id.churchesPanel).visibility = View.GONE
            findViewById<View>(R.id.bottomNavMain).visibility = View.VISIBLE
            findViewById<View>(R.id.bottomNavChurches).visibility = View.GONE
            // Reset church sub-tab for next time
            currentChurchSubTab = "churches"
            showChurchesSubTab("churches")
            // Update nav icons
            findViewById<android.widget.ImageView>(R.id.navMainDashboard).setImageResource(R.drawable.ic_nav_bird_active)
            findViewById<android.widget.ImageView>(R.id.navMainClerics).setImageResource(R.drawable.ic_nav_chel)
            findViewById<android.widget.ImageView>(R.id.navMainChurches).setImageResource(R.drawable.ic_nav_church_s)
            findViewById<android.widget.ImageView>(R.id.navMainOrders).setImageResource(R.drawable.ic_nav_push_s)
            syncMetropolitanTopAction()
        } else {
            super.onBackPressed()
        }
    }

    private fun syncMetropolitanTopAction() {
        val btn = findViewById<android.widget.ImageView>(R.id.btnLogout)
        if (currentTab == "churches") {
            btn.setImageResource(android.R.drawable.ic_menu_revert)
            btn.contentDescription = "Назад на главный экран"
        } else {
            btn.setImageResource(R.drawable.ic_exit)
            btn.contentDescription = "Выйти"
        }
    }

    private fun showChurchCardDialog(church: Church) {
        val view = layoutInflater.inflate(R.layout.dialog_church_card, null)
        view.findViewById<TextView>(R.id.dialog_church_name).text = church.name
        view.findViewById<TextView>(R.id.dialog_church_address).text = church.address.ifEmpty { "—" }
        view.findViewById<TextView>(R.id.dialog_church_rector).text = church.rector.ifEmpty { "—" }
        view.findViewById<TextView>(R.id.dialog_church_cleric_count).text = church.clericCount.toString()
        val dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<ImageView>(R.id.dialog_church_close).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showClericCardDialog(cleric: Cleric) {
        val view = layoutInflater.inflate(R.layout.dialog_cleric_card, null)
        val birthFormatted = runCatching {
            DateUtils.format(DateUtils.parseIso(cleric.birthday))
        }.getOrElse { cleric.birthday }
        val priestOrdFormatted = runCatching {
            DateUtils.format(DateUtils.parseFlexible(cleric.priestOrdination))
        }.getOrElse { cleric.priestOrdination }
        val diaconalFormatted = if (cleric.diaconalOrdination.isNotBlank()) {
            runCatching { DateUtils.format(DateUtils.parseFlexible(cleric.diaconalOrdination)) }.getOrElse { cleric.diaconalOrdination }
        } else "—"
        view.findViewById<TextView>(R.id.dialog_cleric_fio).text = cleric.name
        view.findViewById<TextView>(R.id.dialog_cleric_birth).text = birthFormatted
        view.findViewById<TextView>(R.id.dialog_cleric_name_day).text = if (cleric.nameDay.isNotBlank()) cleric.nameDay else "—"
        view.findViewById<TextView>(R.id.dialog_cleric_rank).text = ClericsListAdapter.getDisplayRank(cleric)
        view.findViewById<TextView>(R.id.dialog_cleric_diaconal).text = diaconalFormatted
        view.findViewById<TextView>(R.id.dialog_cleric_priest_ord).text = priestOrdFormatted
        view.findViewById<TextView>(R.id.dialog_cleric_parish).text = if (cleric.church.isNotBlank()) "${cleric.church}; Клирик" else "—"
        view.findViewById<TextView>(R.id.dialog_cleric_phone).text = if (cleric.phone.isNotBlank()) cleric.phone else "—"
        view.findViewById<TextView>(R.id.dialog_cleric_email).text = if (cleric.email.isNotBlank()) cleric.email else "—"

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        view.findViewById<ImageView>(R.id.dialog_cleric_close).setOnClickListener { dialog.dismiss() }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialog_cleric_edit).setOnClickListener {
            dialog.dismiss()
            showClericEditDialog(cleric)
        }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialog_cleric_delete).setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle("Удалить клирика?")
                .setMessage("Клирик \"${cleric.name}\" будет удалён из списка.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить") { _, _ ->
                    currentClericList.removeAll { it.id == cleric.id }
                    prefManager.saveClericList(currentClericList)
                    refreshClericsData()
                    Toast.makeText(this, "Клирик удалён", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        dialog.show()
    }

    private fun refreshClericsData() {
        applyFilters()
        if (currentTab == "clerics") {
            var list = currentClericList.toList()
            val searchEdit = findViewById<EditText>(R.id.clericsSearch)
            val spinnerRanks = findViewById<Spinner>(R.id.clericsSpinnerRanks)
            val spinnerTypes = findViewById<Spinner>(R.id.clericsSpinnerTypes)
            val query = searchEdit?.text?.toString()?.trim()?.lowercase() ?: ""
            if (query.isNotEmpty()) list = list.filter { it.name.lowercase().contains(query) }
            val clergyTypeSel = spinnerRanks?.selectedItem?.toString() ?: "Все"
            when (clergyTypeSel) {
                "Белое духовенство" -> list = list.filter { !it.isMonk }
                "Монашествующие" -> list = list.filter { it.isMonk }
            }
            val rankSel = spinnerTypes?.selectedItem?.toString() ?: "Все саны"
            if (rankSel != "Все саны") list = list.filter { ClericsListAdapter.getDisplayRank(it) == rankSel }
            clericsListAdapter.updateList(list)
        }
    }

    private val lastAwardOptions = listOf(
        "Нет наград",
        "Набедренник",
        "Камилавка",
        "Наперсный крест (золотой)",
        "Палица",
        "Крест с украшениями",
        "Протоиерей",
        "Литургия с отверстыми вратами (Иже Херувимы)",
        "Литургия с отверстыми вратами (Отче наш)",
        "Митра",
        "Архимандрит",
        "Второй крест с украшениями"
    )

    private fun showClericEditDialog(existing: Cleric?) {
        val view = layoutInflater.inflate(R.layout.dialog_cleric_edit, null)
        val nameEt = view.findViewById<EditText>(R.id.edit_cleric_name)
        val birthdayEt = view.findViewById<EditText>(R.id.edit_cleric_birthday)
        val nameDayEt = view.findViewById<EditText>(R.id.edit_cleric_name_day)
        val diaconalEt = view.findViewById<EditText>(R.id.edit_cleric_diaconal)
        val priestOrdEt = view.findViewById<EditText>(R.id.edit_cleric_priest_ord)
        val churchSpinner = view.findViewById<Spinner>(R.id.edit_cleric_church)
        val phoneEt = view.findViewById<EditText>(R.id.edit_cleric_phone)
        val emailEt = view.findViewById<EditText>(R.id.edit_cleric_email)
        val radioWhite = view.findViewById<RadioButton>(R.id.edit_cleric_white)
        val radioMonk = view.findViewById<RadioButton>(R.id.edit_cleric_monk)
        val lastAwardSpinner = view.findViewById<Spinner>(R.id.edit_cleric_last_award)
        val lastAwardYearEt = view.findViewById<EditText>(R.id.edit_cleric_last_award_year)

        val churchList = prefManager.ensureChurchListLoaded()
        val churchNames = listOf("— Не указан") + churchList.map { it.name }
        churchSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, churchNames)
        lastAwardSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lastAwardOptions)

        if (existing != null) {
            nameEt.setText(existing.name)
            birthdayEt.setText(existing.birthday)
            nameDayEt.setText(existing.nameDay)
            diaconalEt.setText(existing.diaconalOrdination)
            priestOrdEt.setText(existing.priestOrdination)
            val churchIdx = churchNames.indexOf(existing.church).coerceIn(0, churchNames.size - 1)
            churchSpinner.setSelection(churchIdx)
            phoneEt.setText(existing.phone)
            emailEt.setText(existing.email)
            if (existing.isMonk) radioMonk.isChecked = true else radioWhite.isChecked = true
            val awards = prefManager.getClericAwards(existing.id)
            val last = awards.maxByOrNull { it.second }
            if (last != null) {
                val pos = lastAwardOptions.indexOf(last.first).coerceAtLeast(0)
                lastAwardSpinner.setSelection(pos)
                lastAwardYearEt.setText(last.second.year.toString())
            }
        }

        val editDialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Добавить клирика" else "Редактировать клирика")
            .setView(view)
            .create()

        view.findViewById<MaterialButton>(R.id.edit_cleric_cancel).setOnClickListener { editDialog.dismiss() }
        view.findViewById<MaterialButton>(R.id.edit_cleric_save).setOnClickListener {
            val name = nameEt.text.toString().trim()
            val priestOrd = priestOrdEt.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Введите ФИО", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (priestOrd.isEmpty()) {
                Toast.makeText(this, "Введите дату иерейской хиротонии", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isMonk = view.findViewById<RadioGroup>(R.id.edit_cleric_is_monk).checkedRadioButtonId == R.id.edit_cleric_monk
            val birthday = birthdayEt.text.toString().trim().ifEmpty { "1970-01-01" }
            val lastAwardSel = lastAwardSpinner.selectedItem?.toString() ?: "Нет наград"
            val lastAwardYearStr = lastAwardYearEt.text.toString().trim()
            val nextDateStr = runCatching {
                DateUtils.format(DateUtils.parseFlexible(priestOrd).plusYears(if (isMonk) 5L else 3L))
            }.getOrElse { DateUtils.format(LocalDate.now().plusYears(1)) }

            val clericId: Int
            if (existing == null) {
                clericId = (currentClericList.maxOfOrNull { it.id } ?: 0) + 1
                val newCleric = Cleric(
                    id = clericId,
                    name = name,
                    isMonk = isMonk,
                    birthday = birthday,
                    date = nextDateStr,
                    eventType = "Награды",
                    eventValue = "Набедренник",
                    awardType = "REGION",
                    priestOrdination = priestOrd,
                    description = "",
                    church = (churchSpinner.selectedItem?.toString()?.takeIf { it != "— Не указан" } ?: "").trim(),
                    phone = phoneEt.text.toString().trim(),
                    email = emailEt.text.toString().trim(),
                    nameDay = nameDayEt.text.toString().trim(),
                    diaconalOrdination = diaconalEt.text.toString().trim()
                )
                currentClericList.add(newCleric)
                Toast.makeText(this, "Клирик добавлен", Toast.LENGTH_SHORT).show()
            } else {
                clericId = existing.id
                val idx = currentClericList.indexOfFirst { it.id == existing.id }
                if (idx >= 0) {
                    currentClericList[idx] = existing.copy(
                        name = name,
                        isMonk = isMonk,
                        birthday = birthday,
                        priestOrdination = priestOrd,
                        church = (churchSpinner.selectedItem?.toString()?.takeIf { it != "— Не указан" } ?: "").trim(),
                        phone = phoneEt.text.toString().trim(),
                        email = emailEt.text.toString().trim(),
                        nameDay = nameDayEt.text.toString().trim(),
                        diaconalOrdination = diaconalEt.text.toString().trim()
                    )
                }
                Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show()
            }

            if (lastAwardSel == "Нет наград" || lastAwardYearStr.isEmpty()) {
                prefManager.setClericAwards(clericId, emptyList())
            } else {
                val year = lastAwardYearStr.toIntOrNull() ?: LocalDate.now().year
                prefManager.setClericAwards(clericId, listOf(lastAwardSel to LocalDate.of(year.coerceIn(1900, 2100), 1, 1)))
            }

            prefManager.saveClericList(currentClericList)
            currentClericList.firstOrNull { it.id == clericId }?.let { FirestoreManager.saveCleric(it) }
            refreshClericsData()
            editDialog.dismiss()
        }
        editDialog.show()
    }

    override fun onResume() {
        super.onResume()
        findViewById<MapView>(R.id.churchMapView)?.onResume()
        processCompletedOrders()
        applyFilters()
        if (currentTab == "churches" && currentChurchSubTab == "liturgy") {
            refreshLiturgyList()
        }
        if (currentTab == "churches" && currentChurchSubTab == "map") {
            refreshChurchMapOverlays()
        }
    }

    private fun processCompletedOrders() {
        val completedOrders = prefManager.getAllOrdersByStatus("completed")
        for ((clericId, map) in completedOrders) {
            val awardName = map["award"] ?: continue
            val dateStr = map["date"] ?: continue
            val date = runCatching { DateUtils.parseFlexible(dateStr) }.getOrNull() ?: continue
            val normalized = Awards.normalizedAwardName(awardName)
            val history = prefManager.getClericAwards(clericId)
            if (history.none { it.first == normalized }) {
                prefManager.addClericAward(clericId, awardName, date)
            }
        }
    }

    private fun buildDisplayList(): List<Cleric> {
        return currentClericList.map { cleric ->
            if (cleric.eventType != "Награды") return@map cleric

            val ord = cleric.priestOrdination.takeIf { it.isNotBlank() }?.let {
                runCatching { DateUtils.parseFlexible(it) }.getOrNull()
            } ?: return@map cleric

            val history = prefManager.getClericAwards(cleric.id)
            val next = Awards.computeNextFromHistory(cleric.isMonk, ord, history) ?: return@map cleric

            cleric.copy(
                date = DateUtils.format(next.date),
                eventValue = next.title
            )
        }
    }

    private fun applyFilters() {
        var filteredList = buildDisplayList()

        val hideSentMoscowIds = prefManager.getClericIdsInSentMoscowYears()
        filteredList = filteredList.filter { it.id !in hideSentMoscowIds }

        filteredList = filteredList.filter {
            if (it.eventType != "Награды") {
                DateUtils.getDaysUntil(it.date) >= 0
            } else {
                true
            }
        }

        if (currentEventFilter != "Все") {
            filteredList = filteredList.filter { it.eventType == currentEventFilter }
        }

        if (currentAwardNameFilter != "Все награды") {
            val selected = Awards.normalizedAwardName(currentAwardNameFilter)
            filteredList = filteredList.filter { it.eventType == "Награды" && Awards.normalizedAwardName(it.eventValue) == selected }
        }

        if (currentLevelFilter != "Все") {
            filteredList = filteredList.filter { it.awardType == currentLevelFilter }
        }

        if (currentPeriodFilter != "Все") {
            filteredList = filteredList.filter {
                val days = DateUtils.getDaysUntil(it.date)
                if (it.eventType == "Награды" && days < 0) {
                    currentPeriodFilter != "Далее"
                } else {
                    when (currentPeriodFilter) {
                        "Неделя" -> days in 0..7
                        "Месяц" -> days in 0..30
                        "Далее" -> days > 30
                        else -> true
                    }
                }
            }
        }

        val sortedList = filteredList.sortedBy { DateUtils.getDaysUntil(it.date) }

        val workshopIds = prefManager.getActiveOrderedClericIds()
        val moscowIds = prefManager.getMoscowRequestedClericIds()
        klirikAdapter.updateData(sortedList, workshopIds, moscowIds)

        if (sortedList.isEmpty()) {
            Toast.makeText(this, "По данным фильтрам записей нет", Toast.LENGTH_SHORT).show()
        }
    }
}
