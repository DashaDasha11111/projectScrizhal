const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

// Парсит "dd.MM.yyyy" → { day, month }
function parseDayMonth(dateStr) {
    if (!dateStr) return null;
    const parts = dateStr.split(".");
    if (parts.length < 2) return null;
    return { day: parseInt(parts[0], 10), month: parseInt(parts[1], 10) };
}

// Отправляет FCM-уведомление на конкретный токен
async function sendFcm(token, title, body, type) {
    try {
        await messaging.send({
            token,
            notification: { title, body },
            data: { type },
            android: {
                notification: {
                    channelId: "scrizhal_notifications",
                    priority: "high",
                },
            },
        });
        logger.info(`FCM отправлен [${type}]: "${title}"`);
    } catch (e) {
        logger.error(`Ошибка FCM [${type}]:`, e.message);
    }
}

// ─────────────────────────────────────────────────────────────
// 1. Ежедневная проверка дней рождения и именин клириков
//    Уведомляет митрополита за 2 дня
// ─────────────────────────────────────────────────────────────
exports.dailyBirthdayCheck = onSchedule(
    { schedule: "every day 08:00", timeZone: "Europe/Moscow" },
    async () => {
        const now = new Date();
        const target = new Date(now);
        target.setDate(target.getDate() + 2);
        const targetDay   = target.getDate();
        const targetMonth = target.getMonth() + 1; // 1-based

        const tokenDoc = await db.collection("fcmTokens").doc("metropolitan").get();
        if (!tokenDoc.exists || !tokenDoc.data().token) {
            logger.warn("Токен митрополита не найден в fcmTokens/metropolitan");
            return;
        }
        const metropToken = tokenDoc.data().token;

        const clericsSnap = await db.collection("clerics").get();
        const tasks = [];

        clericsSnap.forEach((doc) => {
            const c = doc.data();

            const bday = parseDayMonth(c.birthday);
            if (bday && bday.day === targetDay && bday.month === targetMonth) {
                tasks.push(
                    sendFcm(
                        metropToken,
                        "День рождения — через 2 дня",
                        `${c.name}${c.church ? " (" + c.church + ")" : ""} — ${c.birthday}`,
                        "metropolitan"
                    )
                );
            }

            const nday = parseDayMonth(c.nameDay);
            if (nday && nday.day === targetDay && nday.month === targetMonth) {
                tasks.push(
                    sendFcm(
                        metropToken,
                        "Именины — через 2 дня",
                        `${c.name}${c.church ? " (" + c.church + ")" : ""} — ${c.nameDay}`,
                        "metropolitan"
                    )
                );
            }
        });

        await Promise.all(tasks);
        logger.info(
            `Проверено ${clericsSnap.size} клириков, отправлено ${tasks.length} уведомлений`
        );
    }
);

// ─────────────────────────────────────────────────────────────
// 2. Новый заказ в мастерскую
//    Триггер: создание документа в workshopOrders/{orderId}
// ─────────────────────────────────────────────────────────────
exports.onWorkshopOrderCreated = onDocumentCreated(
    "workshopOrders/{orderId}",
    async (event) => {
        const order = event.data.data();

        const tokenDoc = await db.collection("fcmTokens").doc("workshop").get();
        if (!tokenDoc.exists || !tokenDoc.data().token) {
            logger.warn("Токен мастерской не найден в fcmTokens/workshop");
            return;
        }

        const dueNote = order.dueDate ? ` (к ${order.dueDate})` : "";
        await sendFcm(
            tokenDoc.data().token,
            "Новый заказ",
            `${order.clericName}: ${order.awardName}${dueNote}`,
            "workshop"
        );
    }
);

// ─────────────────────────────────────────────────────────────
// 3. Клирик назначен на службу
//    Триггер: создание документа в liturgyAssignments/{assignmentId}
// ─────────────────────────────────────────────────────────────
exports.onLiturgyAssignmentCreated = onDocumentCreated(
    "liturgyAssignments/{assignmentId}",
    async (event) => {
        const assignment = event.data.data();
        const clericId   = String(assignment.clericId);

        const tokenDoc = await db.collection("fcmTokens").doc(clericId).get();
        if (!tokenDoc.exists || !tokenDoc.data().token) {
            logger.warn(`Токен клирика ${clericId} не найден в fcmTokens/${clericId}`);
            return;
        }

        await sendFcm(
            tokenDoc.data().token,
            "Назначение на службу",
            `${assignment.feastName} — ${assignment.date}, ${assignment.churchName}`,
            "cleric"
        );
    }
);
