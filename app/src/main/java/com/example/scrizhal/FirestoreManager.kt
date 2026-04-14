package com.example.scrizhal

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

object FirestoreManager {

    private val db get() = Firebase.firestore
    private const val TAG = "FirestoreManager"

    private const val COLLECTION_TOKENS = "fcmTokens"
    private const val COLLECTION_ORDERS = "workshopOrders"
    private const val COLLECTION_ASSIGNMENTS = "liturgyAssignments"
    private const val COLLECTION_CLERICS = "clerics"

    // --- FCM токены ---

    fun registerToken(role: String, userId: String = role) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                val data = hashMapOf(
                    "token" to token,
                    "role" to role,
                    "updatedAt" to Timestamp.now()
                )
                db.collection(COLLECTION_TOKENS).document(userId).set(data)
                    .addOnFailureListener { e -> Log.e(TAG, "Ошибка сохранения токена для $userId", e) }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Ошибка получения FCM-токена", e) }
    }

    fun unregisterToken(userId: String) {
        db.collection(COLLECTION_TOKENS).document(userId).delete()
            .addOnFailureListener { e -> Log.e(TAG, "Ошибка удаления токена для $userId", e) }
    }

    // --- Заказы в мастерскую ---

    /**
     * Сохранить заказ в Firestore. Cloud Function услышит это создание
     * и пришлёт push мастерской о новом заказе.
     */
    fun saveWorkshopOrder(
        clericId: Int,
        clericName: String,
        awardName: String,
        workshopName: String,
        price: String,
        dueDate: String,
        size: String?,
        comment: String,
        church: String
    ) {
        val data = hashMapOf(
            "clericId" to clericId,
            "clericName" to clericName,
            "awardName" to awardName,
            "workshopName" to workshopName,
            "price" to price,
            "dueDate" to dueDate,
            "size" to (size ?: ""),
            "comment" to comment,
            "church" to church,
            "status" to "new",
            "createdAt" to Timestamp.now()
        )
        // Документ с ключом clericId — один активный заказ на клирика
        db.collection(COLLECTION_ORDERS).document(clericId.toString()).set(data)
            .addOnSuccessListener { Log.d(TAG, "Заказ сохранён в Firestore для клирика $clericId") }
            .addOnFailureListener { e -> Log.e(TAG, "Ошибка сохранения заказа", e) }
    }

    // --- Назначения на литургию ---

    /**
     * Сохранить назначение на службу в Firestore. Cloud Function услышит это
     * и пришлёт push клирику о назначении.
     */
    fun saveLiturgyAssignment(
        clericId: Int,
        clericName: String,
        feastName: String,
        date: String,
        churchName: String
    ) {
        val docId = "${clericId}_${feastName.take(10).replace(" ", "_")}_$date"
        val data = hashMapOf(
            "clericId" to clericId,
            "clericName" to clericName,
            "feastName" to feastName,
            "date" to date,
            "churchName" to churchName,
            "status" to "назначено",
            "createdAt" to Timestamp.now()
        )
        db.collection(COLLECTION_ASSIGNMENTS).document(docId).set(data)
            .addOnSuccessListener { Log.d(TAG, "Назначение сохранено в Firestore для клирика $clericId") }
            .addOnFailureListener { e -> Log.e(TAG, "Ошибка сохранения назначения", e) }
    }

    // --- Клирики (дни рождения и именины для уведомлений митрополиту) ---

    /**
     * Синхронизировать одного клирика в Firestore.
     * Cloud Function будет ежедневно проверять эту коллекцию
     * и уведомлять митрополита за 2 дня до дня рождения/именин.
     */
    fun saveCleric(cleric: Cleric) {
        val data = hashMapOf(
            "id" to cleric.id,
            "name" to cleric.name,
            "birthday" to cleric.birthday,
            "nameDay" to cleric.nameDay,
            "church" to cleric.church
        )
        db.collection(COLLECTION_CLERICS).document(cleric.id.toString()).set(data)
            .addOnFailureListener { e -> Log.e(TAG, "Ошибка сохранения клирика ${cleric.id}", e) }
    }

    // --- Реалтайм-слушатели (для передачи данных между устройствами) ---

    /**
     * Слушает заказы для конкретной мастерской в реальном времени.
     * Вызывается из WorkshopMainActivity, чтобы видеть заказы от митрополита.
     */
    fun listenToOrdersForWorkshop(
        workshopName: String,
        onUpdate: (List<Pair<String, Map<String, Any>>>) -> Unit
    ): ListenerRegistration {
        return db.collection(COLLECTION_ORDERS)
            .whereEqualTo("workshopName", workshopName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Ошибка слушателя заказов мастерской", error)
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    doc.id to data
                } ?: emptyList()
                onUpdate(orders)
            }
    }

    /**
     * Обновляет статус заказа в Firestore (вызывается мастерской).
     * Митрополит увидит обновлённый статус в своём приложении.
     */
    fun updateWorkshopOrderStatus(clericId: Int, newStatus: String) {
        val updates = mutableMapOf<String, Any>("status" to newStatus)
        if (newStatus == "completed") {
            updates["completedDate"] = Timestamp.now()
        }
        db.collection(COLLECTION_ORDERS).document(clericId.toString())
            .update(updates)
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка обновления статуса заказа $clericId", e)
            }
    }

    /**
     * Удаляет заказ из Firestore (при отклонении мастерской).
     */
    fun deleteWorkshopOrder(clericId: Int) {
        db.collection(COLLECTION_ORDERS).document(clericId.toString())
            .delete()
            .addOnFailureListener { e -> Log.e(TAG, "Ошибка удаления заказа $clericId", e) }
    }

    /**
     * Слушает назначения на литургию для конкретного клирика в реальном времени.
     * Вызывается из ClericLiturgiesTabFragment и ClericNotificationsTabFragment.
     */
    fun listenToAssignmentsForCleric(
        clericId: Int,
        onUpdate: (List<Map<String, Any>>) -> Unit
    ): ListenerRegistration {
        return db.collection(COLLECTION_ASSIGNMENTS)
            .whereEqualTo("clericId", clericId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Ошибка слушателя назначений для клирика $clericId", error)
                    return@addSnapshotListener
                }
                val assignments = snapshot?.documents?.mapNotNull { doc -> doc.data } ?: emptyList()
                onUpdate(assignments)
            }
    }

    /**
     * Синхронизировать весь список клириков за один вызов.
     */
    fun syncClerics(clerics: List<Cleric>) {
        val batch = db.batch()
        clerics.forEach { cleric ->
            val ref = db.collection(COLLECTION_CLERICS).document(cleric.id.toString())
            batch.set(ref, hashMapOf(
                "id" to cleric.id,
                "name" to cleric.name,
                "birthday" to cleric.birthday,
                "nameDay" to cleric.nameDay,
                "church" to cleric.church
            ))
        }
        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "Клирики синхронизированы в Firestore (${clerics.size} шт.)") }
            .addOnFailureListener { e -> Log.e(TAG, "Ошибка синхронизации клириков", e) }
    }
}
