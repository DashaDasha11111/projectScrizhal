/**
 * Ежедневная проверка дней рождения и именин клириков.
 * Запускается через GitHub Actions каждый день в 08:00 по Москве.
 * Уведомляет митрополита за 2 дня до дня рождения или именин.
 *
 * Требует переменную окружения FIREBASE_SERVICE_ACCOUNT_JSON
 * (JSON-ключ сервисного аккаунта из Firebase Console).
 */

const admin = require("firebase-admin");

// Инициализация Firebase Admin SDK через сервисный аккаунт
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();
const messaging = admin.messaging();

// Парсит "dd.MM.yyyy" → { day, month }
function parseDayMonth(dateStr) {
    if (!dateStr) return null;
    const parts = dateStr.split(".");
    if (parts.length < 2) return null;
    return { day: parseInt(parts[0], 10), month: parseInt(parts[1], 10) };
}

// Отправляет FCM-уведомление митрополиту
async function sendFcm(token, title, body) {
    try {
        await messaging.send({
            token,
            notification: { title, body },
            data: { type: "metropolitan" },
            android: {
                notification: {
                    channelId: "scrizhal_notifications",
                    priority: "high",
                },
            },
        });
        console.log(`✓ FCM отправлен: "${title}" — ${body}`);
    } catch (e) {
        console.error(`✗ Ошибка FCM для "${title}":`, e.message);
    }
}

async function main() {
    // Целевая дата = сегодня + 2 дня
    const now = new Date();
    const target = new Date(now);
    target.setDate(target.getDate() + 2);
    const targetDay   = target.getDate();
    const targetMonth = target.getMonth() + 1; // 1-based

    console.log(`Проверка: ищем дни рождения/именины ${String(targetDay).padStart(2,"0")}.${String(targetMonth).padStart(2,"0")}`);

    // Получаем токен митрополита
    const tokenDoc = await db.collection("fcmTokens").doc("metropolitan").get();
    if (!tokenDoc.exists || !tokenDoc.data().token) {
        console.warn("⚠ Токен митрополита не найден в fcmTokens/metropolitan — уведомление не отправлено.");
        return;
    }
    const metropToken = tokenDoc.data().token;

    // Перебираем всех клириков
    const clericsSnap = await db.collection("clerics").get();
    const tasks = [];

    clericsSnap.forEach((doc) => {
        const c = doc.data();

        // День рождения
        const bday = parseDayMonth(c.birthday);
        if (bday && bday.day === targetDay && bday.month === targetMonth) {
            tasks.push(
                sendFcm(
                    metropToken,
                    "День рождения — через 2 дня",
                    `${c.name}${c.church ? " (" + c.church + ")" : ""} — ${c.birthday}`
                )
            );
        }

        // Именины
        const nday = parseDayMonth(c.nameDay);
        if (nday && nday.day === targetDay && nday.month === targetMonth) {
            tasks.push(
                sendFcm(
                    metropToken,
                    "Именины — через 2 дня",
                    `${c.name}${c.church ? " (" + c.church + ")" : ""} — ${c.nameDay}`
                )
            );
        }
    });

    await Promise.all(tasks);
    console.log(`Готово. Проверено ${clericsSnap.size} клириков, отправлено ${tasks.length} уведомлений.`);
}

main().catch((err) => {
    console.error("Критическая ошибка:", err);
    process.exit(1);
});
