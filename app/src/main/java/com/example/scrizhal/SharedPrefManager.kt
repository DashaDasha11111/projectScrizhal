package com.example.scrizhal

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SharedPrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val clericsListKey = "clerics_list"
    private val currentClericIdKey = "current_cleric_id"

    fun saveUser(login: String, password: String) {
        prefs.edit()
            .putString("saved_login", login)
            .putString("saved_password", password)
            .apply()
    }

    fun getSavedLogin(): String? = prefs.getString("saved_login", null)
    fun getSavedPassword(): String? = prefs.getString("saved_password", null)

    fun setLoggedIn(username: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("current_user", username)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun getCurrentUser(): String? = prefs.getString("current_user", null)

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("current_user")
            .apply()
    }

    // --- Cleric auth (for profile) ---

    fun setCurrentClericId(id: Int) {
        prefs.edit().putInt(currentClericIdKey, id).apply()
    }

    fun getCurrentClericId(): Int? {
        val id = prefs.getInt(currentClericIdKey, 0)
        return if (id == 0) null else id
    }

    fun clearCurrentClericId() {
        prefs.edit().remove(currentClericIdKey).apply()
    }

    // --- Clerics list (add/edit/delete) ---

    fun getClericList(): List<Cleric> {
        val raw = prefs.getString(clericsListKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Cleric(
                    id = o.optInt("id", 0),
                    name = o.optString("name", ""),
                    isMonk = o.optBoolean("isMonk", false),
                    birthday = o.optString("birthday", ""),
                    date = o.optString("date", ""),
                    eventType = o.optString("eventType", "Награды"),
                    eventValue = o.optString("eventValue", "Набедренник"),
                    awardType = o.optString("awardType", "REGION"),
                    priestOrdination = o.optString("priestOrdination", ""),
                    description = o.optString("description", ""),
                    church = o.optString("church", ""),
                    phone = o.optString("phone", ""),
                    email = o.optString("email", ""),
                    nameDay = o.optString("nameDay", ""),
                    diaconalOrdination = o.optString("diaconalOrdination", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveClericList(list: List<Cleric>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("isMonk", c.isMonk)
                put("birthday", c.birthday)
                put("date", c.date)
                put("eventType", c.eventType)
                put("eventValue", c.eventValue)
                put("awardType", c.awardType)
                put("priestOrdination", c.priestOrdination)
                put("description", c.description)
                put("church", c.church)
                put("phone", c.phone)
                put("email", c.email)
                put("nameDay", c.nameDay)
                put("diaconalOrdination", c.diaconalOrdination)
            })
        }
        prefs.edit().putString(clericsListKey, arr.toString()).apply()
    }

    // --- Churches list (храмы Красноярского края) ---

    private val churchesListKey = "churches_list"

    fun getChurchList(): List<Church> {
        val raw = prefs.getString(churchesListKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Church(
                    id = o.optInt("id", 0),
                    name = o.optString("name", ""),
                    address = o.optString("address", ""),
                    rector = o.optString("rector", ""),
                    clericCount = o.optInt("clericCount", 0),
                    latitude = o.optDouble("latitude", 0.0),
                    longitude = o.optDouble("longitude", 0.0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveChurchList(list: List<Church>) {
        val arr = JSONArray()
        list.forEach { ch ->
            arr.put(JSONObject().apply {
                put("id", ch.id)
                put("name", ch.name)
                put("address", ch.address)
                put("rector", ch.rector)
                put("clericCount", ch.clericCount)
                put("latitude", ch.latitude)
                put("longitude", ch.longitude)
            })
        }
        prefs.edit().putString(churchesListKey, arr.toString()).apply()
    }

    fun getDefaultChurchList(): List<Church> = listOf(
        Church(1, "Храм иконы Божией Матери «Скоропослушница» при городской больнице г. Красноярска", "660022, г. Красноярск, ул. Партизана Железняка, 1г", "протоиерей Николай Петров", 3, 56.0483, 92.9371),
        Church(2, "Собор Казанской иконы Божией Матери в г. Ачинске", "662100, Красноярский край, г. Ачинск, ул. Карла Маркса, 21", "Евгений Григорьевич Фролов", 8, 56.2694, 90.4953),
        Church(3, "Храм святителя Николая Чудотворца в с. Большом Улуе", "662120, Красноярский край, Ачинский р-н, с. Большой Улуй, ул. Центральная, 42", "иерей Сергий Иванов", 2, 56.6520, 90.2280),
        Church(4, "Храм благоверного князя Александра Невского в г. Красноярске", "660049, г. Красноярск, ул. Академика Вавилова, 1", "протоиерей Андрей Юревич", 5, 56.0152, 92.7975),
        Church(5, "Свято-Покровский кафедральный собор г. Красноярска", "660049, г. Красноярск, ул. Сурикова, 26", "митрополит Пантелеимон", 12, 56.0112, 92.8758),
        Church(6, "Храм Рождества Христова г. Красноярска", "660077, г. Красноярск, ул. 9 Мая, 21", "протоиерей Владимир Смирнов", 6, 56.0421, 92.9365)
    )

    fun ensureChurchListLoaded(): List<Church> {
        var list = getChurchList()
        if (list.isEmpty()) {
            list = getDefaultChurchList()
            saveChurchList(list)
        } else {
            val defaultsById = getDefaultChurchList().associateBy { it.id }
            val merged = list.map { c ->
                if (c.latitude == 0.0 && c.longitude == 0.0) {
                    defaultsById[c.id] ?: c
                } else c
            }
            if (merged != list) {
                saveChurchList(merged)
                list = merged
            }
        }
        return list
    }

    // --- Cleric awards ---

    private fun awardsKey(clericId: Int): String = "cleric_awards_$clericId"

    fun getClericAwards(clericId: Int): List<Pair<String, LocalDate>> {
        val raw = prefs.getString(awardsKey(clericId), "") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.split("||")
            .mapNotNull { part ->
                val pieces = part.split("@@")
                if (pieces.size != 2) return@mapNotNull null
                val title = pieces[0].trim()
                val date = runCatching { LocalDate.parse(pieces[1].trim()) }.getOrNull()
                    ?: return@mapNotNull null
                if (title.isBlank()) return@mapNotNull null
                title to date
            }
    }

    fun addClericAward(clericId: Int, title: String, date: LocalDate) {
        val normalizedTitle = Awards.normalizedAwardName(title)
        val current = getClericAwards(clericId).toMutableList()
        current.removeAll { it.first == normalizedTitle }
        current.add(normalizedTitle to date)

        val encoded = current
            .sortedBy { it.second }
            .joinToString("||") { (t, d) -> "${t}@@${d}" }

        prefs.edit().putString(awardsKey(clericId), encoded).apply()
    }

    fun setClericAwards(clericId: Int, awards: List<Pair<String, LocalDate>>) {
        if (awards.isEmpty()) {
            prefs.edit().remove(awardsKey(clericId)).apply()
            return
        }
        val encoded = awards
            .sortedBy { it.second }
            .joinToString("||") { (title, date) -> "${Awards.normalizedAwardName(title)}@@${date}" }
        prefs.edit().putString(awardsKey(clericId), encoded).apply()
    }

    // --- Workshop orders ---

    private fun orderKey(clericId: Int): String = "workshop_order_$clericId"

    fun saveWorkshopOrder(
        clericId: Int, awardName: String, workshopName: String,
        price: String, date: String, size: String?, comment: String,
        recipient: String = "", church: String = ""
    ) {
        val parts = mutableListOf(
            "award@@$awardName",
            "workshop@@$workshopName",
            "price@@$price",
            "date@@$date",
            "created@@${LocalDate.now()}",
            "status@@new"
        )
        if (recipient.isNotBlank()) parts.add("recipient@@$recipient")
        if (church.isNotBlank()) parts.add("church@@$church")
        size?.let { parts.add("size@@$it") }
        if (comment.isNotBlank()) parts.add("comment@@$comment")

        prefs.edit().putString(orderKey(clericId), parts.joinToString("||")).apply()
    }

    fun getWorkshopOrderRaw(clericId: Int): String? {
        return prefs.getString(orderKey(clericId), null)
    }

    fun hasWorkshopOrder(clericId: Int): Boolean {
        return prefs.contains(orderKey(clericId))
    }

    fun getOrderedClericIds(): Set<Int> {
        return prefs.all.keys
            .filter { it.startsWith("workshop_order_") }
            .mapNotNull { it.removePrefix("workshop_order_").toIntOrNull() }
            .toSet()
    }

    fun getActiveOrderedClericIds(): Set<Int> {
        return getOrderedClericIds().filter { id ->
            val status = getOrderStatus(id)
            status == "new" || status == "in_work"
        }.toSet()
    }

    fun clearWorkshopOrder(clericId: Int) {
        prefs.edit().remove(orderKey(clericId)).apply()
    }

    fun parseOrderMap(clericId: Int): Map<String, String>? {
        val raw = getWorkshopOrderRaw(clericId) ?: return null
        return raw.split("||").associate {
            val parts = it.split("@@", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }
    }

    fun updateOrderStatus(clericId: Int, newStatus: String) {
        val raw = getWorkshopOrderRaw(clericId) ?: return
        val parts = raw.split("||").toMutableList()

        val statusIdx = parts.indexOfFirst { it.startsWith("status@@") }
        if (statusIdx >= 0) {
            parts[statusIdx] = "status@@$newStatus"
        } else {
            parts.add("status@@$newStatus")
        }

        if (newStatus == "completed") {
            val completedIdx = parts.indexOfFirst { it.startsWith("completedDate@@") }
            if (completedIdx >= 0) {
                parts[completedIdx] = "completedDate@@${LocalDate.now()}"
            } else {
                parts.add("completedDate@@${LocalDate.now()}")
            }
        }

        prefs.edit().putString(orderKey(clericId), parts.joinToString("||")).apply()
    }

    fun getOrderStatus(clericId: Int): String {
        val map = parseOrderMap(clericId) ?: return ""
        return map["status"] ?: "new"
    }

    fun getAllOrdersByStatus(status: String): List<Pair<Int, Map<String, String>>> {
        val result = mutableListOf<Pair<Int, Map<String, String>>>()
        for (id in getOrderedClericIds()) {
            val map = parseOrderMap(id) ?: continue
            val orderStatus = map["status"] ?: "new"
            if (orderStatus == status) {
                result.add(id to map)
            }
        }
        return result
    }

    // --- Moscow award requests (documents) ---

    private fun moscowKey(clericId: Int): String = "moscow_request_$clericId"
    private val moscowSentYearsKey = "moscow_sent_years"
    private val moscowSentClericIdsKey = "moscow_sent_cleric_ids"
    private val fcmTokenKey = "fcm_token"

    fun getMoscowSentYears(): Set<Int> {
        val raw = prefs.getString(moscowSentYearsKey, "") ?: ""
        return raw.split("||").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun setMoscowRequestSent(year: Int) {
        val current = getMoscowSentYears().toMutableSet()
        current.add(year)
        prefs.edit().putString(moscowSentYearsKey, current.joinToString("||")).apply()
        val idsInDocument = getMoscowRequestsByYear(year).map { it.clericId }.toSet()
        if (idsInDocument.isNotEmpty()) {
            val hidden = getMoscowSentClericIds().toMutableSet()
            hidden.addAll(idsInDocument)
            prefs.edit().putString(moscowSentClericIdsKey, hidden.joinToString("||")).apply()
        }
    }

    private fun getMoscowSentClericIds(): Set<Int> {
        val raw = prefs.getString(moscowSentClericIdsKey, "") ?: ""
        return raw.split("||").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun addMoscowRequest(
        clericId: Int,
        name: String,
        awardName: String,
        church: String
    ) {
        val now = LocalDate.now()
        val sentYears = getMoscowSentYears()
        val year = if (now.year in sentYears) now.year + 1 else now.year
        val dateAdded = now.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val parts = listOf(
            "name@@$name",
            "award@@$awardName",
            "church@@$church",
            "year@@$year",
            "dateAdded@@$dateAdded"
        )
        prefs.edit().putString(moscowKey(clericId), parts.joinToString("||")).apply()
    }

    fun hasMoscowRequest(clericId: Int): Boolean {
        return prefs.contains(moscowKey(clericId))
    }

    private fun getMoscowRequestYear(clericId: Int): Int? {
        val raw = prefs.getString(moscowKey(clericId), null) ?: return null
        val map = raw.split("||").associate {
            val parts = it.split("@@", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }
        val y = map["year"]?.toIntOrNull()
        if (y != null) return y
        val dateStr = map["date"] ?: return null
        return runCatching { LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrNull()?.year
    }

    fun getMoscowRequestedClericIds(): Set<Int> {
        val sent = getMoscowSentYears()
        return prefs.all.keys
            .filter { it.startsWith("moscow_request_") }
            .mapNotNull { it.removePrefix("moscow_request_").toIntOrNull() }
            .filter { id -> getMoscowRequestYear(id) !in sent }
            .toSet()
    }

    /** ID клириков, которых не показывать на дашборде: сохранённый список при «Обращение отправлено» + по году обращения. */
    fun getClericIdsInSentMoscowYears(): Set<Int> {
        val sent = getMoscowSentYears()
        val byStoredIds = getMoscowSentClericIds()
        if (sent.isEmpty()) return byStoredIds
        val byYear = getAllMoscowRequests()
            .filter { req -> req.year in sent || getMoscowRequestYear(req.clericId) == null }
            .map { it.clericId }
            .toSet()
        return byStoredIds + byYear
    }

    fun getAllMoscowRequests(): List<MoscowRequest> {
        val result = mutableListOf<MoscowRequest>()
        for (key in prefs.all.keys) {
            if (!key.startsWith("moscow_request_")) continue
            val id = key.removePrefix("moscow_request_").toIntOrNull() ?: continue
            val raw = prefs.getString(key, null) ?: continue
            val map = raw.split("||").associate {
                val parts = it.split("@@", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
            var year = map["year"]?.toIntOrNull()
            var dateAdded = map["dateAdded"] ?: ""
            if (year == null || dateAdded.isEmpty()) {
                val dateStr = map["date"] ?: ""
                dateAdded = dateStr
                year = runCatching { LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrNull()?.year ?: LocalDate.now().year
            }
            result += MoscowRequest(
                clericId = id,
                name = map["name"] ?: "Клирик #$id",
                awardName = map["award"] ?: "",
                church = map["church"] ?: "",
                year = year ?: LocalDate.now().year,
                dateAdded = dateAdded
            )
        }
        return result
    }

    fun getMoscowRequestsByYear(year: Int): List<MoscowRequest> =
        getAllMoscowRequests().filter { it.year == year }

    fun getMoscowYearsWithRequests(): Set<Int> =
        getAllMoscowRequests().map { it.year }.toSet()

    fun getLastUpdatedForYear(year: Int): String =
        getMoscowRequestsByYear(year).map { it.dateAdded }.maxOrNull() ?: ""

    fun updateMoscowRequestDateAddedForYear(year: Int) {
        val today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        for (key in prefs.all.keys) {
            if (!key.startsWith("moscow_request_")) continue
            val raw = prefs.getString(key, null) ?: continue
            val map = raw.split("||").associate {
                val parts = it.split("@@", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
            if ((map["year"]?.toIntOrNull() ?: 0) != year) continue
            val parts = raw.split("||").toMutableList()
            val idx = parts.indexOfFirst { it.startsWith("dateAdded@@") }
            if (idx >= 0) parts[idx] = "dateAdded@@$today"
            else parts.add("dateAdded@@$today")
            prefs.edit().putString(key, parts.joinToString("||")).apply()
        }
    }

    // --- Liturgy assignments & cleric notifications (праздничные литургии) ---

    private val liturgyAssignmentsKey = "liturgy_assignments"

    private val liturgyDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun getAllLiturgyAssignments(): List<LiturgyAssignment> {
        val raw = prefs.getString(liturgyAssignmentsKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                LiturgyAssignment(
                    id = o.optInt("id", 0),
                    clericId = o.optInt("clericId", 0),
                    churchId = o.optInt("churchId", 0),
                    feastName = o.optString("feastName", ""),
                    date = o.optString("date", ""),
                    status = o.optString("status", "назначено")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addLiturgyAssignment(assignment: LiturgyAssignment) {
        val current = getAllLiturgyAssignments().toMutableList()
        val newId = if (assignment.id == 0) {
            (current.maxOfOrNull { it.id } ?: 0) + 1
        } else {
            assignment.id
        }
        current.removeAll { it.id == newId }
        current.add(assignment.copy(id = newId))
        val arr = JSONArray()
        current.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("clericId", a.clericId)
                    put("churchId", a.churchId)
                    put("feastName", a.feastName)
                    put("date", a.date)
                    put("status", a.status)
                }
            )
        }
        prefs.edit().putString(liturgyAssignmentsKey, arr.toString()).apply()
    }

    fun getAssignmentsForCleric(clericId: Int): List<LiturgyAssignment> =
        getAllLiturgyAssignments().filter { it.clericId == clericId }

    fun getUpcomingAssignments(): List<LiturgyAssignment> {
        val today = LocalDate.now()
        return getAllLiturgyAssignments().filter { a ->
            runCatching { LocalDate.parse(a.date, liturgyDateFormatter) }
                .getOrNull()
                ?.let { !it.isBefore(today) } ?: false
        }.sortedBy { runCatching { LocalDate.parse(it.date, liturgyDateFormatter) }.getOrNull() }
    }

    fun getAssignmentsByDate(feastDate: String): List<LiturgyAssignment> =
        getAllLiturgyAssignments().filter { it.date == feastDate }

    // --- Cleric notifications ---

    private fun notificationsKey(clericId: Int): String = "cleric_notifications_$clericId"

    fun getClericNotifications(clericId: Int): List<ClericNotification> {
        val raw = prefs.getString(notificationsKey(clericId), null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                ClericNotification(
                    clericId = o.optInt("clericId", clericId),
                    text = o.optString("text", ""),
                    date = o.optString("date", ""),
                    churchName = o.optString("churchName", ""),
                    feastName = o.optString("feastName", ""),
                    createdAt = o.optString("createdAt", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addClericNotification(
        clericId: Int,
        text: String,
        date: String,
        churchName: String,
        feastName: String
    ) {
        val current = getClericNotifications(clericId).toMutableList()
        current.add(
            ClericNotification(
                clericId = clericId,
                text = text,
                date = date,
                churchName = churchName,
                feastName = feastName,
                createdAt = LocalDate.now().format(liturgyDateFormatter)
            )
        )
        val arr = JSONArray()
        current.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("clericId", n.clericId)
                    put("text", n.text)
                    put("date", n.date)
                    put("churchName", n.churchName)
                    put("feastName", n.feastName)
                    put("createdAt", n.createdAt)
                }
            )
        }
        prefs.edit().putString(notificationsKey(clericId), arr.toString()).apply()
    }

    fun clearClericNotifications(clericId: Int) {
        prefs.edit().remove(notificationsKey(clericId)).apply()
    }

    // --- Личный дневник митрополита ---

    private val metropolitanDiaryKey = "metropolitan_diary_entries"

    fun getMetropolitanDiaryEntries(): List<MetropolitanDiaryEntry> {
        val raw = prefs.getString(metropolitanDiaryKey, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                MetropolitanDiaryEntry(
                    id = o.optString("id", ""),
                    date = o.optString("date", ""),
                    whereServes = o.optString("whereServes", ""),
                    scheduleNote = o.optString("scheduleNote", ""),
                    meetingsNote = o.optString("meetingsNote", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveMetropolitanDiaryEntries(entries: List<MetropolitanDiaryEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("date", e.date)
                    put("whereServes", e.whereServes)
                    put("scheduleNote", e.scheduleNote)
                    put("meetingsNote", e.meetingsNote)
                }
            )
        }
        prefs.edit().putString(metropolitanDiaryKey, arr.toString()).apply()
    }

    fun addOrUpdateMetropolitanDiaryEntry(entry: MetropolitanDiaryEntry) {
        val list = getMetropolitanDiaryEntries().toMutableList()
        val id = entry.id.ifBlank { java.util.UUID.randomUUID().toString() }
        val withId = entry.copy(id = id)
        list.removeAll { it.id == withId.id }
        list.add(withId)
        val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        list.sortByDescending {
            runCatching { LocalDate.parse(it.date, fmt) }.getOrNull() ?: LocalDate.MIN
        }
        saveMetropolitanDiaryEntries(list)
    }

    fun deleteMetropolitanDiaryEntry(id: String) {
        saveMetropolitanDiaryEntries(getMetropolitanDiaryEntries().filter { it.id != id })
    }

    // --- FCM token ---

    fun saveFcmToken(token: String) {
        prefs.edit().putString(fcmTokenKey, token).apply()
    }

    fun getFcmToken(): String? = prefs.getString(fcmTokenKey, null)

    fun saveFcmUserRole(role: String) {
        prefs.edit().putString("fcm_user_role", role).apply()
    }

    fun getFcmUserRole(): String? = prefs.getString("fcm_user_role", null)

    // ---

    fun preloadTestDataIfNeeded() {
        if (prefs.getBoolean("test_data_loaded_v4", false)) return

        // ID 4 Сергий Васильев — следующая: Камилавка
        addClericAward(4, "Набедренник", LocalDate.of(2022, 3, 18))

        // ID 6 Иеромонах Тихон (монах) — следующая: Наперсный крест
        addClericAward(6, "Набедренник", LocalDate.of(2021, 4, 15))

        // ID 8 Петр Могила — следующая: Палица
        addClericAward(8, "Набедренник", LocalDate.of(2014, 6, 20))
        addClericAward(8, "Камилавка", LocalDate.of(2017, 6, 20))
        addClericAward(8, "Наперсный крест (золотой)", LocalDate.of(2021, 6, 20))

        // ID 10 Димитрий Соколов — следующая: Камилавка
        addClericAward(10, "Набедренник", LocalDate.of(2023, 4, 1))

        // ID 11 Алексий Волков — следующая: Наперсный крест (золотой)
        addClericAward(11, "Набедренник", LocalDate.of(2019, 5, 15))
        addClericAward(11, "Камилавка", LocalDate.of(2022, 5, 15))

        // ID 12 Игумен Серафим (монах) — следующая: Наперсный крест (монах, без камилавки)
        addClericAward(12, "Набедренник", LocalDate.of(2021, 3, 20))

        // ID 13 Георгий Козлов — следующая: Палица
        addClericAward(13, "Набедренник", LocalDate.of(2008, 1, 20))
        addClericAward(13, "Камилавка", LocalDate.of(2011, 1, 20))
        addClericAward(13, "Наперсный крест (золотой)", LocalDate.of(2015, 1, 20))

        // ID 14 Владимир Орлов — следующая: Крест с украшениями
        addClericAward(14, "Набедренник", LocalDate.of(2003, 9, 14))
        addClericAward(14, "Камилавка", LocalDate.of(2006, 9, 14))
        addClericAward(14, "Наперсный крест (золотой)", LocalDate.of(2010, 9, 14))
        addClericAward(14, "Палица", LocalDate.of(2015, 9, 14))

        // ID 15 Борис Медведев — следующая: Протоиерей
        addClericAward(15, "Набедренник", LocalDate.of(1998, 6, 1))
        addClericAward(15, "Камилавка", LocalDate.of(2001, 6, 1))
        addClericAward(15, "Наперсный крест (золотой)", LocalDate.of(2005, 6, 1))
        addClericAward(15, "Палица", LocalDate.of(2010, 6, 1))
        addClericAward(15, "Крест с украшениями", LocalDate.of(2015, 6, 1))

        // ID 16 Архимандрит Нектарий (монах) — следующая: Крест с украшениями
        addClericAward(16, "Набедренник", LocalDate.of(2006, 4, 25))
        addClericAward(16, "Наперсный крест (золотой)", LocalDate.of(2011, 4, 25))
        addClericAward(16, "Палица", LocalDate.of(2016, 4, 25))

        // ID 20 Анатолий Жуков — следующая: Митра
        addClericAward(20, "Набедренник", LocalDate.of(1988, 4, 10))
        addClericAward(20, "Камилавка", LocalDate.of(1991, 4, 10))
        addClericAward(20, "Наперсный крест (золотой)", LocalDate.of(1995, 4, 10))
        addClericAward(20, "Палица", LocalDate.of(2000, 4, 10))
        addClericAward(20, "Крест с украшениями", LocalDate.of(2005, 4, 10))
        addClericAward(20, "Протоиерей", LocalDate.of(2010, 4, 10))
        addClericAward(20, "Литургия с отверстыми вратами (Иже Херувимы)", LocalDate.of(2015, 4, 10))
        addClericAward(20, "Литургия с отверстыми вратами (Отче наш)", LocalDate.of(2020, 4, 10))

        prefs.edit().putBoolean("test_data_loaded_v4", true).apply()
    }
}

data class MoscowRequest(
    val clericId: Int,
    val name: String,
    val awardName: String,
    val church: String,
    val year: Int,
    val dateAdded: String
)

data class LiturgyAssignment(
    val id: Int,
    val clericId: Int,
    val churchId: Int,
    val feastName: String,
    val date: String,
    val status: String
)

data class ClericNotification(
    val clericId: Int,
    val text: String,
    val date: String,
    val churchName: String,
    val feastName: String,
    val createdAt: String
)

data class MetropolitanDiaryEntry(
    val id: String,
    /** Дата записи, формат dd.MM.yyyy */
    val date: String,
    /** Храм, место; текст ведётся от первого лица (ключ в JSON исторический: whereServes) */
    val whereServes: String,
    /** Порядок дня: службы, переезды */
    val scheduleNote: String,
    /** Встречи, беседы, дела */
    val meetingsNote: String
)

